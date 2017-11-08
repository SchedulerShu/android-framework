/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mtp;

import static com.android.mtp.MtpDatabaseConstants.*;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.MediaFile;
import android.mtp.MtpConstants;
import android.mtp.MtpObjectInfo;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Database for MTP objects.
 * The object handle which is identifier for object in MTP protocol is not stable over sessions.
 * When we resume the process, we need to remap our document ID with MTP's object handle.
 *
 * If the remote MTP device is backed by typical file system, the file name
 * is unique among files in a directory. However, MTP protocol itself does
 * not guarantee the uniqueness of name so we cannot use fullpath as ID.
 *
 * Instead of fullpath, we use artificial ID generated by MtpDatabase itself. The database object
 * remembers the map of document ID and object handle, and remaps new object handle with document ID
 * by comparing the directory structure and object name.
 *
 * To start putting documents into the database, the client needs to call
 * {@link Mapper#startAddingDocuments(String)} with the parent document ID. Also it
 * needs to call {@link Mapper#stopAddingDocuments(String)} after putting all child
 * documents to the database. (All explanations are same for root documents)
 *
 * database.getMapper().startAddingDocuments();
 * database.getMapper().putChildDocuments();
 * database.getMapper().stopAddingDocuments();
 *
 * To update the existing documents, the client code can repeat to call the three methods again.
 * The newly added rows update corresponding existing rows that have same MTP identifier like
 * objectHandle.
 *
 * The client can call putChildDocuments multiple times to add documents by chunk, but it needs to
 * put all documents under the parent before calling stopAddingChildDocuments. Otherwise missing
 * documents are regarded as deleted, and will be removed from the database.
 *
 * If the client calls clearMtpIdentifier(), it clears MTP identifier in the database. In this case,
 * the database tries to find corresponding rows by using document's name instead of MTP identifier
 * at the next update cycle.
 *
 * TODO: Improve performance by SQL optimization.
 */
class MtpDatabase {
    private final SQLiteDatabase mDatabase;
    private final Mapper mMapper;

    SQLiteDatabase getSQLiteDatabase() {
        return mDatabase;
    }

    MtpDatabase(Context context, int flags) {
        final OpenHelper helper = new OpenHelper(context, flags);
        mDatabase = helper.getWritableDatabase();
        mMapper = new Mapper(this);
    }

    void close() {
        mDatabase.close();
    }

    /**
     * Returns operations for mapping.
     * @return Mapping operations.
     */
    Mapper getMapper() {
        return mMapper;
    }

    /**
     * Queries roots information.
     * @param columnNames Column names defined in {@link android.provider.DocumentsContract.Root}.
     * @return Database cursor.
     */
    Cursor queryRoots(Resources resources, String[] columnNames) {
        final String selection =
                COLUMN_ROW_STATE + " IN (?, ?) AND " + COLUMN_DOCUMENT_TYPE + " = ?";
        final Cursor deviceCursor = mDatabase.query(
                TABLE_DOCUMENTS,
                strings(COLUMN_DEVICE_ID),
                selection,
                strings(ROW_STATE_VALID, ROW_STATE_INVALIDATED, DOCUMENT_TYPE_DEVICE),
                COLUMN_DEVICE_ID,
                null,
                null,
                null);

        try {
            final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(JOIN_ROOTS);
            builder.setProjectionMap(COLUMN_MAP_ROOTS);
            final MatrixCursor result = new MatrixCursor(columnNames);
            final ContentValues values = new ContentValues();

            while (deviceCursor.moveToNext()) {
                final int deviceId = deviceCursor.getInt(0);
                final Cursor storageCursor = builder.query(
                        mDatabase,
                        columnNames,
                        selection + " AND " + COLUMN_DEVICE_ID + " = ?",
                        strings(ROW_STATE_VALID,
                                ROW_STATE_INVALIDATED,
                                DOCUMENT_TYPE_STORAGE,
                                deviceId),
                        null,
                        null,
                        null);
                try {
                    values.clear();
                    try (final Cursor deviceRoot = builder.query(
                            mDatabase,
                            columnNames,
                            selection + " AND " + COLUMN_DEVICE_ID + " = ?",
                            strings(ROW_STATE_VALID,
                                    ROW_STATE_INVALIDATED,
                                    DOCUMENT_TYPE_DEVICE,
                                    deviceId),
                            null,
                            null,
                            null)) {
                        deviceRoot.moveToNext();
                        DatabaseUtils.cursorRowToContentValues(deviceRoot, values);
                    }

                    if (storageCursor.getCount() != 0) {
                        long capacityBytes = 0;
                        long availableBytes = 0;
                        final int capacityIndex =
                                storageCursor.getColumnIndex(Root.COLUMN_CAPACITY_BYTES);
                        final int availableIndex =
                                storageCursor.getColumnIndex(Root.COLUMN_AVAILABLE_BYTES);
                        while (storageCursor.moveToNext()) {
                            // If requested columnNames does not include COLUMN_XXX_BYTES, we
                            // don't calculate corresponding values.
                            if (capacityIndex != -1) {
                                capacityBytes += storageCursor.getLong(capacityIndex);
                            }
                            if (availableIndex != -1) {
                                availableBytes += storageCursor.getLong(availableIndex);
                            }
                        }
                        values.put(Root.COLUMN_CAPACITY_BYTES, capacityBytes);
                        values.put(Root.COLUMN_AVAILABLE_BYTES, availableBytes);
                    } else {
                        values.putNull(Root.COLUMN_CAPACITY_BYTES);
                        values.putNull(Root.COLUMN_AVAILABLE_BYTES);
                    }
                    if (storageCursor.getCount() == 1 && values.containsKey(Root.COLUMN_TITLE)) {
                        storageCursor.moveToFirst();
                        // Add storage name to device name if we have only 1 storage.
                        values.put(
                                Root.COLUMN_TITLE,
                                resources.getString(
                                        R.string.root_name,
                                        values.getAsString(Root.COLUMN_TITLE),
                                        storageCursor.getString(
                                                storageCursor.getColumnIndex(Root.COLUMN_TITLE))));
                    }
                } finally {
                    storageCursor.close();
                }

                final RowBuilder row = result.newRow();
                for (final String key : values.keySet()) {
                    row.add(key, values.get(key));
                }
            }

            return result;
        } finally {
            deviceCursor.close();
        }
    }

    /**
     * Queries root documents information.
     * @param columnNames Column names defined in
     *     {@link android.provider.DocumentsContract.Document}.
     * @return Database cursor.
     */
    @VisibleForTesting
    Cursor queryRootDocuments(String[] columnNames) {
        return mDatabase.query(
                TABLE_DOCUMENTS,
                columnNames,
                COLUMN_ROW_STATE + " IN (?, ?) AND " + COLUMN_DOCUMENT_TYPE + " = ?",
                strings(ROW_STATE_VALID, ROW_STATE_INVALIDATED, DOCUMENT_TYPE_STORAGE),
                null,
                null,
                null);
    }

    /**
     * Queries documents information.
     * @param columnNames Column names defined in
     *     {@link android.provider.DocumentsContract.Document}.
     * @return Database cursor.
     */
    Cursor queryChildDocuments(String[] columnNames, String parentDocumentId) {
        return mDatabase.query(
                TABLE_DOCUMENTS,
                columnNames,
                COLUMN_ROW_STATE + " IN (?, ?) AND " + COLUMN_PARENT_DOCUMENT_ID + " = ?",
                strings(ROW_STATE_VALID, ROW_STATE_INVALIDATED, parentDocumentId),
                null,
                null,
                null);
    }

    /**
     * Returns document IDs of storages under the given device document.
     *
     * @param documentId Document ID that points a device.
     * @return Storage document IDs.
     * @throws FileNotFoundException The given document ID is not registered in database.
     */
    String[] getStorageDocumentIds(String documentId)
            throws FileNotFoundException {
        Preconditions.checkArgument(createIdentifier(documentId).mDocumentType ==
                DOCUMENT_TYPE_DEVICE);
        // Check if the parent document is device that has single storage.
        try (final Cursor cursor = mDatabase.query(
                TABLE_DOCUMENTS,
                strings(Document.COLUMN_DOCUMENT_ID),
                COLUMN_ROW_STATE + " IN (?, ?) AND " +
                COLUMN_PARENT_DOCUMENT_ID + " = ? AND " +
                COLUMN_DOCUMENT_TYPE + " = ?",
                strings(ROW_STATE_VALID,
                        ROW_STATE_INVALIDATED,
                        documentId,
                        DOCUMENT_TYPE_STORAGE),
                null,
                null,
                null)) {
            final String[] ids = new String[cursor.getCount()];
            for (int i = 0; cursor.moveToNext(); i++) {
                ids[i] = cursor.getString(0);
            }
            return ids;
        }
    }

    /**
     * Queries a single document.
     * @param documentId
     * @param projection
     * @return Database cursor.
     */
    Cursor queryDocument(String documentId, String[] projection) {
        return mDatabase.query(
                TABLE_DOCUMENTS,
                projection,
                SELECTION_DOCUMENT_ID,
                strings(documentId),
                null,
                null,
                null,
                "1");
    }

    @Nullable String getDocumentIdForDevice(int deviceId) {
        final Cursor cursor = mDatabase.query(
                TABLE_DOCUMENTS,
                strings(Document.COLUMN_DOCUMENT_ID),
                COLUMN_DOCUMENT_TYPE + " = ? AND " + COLUMN_DEVICE_ID + " = ?",
                strings(DOCUMENT_TYPE_DEVICE, deviceId),
                null,
                null,
                null,
                "1");
        try {
            if (cursor.moveToNext()) {
                return cursor.getString(0);
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Obtains parent identifier.
     * @param documentId
     * @return parent identifier.
     * @throws FileNotFoundException
     */
    Identifier getParentIdentifier(String documentId) throws FileNotFoundException {
        final Cursor cursor = mDatabase.query(
                TABLE_DOCUMENTS,
                strings(COLUMN_PARENT_DOCUMENT_ID),
                SELECTION_DOCUMENT_ID,
                strings(documentId),
                null,
                null,
                null,
                "1");
        try {
            if (cursor.moveToNext()) {
                return createIdentifier(cursor.getString(0));
            } else {
                throw new FileNotFoundException("Cannot find a row having ID = " + documentId);
            }
        } finally {
            cursor.close();
        }
    }

    String getDeviceDocumentId(int deviceId) throws FileNotFoundException {
        try (final Cursor cursor = mDatabase.query(
                TABLE_DOCUMENTS,
                strings(Document.COLUMN_DOCUMENT_ID),
                COLUMN_DEVICE_ID + " = ? AND " + COLUMN_DOCUMENT_TYPE + " = ? AND " +
                COLUMN_ROW_STATE + " != ?",
                strings(deviceId, DOCUMENT_TYPE_DEVICE, ROW_STATE_DISCONNECTED),
                null,
                null,
                null,
                "1")) {
            if (cursor.getCount() > 0) {
                cursor.moveToNext();
                return cursor.getString(0);
            } else {
                throw new FileNotFoundException("The device ID not found: " + deviceId);
            }
        }
    }

    /**
     * Adds new document under the parent.
     * The method does not affect invalidated and pending documents because we know the document is
     * newly added and never mapped with existing ones.
     * @param parentDocumentId
     * @param info
     * @param size Object size. info#getCompressedSize() will be ignored because it does not contain
     *     object size more than 4GB.
     * @return Document ID of added document.
     */
    String putNewDocument(
            int deviceId, String parentDocumentId, int[] operationsSupported, MtpObjectInfo info,
            long size) {
        final ContentValues values = new ContentValues();
        getObjectDocumentValues(
                values, deviceId, parentDocumentId, operationsSupported, info, size);
        mDatabase.beginTransaction();
        try {
            final long id = mDatabase.insert(TABLE_DOCUMENTS, null, values);
            mDatabase.setTransactionSuccessful();
            return Long.toString(id);
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Deletes document and its children.
     * @param documentId
     */
    void deleteDocument(String documentId) {
        deleteDocumentsAndRootsRecursively(SELECTION_DOCUMENT_ID, strings(documentId));
    }

    /**
     * Gets identifier from document ID.
     * @param documentId Document ID.
     * @return Identifier.
     * @throws FileNotFoundException
     */
    Identifier createIdentifier(String documentId) throws FileNotFoundException {
        // Currently documentId is old format.
        final Cursor cursor = mDatabase.query(
                TABLE_DOCUMENTS,
                strings(COLUMN_DEVICE_ID,
                        COLUMN_STORAGE_ID,
                        COLUMN_OBJECT_HANDLE,
                        COLUMN_DOCUMENT_TYPE),
                SELECTION_DOCUMENT_ID + " AND " + COLUMN_ROW_STATE + " IN (?, ?)",
                strings(documentId, ROW_STATE_VALID, ROW_STATE_INVALIDATED),
                null,
                null,
                null,
                "1");
        try {
            if (cursor.getCount() == 0) {
                throw new FileNotFoundException("ID \"" + documentId + "\" is not found.");
            } else {
                cursor.moveToNext();
                return new Identifier(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getInt(2),
                        documentId,
                        cursor.getInt(3));
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Deletes a document, and its root information if the document is a root document.
     * @param selection Query to select documents.
     * @param args Arguments for selection.
     * @return Whether the method deletes rows.
     */
    boolean deleteDocumentsAndRootsRecursively(String selection, String[] args) {
        mDatabase.beginTransaction();
        try {
            boolean changed = false;
            final Cursor cursor = mDatabase.query(
                    TABLE_DOCUMENTS,
                    strings(Document.COLUMN_DOCUMENT_ID),
                    selection,
                    args,
                    null,
                    null,
                    null);
            try {
                while (cursor.moveToNext()) {
                    if (deleteDocumentsAndRootsRecursively(
                            COLUMN_PARENT_DOCUMENT_ID + " = ?",
                            strings(cursor.getString(0)))) {
                        changed = true;
                    }
                }
            } finally {
                cursor.close();
            }
            if (deleteDocumentsAndRoots(selection, args)) {
                changed = true;
            }
            mDatabase.setTransactionSuccessful();
            return changed;
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Marks the documents and their child as disconnected documents.
     * @param selection
     * @param args
     * @return True if at least one row is updated.
     */
    boolean disconnectDocumentsRecursively(String selection, String[] args) {
        mDatabase.beginTransaction();
        try {
            boolean changed = false;
            try (final Cursor cursor = mDatabase.query(
                    TABLE_DOCUMENTS,
                    strings(Document.COLUMN_DOCUMENT_ID),
                    selection,
                    args,
                    null,
                    null,
                    null)) {
                while (cursor.moveToNext()) {
                    if (disconnectDocumentsRecursively(
                            COLUMN_PARENT_DOCUMENT_ID + " = ?",
                            strings(cursor.getString(0)))) {
                        changed = true;
                    }
                }
            }
            if (disconnectDocuments(selection, args)) {
                changed = true;
            }
            mDatabase.setTransactionSuccessful();
            return changed;
        } finally {
            mDatabase.endTransaction();
        }
    }

    boolean deleteDocumentsAndRoots(String selection, String[] args) {
        mDatabase.beginTransaction();
        try {
            int deleted = 0;
            deleted += mDatabase.delete(
                    TABLE_ROOT_EXTRA,
                    Root.COLUMN_ROOT_ID + " IN (" + SQLiteQueryBuilder.buildQueryString(
                            false,
                            TABLE_DOCUMENTS,
                            new String[] { Document.COLUMN_DOCUMENT_ID },
                            selection,
                            null,
                            null,
                            null,
                            null) + ")",
                    args);
            deleted += mDatabase.delete(TABLE_DOCUMENTS, selection, args);
            mDatabase.setTransactionSuccessful();
            // TODO Remove mappingState.
            return deleted != 0;
        } finally {
            mDatabase.endTransaction();
        }
    }

    boolean disconnectDocuments(String selection, String[] args) {
        mDatabase.beginTransaction();
        try {
            final ContentValues values = new ContentValues();
            values.put(COLUMN_ROW_STATE, ROW_STATE_DISCONNECTED);
            values.putNull(COLUMN_DEVICE_ID);
            values.putNull(COLUMN_STORAGE_ID);
            values.putNull(COLUMN_OBJECT_HANDLE);
            final boolean updated = mDatabase.update(TABLE_DOCUMENTS, values, selection, args) != 0;
            mDatabase.setTransactionSuccessful();
            return updated;
        } finally {
            mDatabase.endTransaction();
        }
    }

    int getRowState(String documentId) throws FileNotFoundException {
        try (final Cursor cursor = mDatabase.query(
                TABLE_DOCUMENTS,
                strings(COLUMN_ROW_STATE),
                SELECTION_DOCUMENT_ID,
                strings(documentId),
                null,
                null,
                null)) {
            if (cursor.getCount() == 0) {
                throw new FileNotFoundException();
            }
            cursor.moveToNext();
            return cursor.getInt(0);
        }
    }

    void writeRowSnapshot(String documentId, ContentValues values) throws FileNotFoundException {
        try (final Cursor cursor = mDatabase.query(
                JOIN_ROOTS,
                strings("*"),
                SELECTION_DOCUMENT_ID,
                strings(documentId),
                null,
                null,
                null,
                "1")) {
            if (cursor.getCount() == 0) {
                throw new FileNotFoundException();
            }
            cursor.moveToNext();
            values.clear();
            DatabaseUtils.cursorRowToContentValues(cursor, values);
        }
    }

    void updateObject(String documentId, int deviceId, String parentId, int[] operationsSupported,
                      MtpObjectInfo info, Long size) {
        final ContentValues values = new ContentValues();
        getObjectDocumentValues(values, deviceId, parentId, operationsSupported, info, size);

        mDatabase.beginTransaction();
        try {
            mDatabase.update(
                    TABLE_DOCUMENTS,
                    values,
                    Document.COLUMN_DOCUMENT_ID + " = ?",
                    strings(documentId));
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Obtains a document that has already mapped but has unmapped children.
     * @param deviceId Device to find documents.
     * @return Identifier of found document or null.
     */
    @Nullable Identifier getUnmappedDocumentsParent(int deviceId) {
        final String fromClosure =
                TABLE_DOCUMENTS + " AS child INNER JOIN " +
                TABLE_DOCUMENTS + " AS parent ON " +
                "child." + COLUMN_PARENT_DOCUMENT_ID + " = " +
                "parent." + Document.COLUMN_DOCUMENT_ID;
        final String whereClosure =
                "parent." + COLUMN_DEVICE_ID + " = ? AND " +
                "parent." + COLUMN_ROW_STATE + " IN (?, ?) AND " +
                "parent." + COLUMN_DOCUMENT_TYPE + " != ? AND " +
                "child." + COLUMN_ROW_STATE + " = ?";
        try (final Cursor cursor = mDatabase.query(
                fromClosure,
                strings("parent." + COLUMN_DEVICE_ID,
                        "parent." + COLUMN_STORAGE_ID,
                        "parent." + COLUMN_OBJECT_HANDLE,
                        "parent." + Document.COLUMN_DOCUMENT_ID,
                        "parent." + COLUMN_DOCUMENT_TYPE),
                whereClosure,
                strings(deviceId, ROW_STATE_VALID, ROW_STATE_INVALIDATED, DOCUMENT_TYPE_DEVICE,
                        ROW_STATE_DISCONNECTED),
                null,
                null,
                null,
                "1")) {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToNext();
            return new Identifier(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getInt(2),
                    cursor.getString(3),
                    cursor.getInt(4));
        }
    }

    /**
     * Removes metadata except for data used by outgoingPersistedUriPermissions.
     */
    void cleanDatabase(Uri[] outgoingPersistedUris) {
        mDatabase.beginTransaction();
        try {
            final Set<String> ids = new HashSet<>();
            for (final Uri uri : outgoingPersistedUris) {
                String documentId = DocumentsContract.getDocumentId(uri);
                while (documentId != null) {
                    if (ids.contains(documentId)) {
                        break;
                    }
                    ids.add(documentId);
                    try (final Cursor cursor = mDatabase.query(
                            TABLE_DOCUMENTS,
                            strings(COLUMN_PARENT_DOCUMENT_ID),
                            SELECTION_DOCUMENT_ID,
                            strings(documentId),
                            null,
                            null,
                            null)) {
                        documentId = cursor.moveToNext() ? cursor.getString(0) : null;
                    }
                }
            }
            deleteDocumentsAndRoots(
                    Document.COLUMN_DOCUMENT_ID + " NOT IN " + getIdList(ids), null);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    int getLastBootCount() {
        try (final Cursor cursor = mDatabase.query(
                TABLE_LAST_BOOT_COUNT, strings(COLUMN_VALUE), null, null, null, null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            } else {
                return 0;
            }
        }
    }

    void setLastBootCount(int value) {
        Preconditions.checkArgumentNonnegative(value, "Boot count must not be negative.");
        mDatabase.beginTransaction();
        try {
            final ContentValues values = new ContentValues();
            values.put(COLUMN_VALUE, value);
            mDatabase.delete(TABLE_LAST_BOOT_COUNT, null, null);
            mDatabase.insert(TABLE_LAST_BOOT_COUNT, null, values);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context, int flags) {
            super(context,
                  flags == FLAG_DATABASE_IN_MEMORY ? null : DATABASE_NAME,
                  null,
                  DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(QUERY_CREATE_DOCUMENTS);
            db.execSQL(QUERY_CREATE_ROOT_EXTRA);
            db.execSQL(QUERY_CREATE_LAST_BOOT_COUNT);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOCUMENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROOT_EXTRA);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAST_BOOT_COUNT);
            onCreate(db);
        }
    }

    @VisibleForTesting
    static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }

    static void getDeviceDocumentValues(
            ContentValues values,
            ContentValues extraValues,
            MtpDeviceRecord device) {
        values.clear();
        values.put(COLUMN_DEVICE_ID, device.deviceId);
        values.putNull(COLUMN_STORAGE_ID);
        values.putNull(COLUMN_OBJECT_HANDLE);
        values.putNull(COLUMN_PARENT_DOCUMENT_ID);
        values.put(COLUMN_ROW_STATE, ROW_STATE_VALID);
        values.put(COLUMN_DOCUMENT_TYPE, DOCUMENT_TYPE_DEVICE);
        values.put(COLUMN_MAPPING_KEY, device.deviceKey);
        values.put(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        values.put(Document.COLUMN_DISPLAY_NAME, device.name);
        values.putNull(Document.COLUMN_SUMMARY);
        values.putNull(Document.COLUMN_LAST_MODIFIED);
        values.put(Document.COLUMN_ICON, R.drawable.ic_root_mtp);
        values.put(Document.COLUMN_FLAGS, getDocumentFlags(
//M.Add         device.operationsSupported,
                null,
                Document.MIME_TYPE_DIR,
                0,
                MtpConstants.PROTECTION_STATUS_NONE,
                DOCUMENT_TYPE_DEVICE));
        values.putNull(Document.COLUMN_SIZE);

        extraValues.clear();
        extraValues.put(Root.COLUMN_FLAGS, getRootFlags(device.operationsSupported));
        extraValues.putNull(Root.COLUMN_AVAILABLE_BYTES);
        extraValues.putNull(Root.COLUMN_CAPACITY_BYTES);
        extraValues.put(Root.COLUMN_MIME_TYPES, "");
    }

    /**
     * Gets {@link ContentValues} for the given root.
     * @param values {@link ContentValues} that receives values.
     * @param extraValues {@link ContentValues} that receives extra values for roots.
     * @param parentDocumentId Parent document ID.
     * @param operationsSupported Array of Operation code supported by the device.
     * @param root Root to be converted {@link ContentValues}.
     */
    static void getStorageDocumentValues(
            ContentValues values,
            ContentValues extraValues,
            String parentDocumentId,
            int[] operationsSupported,
            MtpRoot root) {
        values.clear();
        values.put(COLUMN_DEVICE_ID, root.mDeviceId);
        values.put(COLUMN_STORAGE_ID, root.mStorageId);
        values.putNull(COLUMN_OBJECT_HANDLE);
        values.put(COLUMN_PARENT_DOCUMENT_ID, parentDocumentId);
        values.put(COLUMN_ROW_STATE, ROW_STATE_VALID);
        values.put(COLUMN_DOCUMENT_TYPE, DOCUMENT_TYPE_STORAGE);
        values.put(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        values.put(Document.COLUMN_DISPLAY_NAME, root.mDescription);
        values.putNull(Document.COLUMN_SUMMARY);
        values.putNull(Document.COLUMN_LAST_MODIFIED);
        values.put(Document.COLUMN_ICON, R.drawable.ic_root_mtp);
        values.put(Document.COLUMN_FLAGS, getDocumentFlags(
                operationsSupported,
                Document.MIME_TYPE_DIR,
                0,
                MtpConstants.PROTECTION_STATUS_NONE,
                DOCUMENT_TYPE_STORAGE));
        values.put(Document.COLUMN_SIZE, root.mMaxCapacity - root.mFreeSpace);

        extraValues.put(Root.COLUMN_FLAGS, getRootFlags(operationsSupported));
        extraValues.put(Root.COLUMN_AVAILABLE_BYTES, root.mFreeSpace);
        extraValues.put(Root.COLUMN_CAPACITY_BYTES, root.mMaxCapacity);
        extraValues.put(Root.COLUMN_MIME_TYPES, "");
    }

    /**
     * Gets {@link ContentValues} for the given MTP object.
     * @param values {@link ContentValues} that receives values.
     * @param deviceId Device ID of the object.
     * @param parentId Parent document ID of the object.
     * @param info MTP object info. getCompressedSize will be ignored.
     * @param size 64-bit size of documents. Negative value is regarded as unknown size.
     */
    static void getObjectDocumentValues(
            ContentValues values, int deviceId, String parentId,
            int[] operationsSupported, MtpObjectInfo info, long size) {
        values.clear();
        final String mimeType = getMimeType(info);
        values.put(COLUMN_DEVICE_ID, deviceId);
        values.put(COLUMN_STORAGE_ID, info.getStorageId());
        values.put(COLUMN_OBJECT_HANDLE, info.getObjectHandle());
        values.put(COLUMN_PARENT_DOCUMENT_ID, parentId);
        values.put(COLUMN_ROW_STATE, ROW_STATE_VALID);
        values.put(COLUMN_DOCUMENT_TYPE, DOCUMENT_TYPE_OBJECT);
        values.put(Document.COLUMN_MIME_TYPE, mimeType);
        values.put(Document.COLUMN_DISPLAY_NAME, info.getName());
        values.putNull(Document.COLUMN_SUMMARY);
        values.put(
                Document.COLUMN_LAST_MODIFIED,
                info.getDateModified() != 0 ? info.getDateModified() : null);
        values.putNull(Document.COLUMN_ICON);
        values.put(Document.COLUMN_FLAGS, getDocumentFlags(
                operationsSupported, mimeType, info.getThumbCompressedSizeLong(),
                info.getProtectionStatus(), DOCUMENT_TYPE_OBJECT));
        if (size >= 0) {
            values.put(Document.COLUMN_SIZE, size);
        } else {
            values.putNull(Document.COLUMN_SIZE);
        }
    }

    private static String getMimeType(MtpObjectInfo info) {
        if (info.getFormat() == MtpConstants.FORMAT_ASSOCIATION) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        }

        final String formatCodeMimeType = MediaFile.getMimeTypeForFormatCode(info.getFormat());
        final String mediaFileMimeType = MediaFile.getMimeTypeForFile(info.getName());

        // Format code can be mapped with multiple mime types, e.g. FORMAT_MPEG is mapped with
        // audio/mp4 and video/mp4.
        // As file extension contains more information than format code, returns mime type obtained
        // from file extension if it is consistent with format code.
        if (mediaFileMimeType != null &&
                MediaFile.getFormatCode("", mediaFileMimeType) == info.getFormat()) {
            return mediaFileMimeType;
        }
        if (formatCodeMimeType != null) {
            return formatCodeMimeType;
        }
        if (mediaFileMimeType != null) {
            return mediaFileMimeType;
        }
        // We don't know the file type.
        return "application/octet-stream";
    }

    private static int getRootFlags(int[] operationsSupported) {
        int rootFlag = Root.FLAG_SUPPORTS_IS_CHILD;
        if (MtpDeviceRecord.isWritingSupported(operationsSupported)) {
            rootFlag |= Root.FLAG_SUPPORTS_CREATE;
        }
        return rootFlag;
    }

    private static int getDocumentFlags(
            @Nullable int[] operationsSupported, String mimeType, long thumbnailSize,
            int protectionState, @DocumentType int documentType) {
        int flag = 0;
        if (!mimeType.equals(Document.MIME_TYPE_DIR) &&
                MtpDeviceRecord.isWritingSupported(operationsSupported) &&
                protectionState == MtpConstants.PROTECTION_STATUS_NONE) {
            flag |= Document.FLAG_SUPPORTS_WRITE;
        }
        if (MtpDeviceRecord.isSupported(
                operationsSupported, MtpConstants.OPERATION_DELETE_OBJECT) &&
                (protectionState == MtpConstants.PROTECTION_STATUS_NONE ||
                 protectionState == MtpConstants.PROTECTION_STATUS_NON_TRANSFERABLE_DATA) &&
                documentType == DOCUMENT_TYPE_OBJECT) {
            flag |= Document.FLAG_SUPPORTS_DELETE;
        }
        if (mimeType.equals(Document.MIME_TYPE_DIR) &&
                MtpDeviceRecord.isWritingSupported(operationsSupported) &&
                protectionState == MtpConstants.PROTECTION_STATUS_NONE) {
            flag |= Document.FLAG_DIR_SUPPORTS_CREATE;
        }
        if (thumbnailSize > 0) {
            flag |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }
        return flag;
    }

    static String[] strings(Object... args) {
        final String[] results = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            results[i] = Objects.toString(args[i]);
        }
        return results;
    }

    private static String getIdList(Set<String> ids) {
        String result = "(";
        for (final String id : ids) {
            if (result.length() > 1) {
                result += ",";
            }
            result += id;
        }
        result += ")";
        return result;
    }
}
