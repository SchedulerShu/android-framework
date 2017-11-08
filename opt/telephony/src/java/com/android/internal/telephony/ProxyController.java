/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
// MTK-START
import android.telephony.SubscriptionManager;
// MTK-END
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.uicc.UiccController;

//Add by MTK,@{
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.worldphone.WorldMode;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
//}@

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;

public class ProxyController {
    static final String LOG_TAG = "ProxyController";

    private static final int EVENT_NOTIFICATION_RC_CHANGED        = 1;
    private static final int EVENT_START_RC_RESPONSE        = 2;
    private static final int EVENT_APPLY_RC_RESPONSE        = 3;
    private static final int EVENT_FINISH_RC_RESPONSE       = 4;
    private static final int EVENT_TIMEOUT                  = 5;
    private static final int EVENT_RADIO_AVAILABLE          = 6;

    private static final int SET_RC_STATUS_IDLE             = 0;
    private static final int SET_RC_STATUS_STARTING         = 1;
    private static final int SET_RC_STATUS_STARTED          = 2;
    private static final int SET_RC_STATUS_APPLYING         = 3;
    private static final int SET_RC_STATUS_SUCCESS          = 4;
    private static final int SET_RC_STATUS_FAIL             = 5;

    // The entire transaction must complete within this amount of time
    // or a FINISH will be issued to each Logical Modem with the old
    // Radio Access Family.
    private static final int SET_RC_TIMEOUT_WAITING_MSEC    = (45 * 1000);
    //Add by MTK,@{
    private static final String MTK_C2K_SUPPORT = "ro.boot.opt_c2k_support";
    private static final int RC_RETRY_CAUSE_NONE                  = 0;
    private static final int RC_RETRY_CAUSE_WORLD_MODE_SWITCHING  = 1;
    private static final int RC_RETRY_CAUSE_CAPABILITY_SWITCHING  = 2;
    private static final int RC_RETRY_CAUSE_IN_CALL               = 3;
    private static final int RC_RETRY_CAUSE_RADIO_UNAVAILABLE     = 4;
    private static final int RC_RETRY_CAUSE_AIRPLANE_MODE         = 5;
    //}@

    //***** Class Variables
    private static ProxyController sProxyController;

    private Phone[] mPhones;

    private UiccController mUiccController;

    private CommandsInterface[] mCi;

    private Context mContext;

    private PhoneSwitcher mPhoneSwitcher;

    //UiccPhoneBookController to use proper IccPhoneBookInterfaceManagerProxy object
    private UiccPhoneBookController mUiccPhoneBookController;

    //PhoneSubInfoController to use proper PhoneSubInfoProxy object
    private PhoneSubInfoController mPhoneSubInfoController;

    //UiccSmsController to use proper IccSmsInterfaceManager object
    private UiccSmsController mUiccSmsController;

    WakeLock mWakeLock;

    // record each phone's set radio capability status
    private int[] mSetRadioAccessFamilyStatus;
    private int mRadioAccessFamilyStatusCounter;
    private boolean mTransactionFailed = false;

    private String[] mCurrentLogicalModemIds;
    private String[] mNewLogicalModemIds;

    // Allows the generation of unique Id's for radio capability request session  id
    private AtomicInteger mUniqueIdGenerator = new AtomicInteger(new Random().nextInt());

    // on-going radio capability request session id
    private int mRadioCapabilitySessionId;

    // Record new and old Radio Access Family (raf) configuration.
    // The old raf configuration is used to restore each logical modem raf when FINISH is
    // issued if any requests fail.
    private int[] mNewRadioAccessFamily;
    private int[] mOldRadioAccessFamily;

    //Add by MTK,@{
    private boolean mIsCapSwitching;
    private boolean mHasRegisterWorldModeReceiver = false;
    private boolean mHasRegisterPhoneStateReceiver = false;
    private boolean mHasRegisterEccStateReceiver = false;
    RadioAccessFamily[] mNextRafs = null;
    private int mSetRafRetryCause;

    // Exception counter..
    private int onExceptionCount = 0;
    // Time stamp
    private long mDoSimSwitchTime = 0;
    //}@


    //***** Class Methods
    public static ProxyController getInstance(Context context, Phone[] phone,
            UiccController uiccController, CommandsInterface[] ci, PhoneSwitcher ps) {
        if (sProxyController == null) {
            sProxyController = new ProxyController(context, phone, uiccController, ci, ps);
        }
        return sProxyController;
    }

    public static ProxyController getInstance() {
        return sProxyController;
    }

    //Add by MTK,@{
    protected BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            logd("onReceive: action=" + action);
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                boolean mAirplaneModeOn = intent.getBooleanExtra("state", false) ? true : false;
                logd("ACTION_AIRPLANE_MODE_CHANGED, enabled = " + mAirplaneModeOn);
                if (!mAirplaneModeOn && (mSetRafRetryCause == RC_RETRY_CAUSE_AIRPLANE_MODE)) {
                    mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                    try {
                        if (!setRadioCapability(mNextRafs)) {
                            sendCapabilityFailBroadcast();
                        }
                    } catch (java.lang.RuntimeException e) {
                        sendCapabilityFailBroadcast();
                    }
                }
            }
        }
    };
    //}@

    private ProxyController(Context context, Phone[] phone, UiccController uiccController,
            CommandsInterface[] ci, PhoneSwitcher phoneSwitcher) {
        logd("Constructor - Enter");

        mContext = context;
        mPhones = phone;
        mUiccController = uiccController;
        mCi = ci;
        mPhoneSwitcher = phoneSwitcher;

        mUiccPhoneBookController = new UiccPhoneBookController(mPhones);
        mPhoneSubInfoController = new PhoneSubInfoController(mContext, mPhones);
        mUiccSmsController = new UiccSmsController(mPhones);
        mSetRadioAccessFamilyStatus = new int[mPhones.length];
        mNewRadioAccessFamily = new int[mPhones.length];
        mOldRadioAccessFamily = new int[mPhones.length];
        mCurrentLogicalModemIds = new String[mPhones.length];
        mNewLogicalModemIds = new String[mPhones.length];

        // wake lock for set radio capability
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mWakeLock.setReferenceCounted(false);

        // Clear to be sure we're in the initial state
        clearTransaction();
        for (int i = 0; i < mPhones.length; i++) {
            mPhones[i].registerForRadioCapabilityChanged(
                    mHandler, EVENT_NOTIFICATION_RC_CHANGED, null);
        }

        //Add by MTK,@{
        // airplaneMode retry
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        context.registerReceiver(mBroadcastReceiver, filter);
        //}@

        logd("Constructor - Exit");
    }

    public void updateDataConnectionTracker(int sub) {
        mPhones[sub].updateDataConnectionTracker();
    }

    public void enableDataConnectivity(int sub) {
        mPhones[sub].setInternalDataEnabled(true, null);
    }

    public void disableDataConnectivity(int sub,
            Message dataCleanedUpMsg) {
        mPhones[sub].setInternalDataEnabled(false, dataCleanedUpMsg);
    }

    public void updateCurrentCarrierInProvider(int sub) {
        mPhones[sub].updateCurrentCarrierInProvider();
    }

    public void registerForAllDataDisconnected(int subId, Handler h, int what, Object obj) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);

        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            mPhones[phoneId].registerForAllDataDisconnected(h, what, obj);
        }
    }

    public void unregisterForAllDataDisconnected(int subId, Handler h) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);

        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            mPhones[phoneId].unregisterForAllDataDisconnected(h);
        }
    }

    public boolean isDataDisconnected(int subId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);

        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            return mPhones[phoneId].mDcTracker.isDisconnected();
        } else {
            // if we can't find a phone for the given subId, it is disconnected.
            return true;
        }
    }

    /**
     * Get phone radio type and access technology.
     *
     * @param phoneId which phone you want to get
     * @return phone radio type and access technology for input phone ID
     */
    public int getRadioAccessFamily(int phoneId) {
        if (phoneId >= mPhones.length) {
            return RadioAccessFamily.RAF_UNKNOWN;
        } else {
            return mPhones[phoneId].getRadioAccessFamily();
        }
    }

    /**
     * Set phone radio type and access technology for each phone.
     *
     * @param rafs an RadioAccessFamily array to indicate all phone's
     *        new radio access family. The length of RadioAccessFamily
     *        must equal to phone count.
     * @return false if another session is already active and the request is rejected.
     */
    public boolean setRadioCapability(RadioAccessFamily[] rafs) {
        if (rafs.length != mPhones.length) {
            throw new RuntimeException("Length of input rafs must equal to total phone count");
        }
        //Add by MTK,@{
        // check if capability switch disabled
        if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false) == true) {
            completeRadioCapabilityTransaction();
            logd("skip switching because mtk_disable_cap_switch is true");
            return true;
        }
        mNextRafs = rafs;

        // check world mode switching
        if (WorldPhoneUtil.isWorldPhoneSwitching()) {
            logd("world mode switching");
            if (!mHasRegisterWorldModeReceiver) {
                registerWorldModeReceiver();
            }
            mSetRafRetryCause = RC_RETRY_CAUSE_WORLD_MODE_SWITCHING;
            return true;
        } else if (mSetRafRetryCause == RC_RETRY_CAUSE_WORLD_MODE_SWITCHING) {
            if (mHasRegisterWorldModeReceiver) {
                unRegisterWorldModeReceiver();
                mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                // mNextRafs = null;
            }
        }

        // check if FTA mode
        if (SystemProperties.getInt("gsm.gcf.testmode", 0) == 2) {
            completeRadioCapabilityTransaction();
            logd("skip switching because FTA mode");
            return true;
        }
        // check if EM disable mode
        if (SystemProperties.getInt("persist.radio.simswitch.emmode", 1) == 0) {
            completeRadioCapabilityTransaction();
            logd("skip switching because EM disable mode");
            return true;
        }
        // check if in call
        if (TelephonyManager.getDefault().getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            //throw new RuntimeException("in call, fail to set RAT for phones");
            logd("setCapability in calling, fail to set RAT for phones");
            if (!mHasRegisterPhoneStateReceiver) {
                registerPhoneStateReceiver();
            }
            mSetRafRetryCause = RC_RETRY_CAUSE_IN_CALL;
            mNextRafs = rafs;
            return false;
        } else if (isEccInProgress()) {
            logd("setCapability in ECC, fail to set RAT for phones");
            if (!mHasRegisterEccStateReceiver) {
                registerEccStateReceiver();
            }
            mSetRafRetryCause = RC_RETRY_CAUSE_IN_CALL;
            return false;
        } else if (mSetRafRetryCause == RC_RETRY_CAUSE_IN_CALL) {
            if (mHasRegisterPhoneStateReceiver) {
                unRegisterPhoneStateReceiver();
                mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                // mNextRafs = null;
            }
            if (mHasRegisterEccStateReceiver) {
                unRegisterEccStateReceiver();
                mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                // mNextRafs = null;
            }
        }
        int airplaneMode = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        if (airplaneMode > 0) {
            //throw new RuntimeException("airplane mode is on, fail to set RAT for phones");
            logd("airplane mode is on, fail to set RAT for phones");
            mSetRafRetryCause = RC_RETRY_CAUSE_AIRPLANE_MODE;
            mNextRafs = rafs;
            return false;
        }

        // check if still switching
        if (mIsCapSwitching == true) {
            //throw new RuntimeException("is still switching");
            logd("keep it and return,because capability swithing");
            mSetRafRetryCause = RC_RETRY_CAUSE_CAPABILITY_SWITCHING;
            mNextRafs = rafs;
            return true;
        } else if (mSetRafRetryCause == RC_RETRY_CAUSE_CAPABILITY_SWITCHING) {
            logd("setCapability, mIsCapSwitching is not switching, can switch");
            mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
            // mNextRafs = null;
        }

        // check radio available
        for (int i = 0; i < mPhones.length; i++) {
            if (!mPhones[i].isRadioAvailable()) {
                //throw new RuntimeException("Phone" + i + " is not available");
                mSetRafRetryCause = RC_RETRY_CAUSE_RADIO_UNAVAILABLE;
                mCi[i].registerForAvailable(mHandler, EVENT_RADIO_AVAILABLE, null);
                logd("setCapability fail,Phone" + i + " is not available");
                mNextRafs = rafs;
                return false;
            } else if (mSetRafRetryCause == RC_RETRY_CAUSE_RADIO_UNAVAILABLE) {
                //logd("setCapability,Phone" + i + " is available");
                mCi[i].unregisterForAvailable(mHandler);
                if (i == mPhones.length - 1) {
                    mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                }
                // mNextRafs = null;
            }
        }

        logd("setCapability,All Phones is available");

        int switchStatus = Integer.valueOf(
                SystemProperties.get(PhoneConstants.PROPERTY_CAPABILITY_SWITCH, "1"));
        // check parameter
        boolean bIsboth3G = false;
        boolean bIsMajorPhone = false;
        int newMajorPhoneId = 0;
        for (int i = 0; i < rafs.length; i++) {
            bIsMajorPhone = false;
            if (SystemProperties.getInt("ro.boot.opt_lte_support", 0) == 1) {
                if ((rafs[i].getRadioAccessFamily() & RadioAccessFamily.RAF_LTE)
                        == RadioAccessFamily.RAF_LTE) {
                    bIsMajorPhone = true;
                }
            } else {
            if ((rafs[i].getRadioAccessFamily() & RadioAccessFamily.RAF_UMTS)
                    == RadioAccessFamily.RAF_UMTS &&
                 (rafs[i].getRadioAccessFamily() & RadioAccessFamily.RAF_GPRS)
                    == RadioAccessFamily.RAF_GPRS) {
                    bIsMajorPhone = true;
                }
            }
            if (bIsMajorPhone) {
                newMajorPhoneId = rafs[i].getPhoneId();
                if (newMajorPhoneId == (switchStatus - 1)) {
                    logd("no change, skip setRadioCapability");
                    mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                    mNextRafs = null;
                    completeRadioCapabilityTransaction();
                    return true;
                }
                if (bIsboth3G) {
                    logd("set more than one 3G phone, fail");
                    throw new RuntimeException("input parameter is incorrect");
                } else {
                    bIsboth3G = true;
                }
            }
        }
        if (bIsboth3G == false) {
            for (int i = 0; i < rafs.length; i++) {
                bIsMajorPhone = false;
                if ((rafs[i].getRadioAccessFamily() & RadioAccessFamily.RAF_UMTS)
                        == RadioAccessFamily.RAF_UMTS) {
                        bIsMajorPhone = true;
                    }

                if (bIsMajorPhone) {
                    newMajorPhoneId = rafs[i].getPhoneId();
                    if (newMajorPhoneId == (switchStatus - 1)) {
                        logd("no change, skip setRadioCapability.[UMTS]");
                        mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                        mNextRafs = null;
                        completeRadioCapabilityTransaction();
                        return true;
                    }
                    if (bIsboth3G) {
                        logd("set more than one 3G phone, fail.[UMTS]");
                        throw new RuntimeException("input parameter is incorrect");
                    } else {
                        bIsboth3G = true;
                    }
                }
            }

            if (bIsboth3G == false) {
                throw new RuntimeException("input parameter is incorrect - no 3g phone");
            }
        }

        // External SIM [Start]
        if (SystemProperties.getInt("ro.mtk_external_sim_support", 0) == 1) {
            // To avoid smart switch before vsim plug in
            for (int i = 0; i < mPhones.length; i++) {
                String isVsimEnabled =
                        TelephonyManager.getDefault().getTelephonyProperty(
                        i, TelephonyProperties.PROPERTY_EXTERNAL_SIM_ENABLED, "0");
                String isVsimInserted =
                        TelephonyManager.getDefault().getTelephonyProperty(
                        i, TelephonyProperties.PROPERTY_EXTERNAL_SIM_INSERTED, "0");
                int defaultPhoneId =
                        SubscriptionManager.getPhoneId(
                        SubscriptionManager.getDefaultDataSubscriptionId());

                if ("1".equals(isVsimEnabled)
                        && ("0".equals(isVsimInserted) || "".equals(isVsimInserted))
                        && (newMajorPhoneId != defaultPhoneId)) {
                    throw new RuntimeException("vsim not ready, can't switch to another sim!");
                }
            }

            // To avoid SIM switch for Remote SIM type
            int mainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
            String isVsimEnabledOnMain =
                    TelephonyManager.getDefault().getTelephonyProperty(
                    mainPhoneId, TelephonyProperties.PROPERTY_EXTERNAL_SIM_ENABLED, "0");
            String mainPhoneIdSimType =
                    TelephonyManager.getDefault().getTelephonyProperty(
                    mainPhoneId, TelephonyProperties.PROPERTY_EXTERNAL_SIM_INSERTED, "0");

            if (isVsimEnabledOnMain.equals("1") && mainPhoneIdSimType.equals("2")) {
                throw new RuntimeException("vsim enabled, can't switch to another sim!");
            }
        }
        // External SIM [End]
        switch (RadioCapabilitySwitchUtil.isNeedSwitchInOpPackage(mPhones, rafs)) {
            case RadioCapabilitySwitchUtil.DO_SWITCH:
                logd("do setRadioCapability");
                break;
            case RadioCapabilitySwitchUtil.NOT_SWITCH:
                logd("no change in op check, skip setRadioCapability");
                completeRadioCapabilityTransaction();
                return true;
            case RadioCapabilitySwitchUtil.NOT_SWITCH_SIM_INFO_NOT_READY:
                logd("Sim status/info is not ready, skip setRadioCapability");
                return true;
            default:
                logd("should not be here...!!");
                return true;
        }
        //}@

        // Check if there is any ongoing transaction and throw an exception if there
        // is one as this is a programming error.
        synchronized (mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < mPhones.length; i++) {
                if (mSetRadioAccessFamilyStatus[i] != SET_RC_STATUS_IDLE) {
                    // TODO: The right behaviour is to cancel previous request and send this.
                    loge("setRadioCapability: Phone[" + i + "] is not idle. Rejecting request.");
                    return false;
                }
            }
        }

        // Check we actually need to do anything
        boolean same = true;
        for (int i = 0; i < mPhones.length; i++) {
            if (mPhones[i].getRadioAccessFamily() != rafs[i].getRadioAccessFamily()) {
                same = false;
            }
        }
        if (same) {
            // All phones are already set to the requested raf
            logd("setRadioCapability: Already in requested configuration, nothing to do.");
            // It isn't really an error, so return true - everything is OK.
            return true;
        }

        //Add by MTK,@{
        if (!WorldPhoneUtil.isWorldModeSupport() && WorldPhoneUtil.isWorldPhoneSupport()) {
            PhoneFactory.getWorldPhone().notifyRadioCapabilityChange(newMajorPhoneId);
        }
        //}@

        // Clear to be sure we're in the initial state
        clearTransaction();

        // Keep a wake lock until we finish radio capability changed
        mWakeLock.acquire();

        return doSetRadioCapabilities(rafs);
    }

    private boolean doSetRadioCapabilities(RadioAccessFamily[] rafs) {
        // A new sessionId for this transaction
        mRadioCapabilitySessionId = mUniqueIdGenerator.getAndIncrement();

        // Start timer to make sure all phones respond within a specific time interval.
        // Will send FINISH if a timeout occurs.
        Message msg = mHandler.obtainMessage(EVENT_TIMEOUT, mRadioCapabilitySessionId, 0);
        mHandler.sendMessageDelayed(msg, SET_RC_TIMEOUT_WAITING_MSEC);

        //Add by MTK,@{
        mDoSimSwitchTime = System.currentTimeMillis() / 1000;
        SystemProperties.set("ril.time.stamp", Long.toString(mDoSimSwitchTime));
        SystemProperties.set("ril.switch.session.id", Integer.toString(mRadioCapabilitySessionId));
        logd("setRadioCapability: timestamp =" + mDoSimSwitchTime);
        mIsCapSwitching = true;
        //}@

        synchronized (mSetRadioAccessFamilyStatus) {
            logd("setRadioCapability: new request session id=" + mRadioCapabilitySessionId);
            resetRadioAccessFamilyStatusCounter();
            onExceptionCount = 0;
            for (int i = 0; i < rafs.length; i++) {
                int phoneId = rafs[i].getPhoneId();
                //logd("setRadioCapability: phoneId=" + phoneId + " status=STARTING");
                mSetRadioAccessFamilyStatus[phoneId] = SET_RC_STATUS_STARTING;
                mOldRadioAccessFamily[phoneId] = mPhones[phoneId].getRadioAccessFamily();
                int requestedRaf = rafs[i].getRadioAccessFamily();
                // TODO Set the new radio access family to the maximum of the requested & supported
                // int supportedRaf = mPhones[i].getRadioAccessFamily();
                // mNewRadioAccessFamily[phoneId] = requestedRaf & supportedRaf;
                mNewRadioAccessFamily[phoneId] = requestedRaf;

                mCurrentLogicalModemIds[phoneId] = mPhones[phoneId].getModemUuId();
                // get the logical mode corresponds to new raf requested and pass the
                // same as part of SET_RADIO_CAP APPLY phase
                mNewLogicalModemIds[phoneId] = getLogicalModemIdFromRaf(requestedRaf);
                logd("setRadioCapability: phoneId=" + phoneId + " status=STARTING" +
                        "mOldRadioAccessFamily[" + phoneId + "]=" +
                        mOldRadioAccessFamily[phoneId] +
                        "mNewRadioAccessFamily[" + phoneId + "]=" +
                        mNewRadioAccessFamily[phoneId]);
                sendRadioCapabilityRequest(
                        phoneId,
                        mRadioCapabilitySessionId,
                        RadioCapability.RC_PHASE_START,
                        mOldRadioAccessFamily[phoneId],
                        mCurrentLogicalModemIds[phoneId],
                        RadioCapability.RC_STATUS_NONE,
                        EVENT_START_RC_RESPONSE);
            }
        }

        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            logd("handleMessage msg.what=" + msg.what);
            switch (msg.what) {
                case EVENT_START_RC_RESPONSE:
                    onStartRadioCapabilityResponse(msg);
                    break;

                case EVENT_APPLY_RC_RESPONSE:
                    onApplyRadioCapabilityResponse(msg);
                    break;

                case EVENT_NOTIFICATION_RC_CHANGED:
                    onNotificationRadioCapabilityChanged(msg);
                    break;

                case EVENT_FINISH_RC_RESPONSE:
                    onFinishRadioCapabilityResponse(msg);
                    break;

                case EVENT_TIMEOUT:
                    onTimeoutRadioCapability(msg);
                    break;

                case EVENT_RADIO_AVAILABLE:
                    onRetryWhenRadioAvailable(msg);
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * Handle START response
     * @param msg obj field isa RadioCapability
     */
    private void onStartRadioCapabilityResponse(Message msg) {
        synchronized (mSetRadioAccessFamilyStatus) {
            AsyncResult ar = (AsyncResult)msg.obj;
            if (ar.exception != null) {
                //Add by MTK,@{
                if (onExceptionCount == 0) {
                    CommandException.Error err = null;
                    // counter is to avoid multiple error handle.
                    onExceptionCount = 1;
                    if (ar.exception instanceof CommandException) {
                        err = ((CommandException) (ar.exception)).getCommandError();
                    }

                    if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                        // Radio has crashed or turned off
                        mSetRafRetryCause = RC_RETRY_CAUSE_RADIO_UNAVAILABLE;
                        // check radio available
                        for (int i = 0; i < mPhones.length; i++) {
                            mCi[i].registerForAvailable(mHandler, EVENT_RADIO_AVAILABLE, null);
                        }
                        loge("onStartRadioCapabilityResponse: Retry later due to modem off");
                    }
                }
                //}@

                // just abort now.  They didn't take our start so we don't have to revert
                logd("onStartRadioCapabilityResponse got exception=" + ar.exception);
                mRadioCapabilitySessionId = mUniqueIdGenerator.getAndIncrement();
                Intent intent = new Intent(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
                mContext.sendBroadcast(intent);
                clearTransaction();
                return;
            }
            RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
            if ((rc == null) || (rc.getSession() != mRadioCapabilitySessionId)) {
                logd("onStartRadioCapabilityResponse: Ignore session=" + mRadioCapabilitySessionId
                        + " rc=" + rc);
                return;
            }
            mRadioAccessFamilyStatusCounter--;
            int id = rc.getPhoneId();
            if (((AsyncResult) msg.obj).exception != null) {
                logd("onStartRadioCapabilityResponse: Error response session=" + rc.getSession());
                logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                mSetRadioAccessFamilyStatus[id] = SET_RC_STATUS_FAIL;
                mTransactionFailed = true;
            } else {
                logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=STARTED");
                mSetRadioAccessFamilyStatus[id] = SET_RC_STATUS_STARTED;
            }

            if (mRadioAccessFamilyStatusCounter == 0) {
                //Add by MTK,@{
                /* remove Google's code because it causes capability switch fail in 3SIM project.
                 * mNewLogicalModemIds get same modem id in two 2G logical modem then cause WTF.
                 */
                /*
                HashSet<String> modemsInUse = new HashSet<String>(mNewLogicalModemIds.length);
                for (String modemId : mNewLogicalModemIds) {
                    if (!modemsInUse.equals("") && !modemsInUse.add(modemId)) {
                        mTransactionFailed = true;
                        Log.wtf(LOG_TAG, "ERROR: sending down the same id for different phones");
                    }
                }
                */
                //}@
                logd("onStartRadioCapabilityResponse: success=" + !mTransactionFailed);
                if (mTransactionFailed) {
                    // Sends a variable number of requests, so don't resetRadioAccessFamilyCounter
                    // here.
                    issueFinish(mRadioCapabilitySessionId);
                } else {
                    // All logical modem accepted the new radio access family, issue the APPLY
                    resetRadioAccessFamilyStatusCounter();
                    for (int i = 0; i < mPhones.length; i++) {
                        sendRadioCapabilityRequest(
                            i,
                            mRadioCapabilitySessionId,
                            RadioCapability.RC_PHASE_APPLY,
                            mNewRadioAccessFamily[i],
                            mNewLogicalModemIds[i],
                            RadioCapability.RC_STATUS_NONE,
                            EVENT_APPLY_RC_RESPONSE);

                        logd("onStartRadioCapabilityResponse: phoneId=" + i + " status=APPLYING");
                        mSetRadioAccessFamilyStatus[i] = SET_RC_STATUS_APPLYING;
                    }
                }
            }
        }
    }

    /**
     * Handle APPLY response
     * @param msg obj field isa RadioCapability
     */
    private void onApplyRadioCapabilityResponse(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        AsyncResult ar = (AsyncResult) msg.obj;
        CommandException.Error err = null;
        if ((rc == null) || (rc.getSession() != mRadioCapabilitySessionId)) {
            //Add by MTK,@{
            if ((rc == null) && (ar.exception != null) && (onExceptionCount == 0)) {
                // counter is to avoid multiple error handle.
                onExceptionCount = 1;
                if (ar.exception instanceof CommandException) {
                    err = ((CommandException) (ar.exception)).getCommandError();
                }

                if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                    // Radio has crashed or turned off
                    mSetRafRetryCause = RC_RETRY_CAUSE_RADIO_UNAVAILABLE;
                    // check radio available
                    for (int i = 0; i < mPhones.length; i++) {
                        mCi[i].registerForAvailable(mHandler, EVENT_RADIO_AVAILABLE, null);
                    }
                    loge("onApplyRadioCapabilityResponse: Retry later due to RADIO_NOT_AVAILABLE");
                } else {
                    loge("onApplyRadioCapabilityResponse: exception=" +
                            ar.exception);
                }
                mRadioCapabilitySessionId = mUniqueIdGenerator.getAndIncrement();
                Intent intent = new Intent(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
                mContext.sendBroadcast(intent);
                clearTransaction();
                //}@
            } else {
                logd("onApplyRadioCapabilityResponse: Ignore session=" + mRadioCapabilitySessionId
                        + " rc=" + rc);
            }
            return;
        }
        logd("onApplyRadioCapabilityResponse: rc=" + rc);
        if (((AsyncResult) msg.obj).exception != null) {
            synchronized (mSetRadioAccessFamilyStatus) {
                logd("onApplyRadioCapabilityResponse: Error response session=" + rc.getSession());
                int id = rc.getPhoneId();

                //Add by MTK,@{
                if (ar.exception instanceof CommandException) {
                    err = ((CommandException) (ar.exception)).getCommandError();
                }

                if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                    // Radio has crashed or turned off
                    mSetRafRetryCause = RC_RETRY_CAUSE_RADIO_UNAVAILABLE;
                    // check radio available
                    mCi[id].registerForAvailable(mHandler, EVENT_RADIO_AVAILABLE, null);
                    loge("onApplyRadioCapabilityResponse: Retry later due to modem off");
                } else {
                    loge("onApplyRadioCapabilityResponse: exception=" +
                            ar.exception);
                }
                //}@

                logd("onApplyRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                mSetRadioAccessFamilyStatus[id] = SET_RC_STATUS_FAIL;
                mTransactionFailed = true;
            }
        } else {
            logd("onApplyRadioCapabilityResponse: Valid start expecting notification rc=" + rc);
        }
    }

    /**
     * Handle the notification unsolicited response associated with the APPLY
     * @param msg obj field isa RadioCapability
     */
    private void onNotificationRadioCapabilityChanged(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        if ((rc == null) || (rc.getSession() != mRadioCapabilitySessionId)) {
            logd("onNotificationRadioCapabilityChanged: Ignore session=" + mRadioCapabilitySessionId
                    + " rc=" + rc);
            return;
        }
        // if the radio change is not triggered by sim switch, the notification should be ignore.
        if (false == mIsCapSwitching) {
            logd("radio change is not triggered by sim switch, notification should be ignore");
            clearTransaction();
            return;
        }
        synchronized (mSetRadioAccessFamilyStatus) {
            logd("onNotificationRadioCapabilityChanged: rc=" + rc);
            // skip the overdue response by checking sessionId
            if (rc.getSession() != mRadioCapabilitySessionId) {
                logd("onNotificationRadioCapabilityChanged: Ignore session="
                        + mRadioCapabilitySessionId + " rc=" + rc);
                return;
            }

            int id = rc.getPhoneId();
            if ((((AsyncResult) msg.obj).exception != null) ||
                    (rc.getStatus() == RadioCapability.RC_STATUS_FAIL)) {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=FAIL");
                mSetRadioAccessFamilyStatus[id] = SET_RC_STATUS_FAIL;
                mTransactionFailed = true;
            } else {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=SUCCESS");
                mSetRadioAccessFamilyStatus[id] = SET_RC_STATUS_SUCCESS;
                // The modems may have been restarted and forgotten this
                mPhoneSwitcher.resendDataAllowed(id);
                mPhones[id].radioCapabilityUpdated(rc);
            }

            mRadioAccessFamilyStatusCounter--;
            if (mRadioAccessFamilyStatusCounter == 0) {
                logd("onNotificationRadioCapabilityChanged: APPLY URC success=" +
                        mTransactionFailed);
                issueFinish(mRadioCapabilitySessionId);
            }
        }
    }

    /**
     * Handle the FINISH Phase response
     * @param msg obj field isa RadioCapability
     */
    void onFinishRadioCapabilityResponse(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        if ((rc == null) || (rc.getSession() != mRadioCapabilitySessionId)) {
            //M:Add by MTK for C2k project,@{
            //When capability switch on Finish phase,socket may disconnected by other module ,
            //like airplan mode ,in this case rc is null,it will return and can not
            //finish at all.
            if (SystemProperties.get(MTK_C2K_SUPPORT).equals("1")) {
                if ((rc == null) && (((AsyncResult) msg.obj).exception != null)) {
                    synchronized (mSetRadioAccessFamilyStatus) {
                        logd("onFinishRadioCapabilityResponse C2K mRadioAccessFamilyStatusCounter="
                                + mRadioAccessFamilyStatusCounter);
                        mRadioAccessFamilyStatusCounter--;
                        if (mRadioAccessFamilyStatusCounter == 0) {
                            completeRadioCapabilityTransaction();
                        }
                    }
                    return;
                }
            }
            //}@
            logd("onFinishRadioCapabilityResponse: Ignore session=" + mRadioCapabilitySessionId
                    + " rc=" + rc);
            return;
        }
        synchronized (mSetRadioAccessFamilyStatus) {
            logd(" onFinishRadioCapabilityResponse mRadioAccessFamilyStatusCounter="
                    + mRadioAccessFamilyStatusCounter);
            mRadioAccessFamilyStatusCounter--;
            if (mRadioAccessFamilyStatusCounter == 0) {
                completeRadioCapabilityTransaction();
            }
        }
    }

    private void onTimeoutRadioCapability(Message msg) {
        if (msg.arg1 != mRadioCapabilitySessionId) {
           logd("RadioCapability timeout: Ignore msg.arg1=" + msg.arg1 +
                   "!= mRadioCapabilitySessionId=" + mRadioCapabilitySessionId);
            return;
        }

        synchronized(mSetRadioAccessFamilyStatus) {
            // timed-out.  Clean up as best we can
            for (int i = 0; i < mPhones.length; i++) {
                logd("RadioCapability timeout: mSetRadioAccessFamilyStatus[" + i + "]=" +
                        mSetRadioAccessFamilyStatus[i]);
            }

            // Increment the sessionId as we are completing the transaction below
            // so we don't want it completed when the FINISH phase is done.
            int uniqueDifferentId = mUniqueIdGenerator.getAndIncrement();
            // send FINISH request with fail status and then uniqueDifferentId
            mTransactionFailed = true;
            issueFinish(uniqueDifferentId);
        }
    }

    private void issueFinish(int sessionId) {
        // Issue FINISH
        synchronized(mSetRadioAccessFamilyStatus) {
            //Add by MTK,@{
            // Reset counter directly instead of AOSP accumulate, to fix apply stage fail case
            resetRadioAccessFamilyStatusCounter();
            //}@
            for (int i = 0; i < mPhones.length; i++) {
                logd("issueFinish: phoneId=" + i + " sessionId=" + sessionId
                        + " mTransactionFailed=" + mTransactionFailed);
                //Add by MTK,@{
                // remove the counter++, because the counter has already reset
                //mRadioAccessFamilyStatusCounter++;
                //}@
                sendRadioCapabilityRequest(
                        i,
                        sessionId,
                        RadioCapability.RC_PHASE_FINISH,
                        mOldRadioAccessFamily[i],
                        mCurrentLogicalModemIds[i],
                        (mTransactionFailed ? RadioCapability.RC_STATUS_FAIL :
                        RadioCapability.RC_STATUS_SUCCESS),
                        EVENT_FINISH_RC_RESPONSE);
                if (mTransactionFailed) {
                    logd("issueFinish: phoneId: " + i + " status: FAIL");
                    // At least one failed, mark them all failed.
                    mSetRadioAccessFamilyStatus[i] = SET_RC_STATUS_FAIL;
                }
            }
        }
    }

    private void completeRadioCapabilityTransaction() {
        // Create the intent to broadcast
        Intent intent;
        logd("onFinishRadioCapabilityResponse: success=" + !mTransactionFailed);
        if (!mTransactionFailed) {
            ArrayList<RadioAccessFamily> phoneRAFList = new ArrayList<RadioAccessFamily>();
            for (int i = 0; i < mPhones.length; i++) {
                int raf = mPhones[i].getRadioAccessFamily();
                logd("radioAccessFamily[" + i + "]=" + raf);
                RadioAccessFamily phoneRC = new RadioAccessFamily(i, raf);
                phoneRAFList.add(phoneRC);
            }
            intent = new Intent(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
            intent.putParcelableArrayListExtra(TelephonyIntents.EXTRA_RADIO_ACCESS_FAMILY,
                    phoneRAFList);

            // make messages about the old transaction obsolete (specifically the timeout)
            mRadioCapabilitySessionId = mUniqueIdGenerator.getAndIncrement();

            // Reinitialize
            clearTransaction();
        } else {
            intent = new Intent(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);

            // now revert.
            mTransactionFailed = false;

            //Add by MTK,@{
            if (retryToSetRadioCapabilityIfTimeout() ==  true) {
                mSetRafRetryCause = RC_RETRY_CAUSE_CAPABILITY_SWITCHING;
            } else {
                // ASOP revert is not acceptable by user, so clear transaction and retry later.
                clearTransaction();
            }
            /*
            RadioAccessFamily[] rafs = new RadioAccessFamily[mPhones.length];
            for (int phoneId = 0; phoneId < mPhones.length; phoneId++) {
                rafs[phoneId] = new RadioAccessFamily(phoneId, mOldRadioAccessFamily[phoneId]);
            }
            doSetRadioCapabilities(rafs);
            */
            //}@
        }

        //Add by MTK,@{
        RadioCapabilitySwitchUtil.updateIccid(mPhones);
        //}@

        // Broadcast that we're done
        mContext.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);

        //Add by MTK,if any request pending,we will trigger it ,@M{
        if ((mNextRafs != null) &&
                (mSetRafRetryCause == RC_RETRY_CAUSE_CAPABILITY_SWITCHING)) {
            logd("has next capability switch request,trigger it");
            try {
                if (!setRadioCapability(mNextRafs)) {
                    sendCapabilityFailBroadcast();
                } else {
                    mSetRafRetryCause = RC_RETRY_CAUSE_NONE;
                    mNextRafs = null;
                }
            } catch (java.lang.RuntimeException e) {
                sendCapabilityFailBroadcast();
            }
        }
        //}@
    }

    // Clear this transaction
    private void clearTransaction() {
        logd("clearTransaction mIsCapSwitching =" + mIsCapSwitching);

        //Add by MTK,@{
        if (mIsCapSwitching) {
            mHandler.removeMessages(EVENT_TIMEOUT);
            //logd("clearTransaction: remove timer");
        }
        mIsCapSwitching = false;
        //}@

        synchronized(mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < mPhones.length; i++) {
                //logd("clearTransaction: phoneId=" + i + " status=IDLE");
                mSetRadioAccessFamilyStatus[i] = SET_RC_STATUS_IDLE;
                mOldRadioAccessFamily[i] = 0;
                mNewRadioAccessFamily[i] = 0;
                mTransactionFailed = false;
            }

            logd("clearTransaction: All phones status=IDLE");

            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private void resetRadioAccessFamilyStatusCounter() {
        mRadioAccessFamilyStatusCounter = mPhones.length;
    }

    private void sendRadioCapabilityRequest(int phoneId, int sessionId, int rcPhase,
            int radioFamily, String logicalModemId, int status, int eventId) {
        //Add by MTK, TBD, work around for lost ModemID,@{
        if (logicalModemId == null || logicalModemId.equals("")) {
            logicalModemId = "modem_sys3";
        }
        //}

        RadioCapability requestRC = new RadioCapability(
                phoneId, sessionId, rcPhase, radioFamily, logicalModemId, status);
        mPhones[phoneId].setRadioCapability(
                requestRC, mHandler.obtainMessage(eventId));
    }

    // This method will return max number of raf bits supported from the raf
    // values currently stored in all phone objects
    public int getMaxRafSupported() {
        int[] numRafSupported = new int[mPhones.length];
        int maxNumRafBit = 0;
        int maxRaf = RadioAccessFamily.RAF_UNKNOWN;

        for (int len = 0; len < mPhones.length; len++) {
            numRafSupported[len] = Integer.bitCount(mPhones[len].getRadioAccessFamily());
            if (maxNumRafBit < numRafSupported[len]) {
                maxNumRafBit = numRafSupported[len];
                maxRaf = mPhones[len].getRadioAccessFamily();
            }
        }

        return maxRaf;
    }

    // This method will return minimum number of raf bits supported from the raf
    // values currently stored in all phone objects
    public int getMinRafSupported() {
        int[] numRafSupported = new int[mPhones.length];
        int minNumRafBit = 0;
        int minRaf = RadioAccessFamily.RAF_UNKNOWN;

        for (int len = 0; len < mPhones.length; len++) {
            numRafSupported[len] = Integer.bitCount(mPhones[len].getRadioAccessFamily());
            if ((minNumRafBit == 0) || (minNumRafBit > numRafSupported[len])) {
                minNumRafBit = numRafSupported[len];
                minRaf = mPhones[len].getRadioAccessFamily();
            }
        }
        return minRaf;
    }

    // This method checks current raf values stored in all phones and
    // whicheve phone raf matches with input raf, returns modemId from that phone
    private String getLogicalModemIdFromRaf(int raf) {
        String modemUuid = null;

        for (int phoneId = 0; phoneId < mPhones.length; phoneId++) {
            if (mPhones[phoneId].getRadioAccessFamily() == raf) {
                modemUuid = mPhones[phoneId].getModemUuId();
                break;
            }
        }
        return modemUuid;
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }

    //Add by MTK,@{
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        try {
            mPhoneSwitcher.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if under capability switching.
     *
     * @return true if switching
     */
    public boolean isCapabilitySwitching() {
        return mIsCapSwitching;
    }

    private boolean retryToSetRadioCapabilityIfTimeout() {
        int iRet = SystemProperties.getInt("ril.switch.result", 0);
        SystemProperties.set("ril.switch.result", "0");
        logd("retryToSetRadioCapabilityIfTimeout ret = " + iRet);
        if (iRet == 1) {
            return true;
        }
        return false;
    }

    private void onRetryWhenRadioAvailable(Message msg) {
        logd("onRetryWhenRadioAvailable,mSetRafRetryCause:" + mSetRafRetryCause);
        for (int i = 0; i < mPhones.length; i++) {
            if (RadioManager.isModemPowerOff(i)) {
                logd("onRetryWhenRadioAvailable, Phone" + i + " modem off");
                return;
            }
        }
        if ((mNextRafs != null) && (mSetRafRetryCause == RC_RETRY_CAUSE_RADIO_UNAVAILABLE)) {
            try {
                setRadioCapability(mNextRafs);
            } catch (java.lang.RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private BroadcastReceiver mWorldModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int wmState = WorldMode.MD_WM_CHANGED_UNKNOWN;
            logd("mWorldModeReceiver: action = " + action);
            if (TelephonyIntents.ACTION_WORLD_MODE_CHANGED.equals(action)) {
                wmState = intent.getIntExtra(TelephonyIntents.EXTRA_WORLD_MODE_CHANGE_STATE,
                        WorldMode.MD_WM_CHANGED_UNKNOWN);
                logd("wmState: " + wmState);
                if (wmState == WorldMode.MD_WM_CHANGED_END) {
                    if ((mNextRafs != null) &&
                            (mSetRafRetryCause == RC_RETRY_CAUSE_WORLD_MODE_SWITCHING)) {
                        try {
                            if (!setRadioCapability(mNextRafs)) {
                                sendCapabilityFailBroadcast();
                            }
                        } catch (java.lang.RuntimeException e) {
                            sendCapabilityFailBroadcast();
                        }
                    }
                }
            }
        }
    };

    private BroadcastReceiver mPhoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String phoneState = TelephonyManager.EXTRA_STATE_OFFHOOK;
            logd("mPhoneStateReceiver: action = " + action);
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                logd("phoneState: " + phoneState);
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(phoneState)) {
                    if ((mNextRafs != null) &&
                            (mSetRafRetryCause == RC_RETRY_CAUSE_IN_CALL)) {
                        try {
                            if (!setRadioCapability(mNextRafs)) {
                                sendCapabilityFailBroadcast();
                            }
                        } catch (java.lang.RuntimeException e) {
                            sendCapabilityFailBroadcast();
                        }
                    }
                }
            }
        }
    };

    private void sendCapabilityFailBroadcast() {
        if (mContext != null) {
            Intent intent = new Intent(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
            mContext.sendBroadcast(intent);
        }
    }

    private void registerWorldModeReceiver() {
        if (mContext == null) {
            logd("registerWorldModeReceiver, context is null => return");
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_WORLD_MODE_CHANGED);
        mContext.registerReceiver(mWorldModeReceiver, filter);
        mHasRegisterWorldModeReceiver = true;
    }

    private void unRegisterWorldModeReceiver() {
        if (mContext == null) {
            logd("unRegisterWorldModeReceiver, context is null => return");
            return;
        }

        mContext.unregisterReceiver(mWorldModeReceiver);
        mHasRegisterWorldModeReceiver = false;
    }

    private void registerPhoneStateReceiver() {
        if (mContext == null) {
            logd("registerPhoneStateReceiver, context is null => return");
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mContext.registerReceiver(mPhoneStateReceiver, filter);
        mHasRegisterPhoneStateReceiver = true;
    }

    private void unRegisterPhoneStateReceiver() {
        if (mContext == null) {
            logd("unRegisterPhoneStateReceiver, context is null => return");
            return;
        }

        mContext.unregisterReceiver(mPhoneStateReceiver);
        mHasRegisterPhoneStateReceiver = false;
    }

    private BroadcastReceiver mEccStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logd("mEccStateReceiver, received " + intent.getAction());
            if (!isEccInProgress()) {
                if ((mNextRafs != null) && (mSetRafRetryCause == RC_RETRY_CAUSE_IN_CALL)) {
                    try {
                        if (!setRadioCapability(mNextRafs)) {
                            sendCapabilityFailBroadcast();
                        }
                    } catch (RuntimeException e) {
                        sendCapabilityFailBroadcast();
                    }
                }
            }
        }
    };

    private void registerEccStateReceiver() {
        if (mContext == null) {
            logd("registerEccStateReceiver, context is null => return");
            return;
        }
        IntentFilter filter = new IntentFilter("android.intent.action.ECC_IN_PROGRESS");
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        mContext.registerReceiver(mEccStateReceiver, filter);
        mHasRegisterEccStateReceiver = true;
    }

    private void unRegisterEccStateReceiver() {
        if (mContext == null) {
            logd("unRegisterEccStateReceiver, context is null => return");
            return;
        }
        mContext.unregisterReceiver(mEccStateReceiver);
        mHasRegisterEccStateReceiver = false;
    }

    private boolean isEccInProgress() {
        String value = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE, "");
        boolean inEcm = value.contains("true");
        boolean isInEcc = false;
        ITelephonyEx telEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        if (telEx != null) {
            try {
                isInEcc = telEx.isEccInProgress();
            } catch (RemoteException e) {
                loge("Exception of isEccInProgress");
            }
        }
        logd("isEccInProgress, value:" + value + ", inEcm:" + inEcm + ", isInEcc:" + isInEcc);
        return inEcm || isInEcc;
    }
    //}@
}
