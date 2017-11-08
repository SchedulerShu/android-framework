/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.internal.telephony.cdma;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;

import com.mediatek.internal.telephony.ITelephonyEx;

/**
 * Utils Class for CDMA OMH SMS.
 */
public class CdmaOmhSmsUtils {
    /** Invalid message ID. */
    public static final int INVALID_MESSAGE_ID = -1;

    /** Invalid broadcase SMS config. */
    public static final int INVALID_BROADCAST_CONFIG = -1;

    /* log tag */
    private static final String TAG = "CdmaOmhSmsUtils";

    /**
     * Check if the card is OMH card.
     *
     * @param subId the sub id for check.
     *
     * @return true if the card is OMH card.
     */
    public static boolean isOmhCard(int subId) {
        boolean isOmh = false;
        try {
            ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            isOmh = iTel.isOmhCard(subId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Rlog.d(TAG, "isOmhCard " + isOmh);
        return isOmh;
    }

    /**
     * Calculate the next message id, starting at 1 and iteratively incrementing
     * within the range 1..65535 remembering the state via a persistent system
     * property. (See C.S0015-B, v2.0, 4.3.1.5)
     * save the message id in R-UIM card, (See
     * C.S0023-D_v1.0 3.4.29)
     *
     * @param subId the sub id for check.
     *
     * @return the saved message ID
     */
    public static int getNextMessageId(int subId) {
        int id = INVALID_MESSAGE_ID;
        try {
            ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            id = iTel.getNextMessageId(subId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Rlog.d(TAG, "getNextMessageId " + id);
        return id;
    }

    /**
     * Get broadcast SMS configure from RUIM.
     *
     * @param subId the sub id for check
     * @param userCategory  service category
     * @param userPriority  priority indicator
     *
     * @return 0=Disallow, 2=Allow, -1=Unknown
     */
    public static int getBcsmsCfgFromRuim(int subId, int userCategory, int userPriority) {
        int ret = -1;
        try {
            ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            ret = iTel.getBcsmsCfgFromRuim(subId, userCategory, userPriority);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Rlog.d(TAG, "getBcsmsCfgFromRuim " + ret);
        return ret;
    }
};