/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.internal.telephony;

// MTK-START
import android.os.SystemProperties;
// MTK-END

/**
 * SMS Constants and must be the same as the corresponding
 * deprecated version in SmsMessage.
 *
 * @hide
 */
public class SmsConstants {
    /** User data text encoding code unit size */
    public static final int ENCODING_UNKNOWN = 0;
    public static final int ENCODING_7BIT = 1;
    public static final int ENCODING_8BIT = 2;
    public static final int ENCODING_16BIT = 3;

    /** The maximum number of payload septets per message */
    public static final int MAX_USER_DATA_SEPTETS = 160;

    /**
     * The maximum number of payload septets per message if a user data header
     * is present.  This assumes the header only contains the
     * CONCATENATED_8_BIT_REFERENCE element.
     */
    public static final int MAX_USER_DATA_SEPTETS_WITH_HEADER = 153;

    /**
     * This value is not defined in global standard. Only in Korea, this is used.
     */
    public static final int ENCODING_KSC5601 = 4;

    /** The maximum number of payload bytes per message */
    public static final int MAX_USER_DATA_BYTES = 140;

    /**
     * The maximum number of payload bytes per message if a user data header
     * is present.  This assumes the header only contains the
     * CONCATENATED_8_BIT_REFERENCE element.
     */
    public static final int MAX_USER_DATA_BYTES_WITH_HEADER = 134;

    /**
     * SMS Class enumeration.
     * See TS 23.038.
     */
    public enum MessageClass{
        UNKNOWN, CLASS_0, CLASS_1, CLASS_2, CLASS_3;
    }

    /**
     * Indicates unknown format SMS message.
     * @hide pending API council approval
     */
    public static final String FORMAT_UNKNOWN = "unknown";

    /**
     * Indicates a 3GPP format SMS message.
     * @hide pending API council approval
     */
    public static final String FORMAT_3GPP = "3gpp";

    /**
     * Indicates a 3GPP2 format SMS message.
     * @hide pending API council approval
     */
    public static final String FORMAT_3GPP2 = "3gpp2";

    // MTK-START
    /**
     * Indicates isPrimary for ETWS..
     * @hide pending API council approval
     */
    public static final String IS_EMERGENCY_CB_PRIMARY = "isPrimary";

    private static final boolean IS_PRIVACY_PROTECTION_LOCK_SUPPORT =
            SystemProperties.get("ro.mtk_privacy_protection_lock").equals("1");

    private static final boolean IS_WAPPUSH_SUPPORT =
            SystemProperties.get("ro.mtk_wappush_support").equals("1");

    public static boolean isPrivacyLockSupport() {
        return IS_PRIVACY_PROTECTION_LOCK_SUPPORT;
    }

    public static boolean isWapPushSupport() {
        return IS_WAPPUSH_SUPPORT;
    }
    // MTK-END
}
