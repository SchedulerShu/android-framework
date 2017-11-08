/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package org.gsma.joyn.contacts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.provider.ContactsContract;

import org.gsma.joyn.ICoreServiceWrapper;
import org.gsma.joyn.Logger;

/**
 * Contacts service offers additional methods to manage RCS info in the
 * local address book.
 *
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 *
 * @author Jean-Marc AUFFRET
 */
public class ContactsService extends JoynService {
    /**
     * API
     */
    private IContactsService api = null;
    private Context mContext = null;
    public static final String TAG = "TAPI-ContactsService";
    /**
     * Constructor
     *
     * @param ctx Application context
     * @param listener Service listener
     */
    public ContactsService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        mContext = ctx;
    }

    /**
     * Connects to the API
     */
    public void connect() {
        Logger.i(TAG, "ContactsService connect() entry");
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName("com.orangelabs.rcs", "com.orangelabs.rcs.service.RcsCoreService");
        intent.setComponent(cmp);
        ctx.bindService(intent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        Logger.i(TAG, "ContactsService disconnect() entry");
        try {
            ctx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Set API interface
     *
     * @param api API interface
     */
    protected void setApi(IInterface api) {
        super.setApi(api);

        this.api = (IContactsService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Logger.i(TAG, "onServiceConnected entry " + className);
            ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
            IBinder binder = null;
            try {
                binder = mCoreServiceWrapperBinder.getContactsServiceBinder();
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            setApi(IContactsService.Stub.asInterface(binder));
            if (serviceListener != null) {
                serviceListener.onServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Logger.i(TAG, "onServiceDisconnected entry " + className);
            setApi(null);
            if (serviceListener != null) {
                serviceListener.onServiceDisconnected(Error.CONNECTION_LOST);
            }
        }
    };

    /**
     * Returns the joyn contact infos from its contact ID (i.e. MSISDN)
     *
     * @param contactId Contact ID
     * @return Contact
     * @throws JoynServiceException
     */
    public JoynContact getJoynContact(String contactId) throws JoynServiceException {
        Logger.i(TAG, "getJoynContact entry Id=" + contactId);
        if (api != null) {
            try {
                return api.getJoynContact(contactId);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Returns the list of joyn contacts
     *
     * @return List of contacts
     * @throws JoynServiceException
     */
    public Set<JoynContact> getJoynContacts() throws JoynServiceException {
        Logger.i(TAG, "getJoynContacts entry ");
        if (api != null) {
            try {
                Set<JoynContact> result = new HashSet<JoynContact>();
                List<JoynContact> contacts = api.getJoynContacts();
                for (int i = 0; i < contacts.size(); i++) {
                    JoynContact contact = contacts.get(i);
                    result.add(contact);
                }
                Logger.i(TAG, "getJoynContacts returning result = " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Returns the list of online contacts (i.e. registered)
     *
     * @return List of contacts
     * @throws JoynServiceException
     */
    public Set<JoynContact> getJoynContactsOnline() throws JoynServiceException {
        Logger.i(TAG, "getJoynContactsOnline() entry ");
        if (api != null) {
            try {
                Set<JoynContact> result = new HashSet<JoynContact>();
                result.addAll(api.getJoynContactsOnline());
                Logger.i(TAG, "getJoynContactsOnline() returning result = " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Returns the list of contacts supporting a given extension or service ID
     *
     * @param serviceId Service ID
     * @return List of contacts
     * @throws JoynServiceException
     */
    public Set<JoynContact> getJoynContactsSupporting(String serviceId) throws JoynServiceException {
        Logger.i(TAG, "getJoynContactsSupporting() entry ");
        if (api != null) {
            try {
                Set<JoynContact> result = new HashSet<JoynContact>();
                result.addAll(api.getJoynContactsSupporting(serviceId));
                Logger.i(TAG, "getJoynContactsSupporting() returning result = " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Returns the vCard of a contact. The method returns the complete filename including the path of the visit
     * card. The filename has the file extension .vcf and is generated from the native address book
     * vCard URI (see Android SDK attribute ContactsContract.Contacts.CONTENT_VCARD_URI which returns
     * the referenced contact formatted as a vCard when opened through openAssetFileDescriptor(Uri, String)).
     *
     * @param contactUri Contact URI of the contact in the native address book
     * @return Filename of vCard
     * @throws JoynServiceException
     */
    public String getVCard(Uri contactUri) throws JoynServiceException {
        Logger.i(TAG, "getVCard() entry ");
        String fileName = null;
        Cursor cursor = mContext.getContentResolver().query(contactUri, null, null, null, null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            Uri vCardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
            Logger.i(TAG, "getVCard() uri= " + vCardUri);
            AssetFileDescriptor fd;
            try {
                fd = mContext.getContentResolver().openAssetFileDescriptor(vCardUri, "r");
                FileInputStream fis = fd.createInputStream();
                byte[] buf = new byte[(int) fd.getDeclaredLength()];
                if(buf.length > 0){
                    int n = fis.read(buf);
                String Vcard = new String(buf);

                fileName = Environment.getExternalStorageDirectory().toString() + File.separator + name + ".vcf";
                Logger.i(TAG, "getVCard() filename= " + fileName + ",bytes read:" + n);
                File vCardFile = new File(fileName);

                if (vCardFile.exists())
                    vCardFile.delete();

                FileOutputStream mFileOutputStream = new FileOutputStream(vCardFile, true);
                mFileOutputStream.write(Vcard.toString().getBytes());
                mFileOutputStream.close();
                }
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        cursor.close();
        return fileName;
    }

    /**
     * Get all IM blocked contacts that stored in the local copy
     * @return
     */
    public List<String> getImBlockedContactsFromLocal() {
        Logger.i(TAG, "getImBlockedContactsFromLocal entry");
        if (api != null) {
            try {
                return api.getImBlockedContactsFromLocal();
            } catch (Exception e) {
                //throw new JoynServiceException(e.getMessage());
                return null;
            }
        } else {
            //throw new JoynServiceNotAvailableException();
            return null;
        }
    }

    /**
     * Get whether the "IM" feature is enabled or not for the contact
     *
     * @param contact
     * @return flag indicating if IM sessions with the contact are enabled or not
     */
    public boolean isImBlockedForContact(String contact) throws JoynServiceException {
        if (api != null) {
            try {
                return api.isImBlockedForContact(contact);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }
    /**
     * Get the contacts that are "IM blocked"
     *
     * @return list containing all contacts that are "IM blocked"
     */
    public List<String> getImBlockedContacts() throws JoynServiceException {
        if (api != null) {
            try {
                return api.getImBlockedContacts();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    public String getTimeStampForBlockedContact(String contact) throws JoynServiceException {
        Logger.i(TAG, "getTimeStampForBlockedContact entry" + contact);
        if (api != null) {
            try {
                return api.getTimeStampForBlockedContact(contact);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /*public static Set<String> getAllBlockedList(){
        Logger.i(TAG, "getAllBlockedList entry");
        if (api != null) {
            try {
                return api.getAllBlockedList();
            } catch(Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }*/

    /**
     * Mark the contact as "blocked for IM"
     *
     * @param contact
     * @param flag indicating if we enable or disable the IM sessions with the contact
     */
    public void setImBlockedForContact(String contact, boolean flag) throws JoynServiceException {
        Logger.i(TAG, "setImBlockedForContact entry" + contact + ",flag=" + flag);
        if (api != null) {
            try {
                api.setImBlockedForContact(contact, flag);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Mark the contact as "blocked for FT"
     *
     * @param contact
     * @param flag indicating if we enable or disable the FT sessions with the contact
     */
    public void setFtBlockedForContact(String contact, boolean flag) throws JoynServiceException {
        Logger.i(TAG, "setFtBlockedForContact entry" + contact + ",flag=" + flag);
        if (api != null) {
            try {
                api.setFtBlockedForContact(contact, flag);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Check if number provided is a valid number for RCS
     * <br>It is not valid if :
     * <li>well formatted (not digits only or '+')
     * <li>minimum length
     *
     * @param number Phone number
     * @return Returns true if it is a RCS valid number
     */
    public boolean isRcsValidNumber(String number) throws JoynServiceException {
        if (api != null) {
            try {
                return api.isRcsValidNumber(number);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Get the registration state of a contact in the EAB
     *
     * @param contact Contact
     * @return Contact info
     */
    public int getRegistrationState(String contact) throws JoynServiceException {
        if (api != null) {
            try {
                return api.getRegistrationState(contact);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

    /**
     * Load the IM blocked contacts to the local copy
     */
    public void loadImBlockedContactsToLocal() throws JoynServiceException {
        if (api != null) {
            try {
                api.loadImBlockedContactsToLocal();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }


}
