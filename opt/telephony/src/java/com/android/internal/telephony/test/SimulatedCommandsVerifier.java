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

package com.android.internal.telephony.test;

import android.os.Handler;
import android.os.Message;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.dataconnection.DataProfile;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;

// MTK-START, SMS part
import android.telephony.SmsParameters;
// MTK-END
import com.mediatek.common.telephony.gsm.PBEntry;
import com.mediatek.internal.telephony.uicc.PhbEntry;

import com.mediatek.internal.telephony.FemtoCellInfo;

public class SimulatedCommandsVerifier implements CommandsInterface {
    private static SimulatedCommandsVerifier sInstance;

    private SimulatedCommandsVerifier() {

    }

    public static SimulatedCommandsVerifier getInstance() {
        if (sInstance == null) {
            sInstance = new SimulatedCommandsVerifier();
        }
        return sInstance;
    }

    @Override
    public RadioState getRadioState() {
        return null;
    }

    @Override
    public void getImsRegistrationState(Message result) {

    }

    @Override
    public void registerForRadioStateChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForRadioStateChanged(Handler h) {

    }

    @Override
    public void registerForVoiceRadioTechChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForVoiceRadioTechChanged(Handler h) {

    }

    @Override
    public void registerForImsNetworkStateChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForImsNetworkStateChanged(Handler h) {

    }

    @Override
    public void registerForOn(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForOn(Handler h) {

    }

    @Override
    public void registerForAvailable(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForAvailable(Handler h) {

    }

    @Override
    public void registerForNotAvailable(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForNotAvailable(Handler h) {

    }

    @Override
    public void registerForOffOrNotAvailable(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForOffOrNotAvailable(Handler h) {

    }

    @Override
    public void registerForIccStatusChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForIccStatusChanged(Handler h) {

    }

    @Override
    public void registerForCallStateChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForCallStateChanged(Handler h) {

    }

    @Override
    public void registerForVoiceNetworkStateChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForVoiceNetworkStateChanged(Handler h) {

    }

    @Override
    public void registerForDataNetworkStateChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForDataNetworkStateChanged(Handler h) {

    }

    @Override
    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForInCallVoicePrivacyOn(Handler h) {

    }

    @Override
    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForInCallVoicePrivacyOff(Handler h) {

    }

    @Override
    public void registerForSrvccStateChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForSrvccStateChanged(Handler h) {

    }

    @Override
    public void registerForSubscriptionStatusChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForSubscriptionStatusChanged(Handler h) {

    }

    @Override
    public void registerForHardwareConfigChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForHardwareConfigChanged(Handler h) {

    }

    @Override
    public void setOnNewGsmSms(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnNewGsmSms(Handler h) {

    }

    @Override
    public void setOnNewCdmaSms(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnNewCdmaSms(Handler h) {

    }

    @Override
    public void setOnNewGsmBroadcastSms(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnNewGsmBroadcastSms(Handler h) {

    }

    @Override
    public void setOnSmsOnSim(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnSmsOnSim(Handler h) {

    }

    @Override
    public void setOnSmsStatus(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnSmsStatus(Handler h) {

    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnNITZTime(Handler h) {

    }

    @Override
    public void setOnUSSD(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnUSSD(Handler h) {

    }

    @Override
    public void setOnSignalStrengthUpdate(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnSignalStrengthUpdate(Handler h) {

    }

    @Override
    public void setOnIccSmsFull(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnIccSmsFull(Handler h) {

    }

    @Override
    public void registerForIccRefresh(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForIccRefresh(Handler h) {

    }

    @Override
    public void setOnIccRefresh(Handler h, int what, Object obj) {

    }

    @Override
    public void unsetOnIccRefresh(Handler h) {

    }

    @Override
    public void setOnCallRing(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnCallRing(Handler h) {

    }

    @Override
    public void setOnRestrictedStateChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnRestrictedStateChanged(Handler h) {

    }

    @Override
    public void setOnSuppServiceNotification(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnSuppServiceNotification(Handler h) {

    }

    @Override
    public void setOnCatSessionEnd(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnCatSessionEnd(Handler h) {

    }

    @Override
    public void setOnCatProactiveCmd(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnCatProactiveCmd(Handler h) {

    }

    @Override
    public void setOnCatEvent(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnCatEvent(Handler h) {

    }

    @Override
    public void setOnCatCallSetUp(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnCatCallSetUp(Handler h) {

    }

    @Override
    public void setSuppServiceNotifications(boolean enable, Message result) {

    }

    @Override
    public void setOnCatCcAlphaNotify(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnCatCcAlphaNotify(Handler h) {

    }

    @Override
    public void setOnSs(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnSs(Handler h) {

    }

    @Override
    public void registerForDisplayInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForDisplayInfo(Handler h) {

    }

    @Override
    public void registerForCallWaitingInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForCallWaitingInfo(Handler h) {

    }

    @Override
    public void registerForSignalInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForSignalInfo(Handler h) {

    }

    @Override
    public void registerForNumberInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForNumberInfo(Handler h) {

    }

    @Override
    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForRedirectedNumberInfo(Handler h) {

    }

    @Override
    public void registerForLineControlInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForLineControlInfo(Handler h) {

    }

    @Override
    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForT53ClirInfo(Handler h) {

    }

    @Override
    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForT53AudioControlInfo(Handler h) {

    }

    @Override
    public void setEmergencyCallbackMode(Handler h, int what, Object obj) {

    }

    @Override
    public void registerForCdmaOtaProvision(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForCdmaOtaProvision(Handler h) {

    }

    @Override
    public void registerForRingbackTone(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForRingbackTone(Handler h) {

    }

    @Override
    public void registerForResendIncallMute(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForResendIncallMute(Handler h) {

    }

    @Override
    public void registerForCdmaSubscriptionChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForCdmaSubscriptionChanged(Handler h) {

    }

    @Override
    public void registerForCdmaPrlChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForCdmaPrlChanged(Handler h) {

    }

    @Override
    public void registerForExitEmergencyCallbackMode(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForExitEmergencyCallbackMode(Handler h) {

    }

    @Override
    public void registerForRilConnected(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForRilConnected(Handler h) {

    }

    @Override
    public void supplyIccPin(String pin, Message result) {

    }

    @Override
    public void supplyIccPinForApp(String pin, String aid, Message result) {

    }

    @Override
    public void supplyIccPuk(String puk, String newPin, Message result) {

    }

    @Override
    public void supplyIccPukForApp(String puk, String newPin, String aid, Message result) {

    }

    @Override
    public void supplyIccPin2(String pin2, Message result) {

    }

    @Override
    public void supplyIccPin2ForApp(String pin2, String aid, Message result) {

    }

    @Override
    public void supplyIccPuk2(String puk2, String newPin2, Message result) {

    }

    @Override
    public void supplyIccPuk2ForApp(String puk2, String newPin2, String aid, Message result) {

    }

    @Override
    public void changeIccPin(String oldPin, String newPin, Message result) {

    }

    @Override
    public void changeIccPinForApp(String oldPin, String newPin, String aidPtr, Message result) {

    }

    @Override
    public void changeIccPin2(String oldPin2, String newPin2, Message result) {

    }

    @Override
    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aidPtr, Message result) {

    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd,
                                      Message result) {

    }

    @Override
    public void supplyNetworkDepersonalization(String netpin, Message result) {

    }

    @Override
    public void getCurrentCalls(Message result) {

    }

    @Override
    public void getPDPContextList(Message result) {

    }

    @Override
    public void getDataCallList(Message result) {

    }

    @Override
    public void dial(String address, int clirMode, Message result) {

    }

    @Override
    public void dial(String address, int clirMode, UUSInfo uusInfo, Message result) {

    }

    @Override
    public void getIMSI(Message result) {

    }

    @Override
    public void getIMSIForApp(String aid, Message result) {

    }

    @Override
    public void getIMEI(Message result) {

    }

    @Override
    public void getIMEISV(Message result) {

    }

    @Override
    public void hangupConnection(int gsmIndex, Message result) {

    }

    @Override
    public void hangupWaitingOrBackground(Message result) {

    }

    @Override
    public void hangupForegroundResumeBackground(Message result) {

    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {

    }

    @Override
    public void conference(Message result) {

    }

    @Override
    public void setPreferredVoicePrivacy(boolean enable, Message result) {

    }

    @Override
    public void getPreferredVoicePrivacy(Message result) {

    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {

    }

    @Override
    public void acceptCall(Message result) {

    }

    @Override
    public void rejectCall(Message result) {

    }

    @Override
    public void explicitCallTransfer(Message result) {

    }

    @Override
    public void getLastCallFailCause(Message result) {

    }

    @Override
    public void getLastPdpFailCause(Message result) {

    }

    @Override
    public void getLastDataCallFailCause(Message result) {

    }

    @Override
    public void setMute(boolean enableMute, Message response) {

    }

    @Override
    public void getMute(Message response) {

    }

    @Override
    public void getSignalStrength(Message response) {

    }

    @Override
    public void getVoiceRegistrationState(Message response) {

    }

    @Override
    public void getDataRegistrationState(Message response) {

    }

    @Override
    public void getOperator(Message response) {

    }

    @Override
    public void sendDtmf(char c, Message result) {

    }

    @Override
    public void startDtmf(char c, Message result) {

    }

    @Override
    public void stopDtmf(Message result) {

    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {

    }

    @Override
    public void sendSMS(String smscPDU, String pdu, Message response) {

    }

    @Override
    public void sendSMSExpectMore(String smscPDU, String pdu, Message response) {

    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message response) {

    }

    @Override
    public void sendImsGsmSms(String smscPDU, String pdu, int retry, int messageRef,
                              Message response) {

    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message response) {

    }

    @Override
    public void deleteSmsOnSim(int index, Message response) {

    }

    @Override
    public void deleteSmsOnRuim(int index, Message response) {

    }

    @Override
    public void writeSmsToSim(int status, String smsc, String pdu, Message response) {

    }

    @Override
    public void writeSmsToRuim(int status, String pdu, Message response) {

    }

    @Override
    public void setRadioPower(boolean on, Message response) {

    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message response) {

    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message response) {

    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message response) {

    }

    @Override
    public void iccIO(int command, int fileid, String path, int p1, int p2, int p3, String data,
                      String pin2, Message response) {

    }

    @Override
    public void iccIOForApp(int command, int fileid, String path, int p1, int p2, int p3,
                            String data, String pin2, String aid, Message response) {

    }

    @Override
    public void queryCLIP(Message response) {

    }

    @Override
    public void getCLIR(Message response) {

    }

    @Override
    public void setCLIR(int clirMode, Message response) {

    }

    @Override
    public void queryCallWaiting(int serviceClass, Message response) {

    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message response) {

    }

    @Override
    public void setCallForward(int action, int cfReason, int serviceClass, String number,
                               int timeSeconds, Message response) {

    }

    @Override
    public void queryCallForwardStatus(int cfReason, int serviceClass, String number,
                                       Message response) {

    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message response) {

    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, Message response) {

    }

    @Override
    public void getNetworkSelectionMode(Message response) {

    }

    @Override
    public void getAvailableNetworks(Message response) {

    }

    @Override
    public void getBasebandVersion(Message response) {

    }

    @Override
    public void queryFacilityLock(String facility, String password, int serviceClass,
                                  Message response) {

    }

    @Override
    public void queryFacilityLockForApp(String facility, String password, int serviceClass,
                                        String appId, Message response) {

    }

    @Override
    public void setFacilityLock(String facility, boolean lockState, String password,
                                int serviceClass, Message response) {

    }

    @Override
    public void setFacilityLockForApp(String facility, boolean lockState, String password,
                                      int serviceClass, String appId, Message response) {

    }

    @Override
    public void sendUSSD(String ussdString, Message response) {

    }

    @Override
    public void cancelPendingUssd(Message response) {

    }

    @Override
    public void resetRadio(Message result) {

    }

    @Override
    public void setBandMode(int bandMode, Message response) {

    }

    @Override
    public void queryAvailableBandMode(Message response) {

    }

    @Override
    public void setPreferredNetworkType(int networkType, Message response) {

    }

    @Override
    public void getPreferredNetworkType(Message response) {

    }

    @Override
    public void getNeighboringCids(Message response) {

    }

    @Override
    public void setLocationUpdates(boolean enable, Message response) {

    }

    @Override
    public void getSmscAddress(Message result) {

    }

    @Override
    public void setSmscAddress(String address, Message result) {

    }

    @Override
    public void reportSmsMemoryStatus(boolean available, Message result) {

    }

    @Override
    public void reportStkServiceIsRunning(Message result) {

    }

    @Override
    public void invokeOemRilRequestRaw(byte[] data, Message response) {

    }

    @Override
    public void invokeOemRilRequestStrings(String[] strings, Message response) {

    }

    @Override
    public void setOnUnsolOemHookRaw(Handler h, int what, Object obj) {

    }

    @Override
    public void unSetOnUnsolOemHookRaw(Handler h) {

    }

    @Override
    public void sendTerminalResponse(String contents, Message response) {

    }

    @Override
    public void sendEnvelope(String contents, Message response) {

    }

    @Override
    public void sendEnvelopeWithStatus(String contents, Message response) {

    }

    @Override
    public void handleCallSetupRequestFromSim(boolean accept, int resCode, Message response) {

    }

    @Override
    public void setGsmBroadcastActivation(boolean activate, Message result) {

    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message response) {

    }

    @Override
    public void getGsmBroadcastConfig(Message response) {

    }

    @Override
    public void getDeviceIdentity(Message response) {

    }

    @Override
    public void getCDMASubscription(Message response) {

    }

    @Override
    public void sendCDMAFeatureCode(String FeatureCode, Message response) {

    }

    @Override
    public void setPhoneType(int phoneType) {

    }

    @Override
    public void queryCdmaRoamingPreference(Message response) {

    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {

    }

    @Override
    public void setCdmaSubscriptionSource(int cdmaSubscriptionType, Message response) {

    }

    @Override
    public void getCdmaSubscriptionSource(Message response) {

    }

    @Override
    public void setTTYMode(int ttyMode, Message response) {

    }

    @Override
    public void queryTTYMode(Message response) {

    }

    @Override
    public void setupDataCall(int radioTechnology, int profile, String apn, String user,
                              String password, int authType, String protocol, Message result) {

    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {

    }

    @Override
    public void setCdmaBroadcastActivation(boolean activate, Message result) {

    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message response) {

    }

    @Override
    public void getCdmaBroadcastConfig(Message result) {

    }

    @Override
    public void exitEmergencyCallbackMode(Message response) {

    }

    @Override
    public void getIccCardStatus(Message result) {

    }

    @Override
    public int getLteOnCdmaMode() {
        return 0;
    }

    @Override
    public void requestIsimAuthentication(String nonce, Message response) {

    }

    @Override
    public void requestIccSimAuthentication(int authContext, String data, String aid,
                                            Message response) {

    }

    @Override
    public void getVoiceRadioTechnology(Message result) {

    }

    @Override
    public void getCellInfoList(Message result) {

    }

    @Override
    public void setCellInfoListRate(int rateInMillis, Message response) {

    }

    @Override
    public void registerForCellInfoList(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForCellInfoList(Handler h) {

    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username,
                                    String password, Message result) {

    }

    @Override
    public void setDataProfile(DataProfile[] dps, Message result) {

    }

    @Override
    public void testingEmergencyCall() {

    }

    @Override
    public void iccOpenLogicalChannel(String AID, Message response) {

    }

    @Override
    public void iccCloseLogicalChannel(int channel, Message response) {

    }

    @Override
    public void iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1,
                                              int p2, int p3, String data, Message response) {

    }

    @Override
    public void iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3,
                                            String data, Message response) {

    }

    @Override
    public void nvReadItem(int itemID, Message response) {

    }

    @Override
    public void nvWriteItem(int itemID, String itemValue, Message response) {

    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {

    }

    @Override
    public void nvResetConfig(int resetType, Message response) {

    }

    @Override
    public void getHardwareConfig(Message result) {

    }

    @Override
    public int getRilVersion() {
        return 0;
    }

    @Override
    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus,
                                    Message result) {

    }

    @Override
    public void setDataAllowed(boolean allowed, Message result) {

    }

    @Override
    public void requestShutdown(Message result) {

    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message result) {

    }

    @Override
    public void getRadioCapability(Message result) {

    }

    @Override
    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForRadioCapabilityChanged(Handler h) {

    }

    @Override
    public void startLceService(int reportIntervalMs, boolean pullMode, Message result) {

    }

    @Override
    public void stopLceService(Message result) {

    }

    @Override
    public void pullLceData(Message result) {

    }

    @Override
    public void registerForLceInfo(Handler h, int what, Object obj) {

    }

    @Override
    public void unregisterForLceInfo(Handler h) {

    }

    @Override
    public void getModemActivityInfo(Message result) {

    }

    @Override
    public void setModemPower(boolean power, Message response) {
    }

    @Override
    public void setStkEvdlCallByAP(int enabled, Message response) {
    }


    @Override
    public void setOnStkEvdlCall(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnStkEvdlCall(Handler h) {
    }

    @Override
    public void setOnStkSetupMenuReset(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnStkSetupMenuReset(Handler h) {
    }

    @Override
    public void setOnStkCallCtrl(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnStkCallCtrl(Handler h) {
    }

    //MTK Data Start
    /* M: CC33 LTE Start. */
    @Override
    public void setDataOnToMD(boolean enable, Message result) {}

    @Override
    public void setRemoveRestrictEutranMode(boolean enable, Message result) {}

    @Override
    public void registerForRemoveRestrictEutran(Handler h, int what, Object obj) {}

    @Override
    public void unregisterForRemoveRestrictEutran(Handler h) {}
    /* M: CC33 LTE End. */
    /* M: IA Start. */
    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username,
            String password, Object obj, Message result) {}

    @Override
    public void registerForResetAttachApn(Handler h, int what, Object obj) {}

    @Override
    public void unregisterForResetAttachApn(Handler h) {}
    /* M: IA End. */

    @Override
    public void setupDataCall(int radioTechnology, int profile,
            String apn, String user, String password, int authType,
            String protocol, int interfaceId, Message result) {
    }

    // M: For OP12 Start
    @Override
    public void syncApnTable(String index, String apnClass, String apn, String apnType,
            String apnBearer, String apnEnable, String apnTime, String maxConn, String maxConnTime,
            String waitTime, String throttlingTime, String inactiveTimer, Message result) {
    }

    @Override
    public void syncDataSettingsToMd(boolean dataSetting, boolean dataRoamingSetting,
            Message result) {
    }

    // M: For OP12 End
    //MTK Data End

    @Override
    public void setTrm(int mode, Message result) {
    }

    @Override
    public void triggerModeSwitchByEcc(int mode, Message response) {
    }

    @Override
    public void setOnBipProactiveCmd(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnBipProactiveCmd(Handler h) {
    }

    @Override
    public void setNetworkSelectionModeManualWithAct(String operatorNumeric, String act,
            Message response) {
    }

    @Override
    public void setNetworkSelectionModeSemiAutomatic(String operatorNumeric, String act,
            Message response) {
    }

    @Override
    public void cancelAvailableNetworks(Message response) {   }

    @Override
    public void sendCNAPSS(String cnapssString, Message response) {
    }

    /// M: SS: CFU for bootup @{
    @Override
    public void registerForCallForwardingInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForCallForwardingInfo(Handler h) {
    }
    /// @}

    /// M: CC: Proprietary CRSS handling @{
    @Override
    public void setOnCallRelatedSuppSvc(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnCallRelatedSuppSvc(Handler h) {
    }
    /// @}

    /// M: CC: HangupAll for FTA 31.4.4.2 @{
    @Override
    public void hangupAll(Message result) {
    }
    /// @}

    /// M: CC: Hangup special handling @{
    @Override
    public void forceReleaseCall(int index, Message result) {
    }
    /// @}

    /// M: CC: Proprietary incoming call handling @{
    @Override
    public void setCallIndication(int mode, int callId, int seqNumber, Message result) {
    }

    @Override
    public void setOnIncomingCallIndication(Handler h, int what, Object obj) {
    }

    @Override
    public void unsetOnIncomingCallIndication(Handler h) {
    }
    /// @}

    /// M: CC: Proprietary ECC handling @{
    @Override
    public void emergencyDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
    }

    @Override
    public void setEccServiceCategory(int serviceCategory) {
    }
    /// @}

    /// M: CC: GSA HD Voice for 2/3G network support @{
    @Override
    public void setSpeechCodecInfo(boolean enable, Message response) {
    }

    @Override
    public void setOnSpeechCodecInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnSpeechCodecInfo(Handler h) {
    }
    /// @}

    /// M: CC: GSM 02.07 B.1.26 Ciphering Indicator support @{
    @Override
    public void registerForCipherIndication(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForCipherIndication(Handler h) {
    }
    /// @}

    /// M: CC: For 3G VT only @{
    @Override
    public void registerForVtStatusInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForVtStatusInfo(Handler h) {
    }

    @Override
    public void registerForVtRingInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForVtRingInfo(Handler h) {
    }

    @Override
    public void vtDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
    }

    @Override
    public void acceptVtCallWithVoiceOnly(int callId, Message result) {
    }

    @Override
    public void replaceVtCall(int index, Message result) {
    }
    /// @}

    /// M: CC: Vzw ECC/hVoLTE redial @{
    @Override
    public void registerForCallRedialState(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForCallRedialState(Handler h) {
    }
    /// @}

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd, String newCfm,
            Message result) {
    }

    @Override
    public void getCOLP(Message response) {
    }

    @Override
    public void setCOLP(boolean enable, Message response) {
    }

    @Override
    public void getCOLR(Message response) {
    }

    @Override
    public void setCLIP(boolean enable, Message response) {
    }

    @Override
    public void openIccApplication(int application, Message response) {
    }

    @Override
    public void getIccApplicationStatus(int sessionId, Message result) {
    }

    @Override
    public void registerForSessionChanged(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForSessionChanged(Handler h) {
    }

    @Override
    public void queryNetworkLock(int categrory, Message response) {
    }

    @Override
    public void setNetworkLock(int catagory, int lockop, String password, String data_imsi,
            String gid1, String gid2, Message response) {
    }

    @Override
    public void doGeneralSimAuthentication(int sessionId, int mode, int tag, String param1,
            String param2, Message response) {
    }

    @Override
    public void iccGetATR(Message result) {
    }

    @Override
    public void iccOpenChannelWithSw(String AID, Message result) {
    }

    @Override
    public void registerForSimMissing(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForSimMissing(Handler h) {
    }

    @Override
    public void registerForSimRecovery(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForSimRecovery(Handler h) {
    }

    @Override
    public void registerForVirtualSimOn(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForVirtualSimOn(Handler h) {
    }

    @Override
    public void registerForVirtualSimOff(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForVirtualSimOff(Handler h) {
    }

    @Override
    public void registerForSimPlugOut(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForSimPlugOut(Handler h) {
    }

    @Override
    public void registerForSimPlugIn(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForSimPlugIn(Handler h) {
    }

    @Override
    public void registerForTrayPlugIn(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForTrayPlugIn(Handler h) {
    }

    @Override
    public void registerForCommonSlotNoChanged(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForCommonSlotNoChanged(Handler h) {
    }

    @Override
    public void registerSetDataAllowed(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterSetDataAllowed(Handler h) {
    }

    @Override
    public void sendBTSIMProfile(int nAction, int nType, String strData, Message response) {
    }

    @Override
    public void registerForEfCspPlmnModeBitChanged(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForEfCspPlmnModeBitChanged(Handler h) {
    }

    @Override
    public void queryPhbStorageInfo(int type, Message response) {
    }

    @Override
    public void writePhbEntry(PhbEntry entry, Message result) {
    }

    @Override
    public void ReadPhbEntry(int type, int bIndex, int eIndex, Message response) {
    }

    @Override
    public void registerForPhbReady(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForPhbReady(Handler h) {
    }

    @Override
    public void queryUPBCapability(Message response) {
    }

    @Override
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal,
            String tonForNum, String aasAnrIndex, Message response) {
    }

    @Override
    public void editUPBEntry(int entryType, int adnIndex, int entryIndex, String strVal,
            String tonForNum, Message response) {
    }

    @Override
    public void deleteUPBEntry(int entryType, int adnIndex, int entryIndex, Message response) {
    }

    @Override
    public void readUPBGasList(int startIndex, int endIndex, Message response) {
    }

    @Override
    public void readUPBGrpEntry(int adnIndex, Message response) {
    }

    @Override
    public void writeUPBGrpEntry(int adnIndex, int[] grpIds, Message response) {
    }

    @Override
    public void getPhoneBookStringsLength(Message result) {
    }

    @Override
    public void getPhoneBookMemStorage(Message result) {
    }

    @Override
    public void setPhoneBookMemStorage(String storage, String password, Message result) {
    }

    @Override
    public void readPhoneBookEntryExt(int index1, int index2, Message result) {
    }

    @Override
    public void writePhoneBookEntryExt(PBEntry entry, Message result) {
    }

    @Override
    public void queryUPBAvailable(int eftype, int fileIndex, Message response) {
    }

    @Override
    public void readUPBEmailEntry(int adnIndex, int fileIndex, Message response) {
    }

    @Override
    public void readUPBSneEntry(int adnIndex, int fileIndex, Message response) {
    }

    @Override
    public void readUPBAnrEntry(int adnIndex, int fileIndex, Message response) {
    }

    @Override
    public void readUPBAasList(int startIndex, int endIndex, Message response) {
    }

    @Override
    public void getSmsParameters(Message response) {
    }

    @Override
    public void setSmsParameters(SmsParameters params, Message response) {
    }

    @Override
    public void getSmsSimMemoryStatus(Message result) {
    }

    @Override
    public void setEtws(int mode, Message result) {
    }

    @Override
    public void setOnEtwsNotification(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnEtwsNotification(Handler h) {
    }

    @Override
    public void setOnMeSmsFull(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnMeSmsFull(Handler h) {
    }

    @Override
    public void registerForSmsReady(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForSmsReady(Handler h) {
    }

    @Override
    public void setCellBroadcastChannelConfigInfo(String config, int cb_set_type,
            Message response) {
    }

    @Override
    public void setCellBroadcastLanguageConfigInfo(String config, Message response) {
    }

    @Override
    public void queryCellBroadcastConfigInfo(Message response) {
    }

    @Override
    public void removeCellBroadcastMsg(int channelId, int serialId, Message response) {
    }

    @Override
    public void setCDMACardInitalEsnMeid(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetCDMACardInitalEsnMeid(Handler h) {
    }

    @Override
    public void getPOLCapabilty(Message response) {
    }

    @Override
    public void getCurrentPOLList(Message response) {
    }

    @Override
    public void setPOLEntry(int index, String numeric, int nAct, Message response) {
    }

    @Override
    public void registerForPsNetworkStateChanged(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForPsNetworkStateChanged(Handler h) {
    }

    @Override
    public void registerForIMEILock(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForIMEILock(Handler h) {
    }

    @Override
    public void setInvalidSimInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetInvalidSimInfo(Handler h) {
    }

    @Override
    public void registerForGetAvailableNetworksDone(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForGetAvailableNetworksDone(Handler h) {
    }

    @Override
    public boolean isGettingAvailableNetworks() {

        return false;
    }

    @Override
    public void getFemtoCellList(String operatorNumeric, int rat, Message response) {
    }

    @Override
    public void abortFemtoCellList(Message response) {
    }

    @Override
    public void selectFemtoCell(FemtoCellInfo femtocell, Message response) {
    }

    @Override
    public void registerForFemtoCellInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForFemtoCellInfo(Handler h) {
    }

    @Override
    public void registerForNeighboringInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForNeighboringInfo(Handler h) {
    }

    @Override
    public void registerForNetworkInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForNetworkInfo(Handler h) {
    }

    @Override
    public void registerForImsEnable(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForImsEnable(Handler h) {
    }

    @Override
    public void registerForImsDisable(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForImsDisable(Handler h) {
    }

    @Override
    public void setIMSEnabled(boolean enable, Message response) {
    }

    @Override
    public void registerForImsDisableDone(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForImsDisableDone(Handler h) {
    }

    @Override
    public void registerForImsRegistrationInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForImsRegistrationInfo(Handler h) {
    }

    @Override
    public void setOnPlmnChangeNotification(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnPlmnChangeNotification(Handler h) {
    }

    @Override
    public void setOnRegistrationSuspended(Handler h, int what, Object obj) {
    }

    @Override
    public void unSetOnRegistrationSuspended(Handler h) {
    }

    @Override
    public void storeModemType(int modemType, Message response) {
    }

    @Override
    public void reloadModemType(int modemType, Message response) {
    }

    @Override
    public void queryModemType(Message response) {
    }

    @Override
    public void registerForMelockChanged(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForMelockChanged(Handler h) {
    }

    @Override
    public void setFDMode(int mode, int parameter1, int parameter2, Message response) {
    }

    @Override
    public void registerForEconfSrvcc(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForEconfSrvcc(Handler h) {
    }

    @Override
    public void registerForEconfResult(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForEconfResult(Handler h) {
    }

    @Override
    public void registerForCallInfo(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForCallInfo(Handler h) {
    }

    @Override
    public void addConferenceMember(int confCallId, String address, int callIdToAdd,
            Message response) {
    }

    @Override
    public void removeConferenceMember(int confCallId, String address, int callIdToRemove,
            Message response) {
    }

    @Override
    public void resumeCall(int callIdToResume, Message response) {
    }

    @Override
    public void holdCall(int callIdToHold, Message response) {
    }

    @Override
    public void sendScreenState(boolean on) {
    }

    @Override
    public void setLteAccessStratumReport(boolean enable, Message result) {
    }

    @Override
    public void setLteUplinkDataTransfer(int state, int interfaceId, Message result) {
    }

    @Override
    public void registerForLteAccessStratumState(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForLteAccessStratumState(Handler h) {
    }

    @Override
    public void registerForModulation(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForModulation(Handler h) {
    }

    @Override
    public void setDataCentric(boolean enable, Message response) {
    }

    @Override
    public void setImsCallStatus(boolean existed, Message response) {
    }

    @Override
    public void updateImsRegistrationStatus(int regState, int regType, int reason) {
    }

    @Override
    public void setBandMode(int[] bandMode, Message response) {
    }

    @Override
    public void registerForAbnormalEvent(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForAbnormalEvent(Handler h) {
    }

    @Override
    public int getDisplayState() {

        return 0;
    }

    @Override
    public String lookupOperatorNameFromNetwork(long subId, String numeric,
            boolean desireLongName) {

        return null;
    }

    @Override
    public void conferenceDial(String[] participants, int clirMode, boolean isVideoCall,
            Message result) {
    }

    @Override
    public void registerForImsiRefreshDone(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForImsiRefreshDone(Handler h) {
    }

    @Override
    public RadioCapability getBootupRadioCapability() {

        return null;
    }

    @Override
    public void setRegistrationSuspendEnabled(int enabled, Message response) {
    }

    @Override
    public void setResumeRegistration(int sessionId, Message response) {
    }

    @Override
    public void enableMd3Sleep(int enable) {
    }

    @Override
    public void registerForNetworkExsit(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForNetworkExsit(Handler h) {
    }

    @Override
    public void registerForNetworkEvent(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForNetworkEvent(Handler h) {
    }

    @Override
    public void registerForCallAccepted(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForCallAccepted(Handler h) {
    }

    @Override
    public void setSimPower(int mode, Message response) {
    }

    @Override
    public void registerForPcoStatus(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForPcoStatus(Handler h) {
    }

    @Override
    public void registerForAttachApnChanged(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForAttachApnChanged(Handler h) {
    }

    @Override
    public void registerForGmssRatChanged(Handler h, int what, Object obj) {
    }
}
