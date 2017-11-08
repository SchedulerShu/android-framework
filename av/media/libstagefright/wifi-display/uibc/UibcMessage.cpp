

#define LOG_TAG "UibcMessage"
#include <utils/Log.h>

#include "UibcMessage.h"

#include <media/IRemoteDisplayClient.h>

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/foundation/hexdump.h>


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <ctype.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <netinet/in.h>

#include <unistd.h>
#include <linux/fb.h>
#include <sys/mman.h>

#include <ui/DisplayInfo.h>
#include <gui/ISurfaceComposer.h>
#include <gui/SurfaceComposerClient.h>

namespace android {

static const scanCodeBuild_t scanCode_DefaultMap[] = {
    {0x00}, //0x00  #   NULL
    {0x00}, //0x01  #   START OF HEADING
    {0x00}, //0x02  #   START OF TEXT
    {0x00}, //0x03  #   END OF TEXT
    {0x00}, //0x04  #   END OF TRANSMISSION
    {0x00}, //0x05  #   ENQUIRY
    {0x00}, //0x06  #   ACKNOWLEDGE
    {0x00}, //0x07  #   BELL
    {KEY_BACKSPACE}, //0x08 #   BACKSPACE
    {KEY_TAB}, //0x09   #   HORIZONTAL TABULATION
    {KEY_LINEFEED}, //0x0A  #   LINE FEED
    {KEY_TAB}, //0x0B   #   VERTICAL TABULATION
    {KEY_PAGEDOWN}, //0x0C  #   FORM FEED
    {KEY_ENTER}, //0x0D #   CARRIAGE RETURN
    {0x00}, //0x0E  #   SHIFT OUT
    {KEY_LEFTSHIFT}, //0x0F #   SHIFT IN
    {0x00}, //0x10  #   DATA LINK ESCAPE
    {KEY_LEFTCTRL}, //0x11  #   DEVICE CONTROL ONE
    {KEY_LEFTCTRL}, //0x12  #   DEVICE CONTROL TWO
    {KEY_LEFTCTRL}, //0x13  #   DEVICE CONTROL THREE
    {KEY_LEFTCTRL}, //0x14  #   DEVICE CONTROL FOUR
    {0x00}, //0x15  #   NEGATIVE ACKNOWLEDGE
    {0x00}, //0x16  #   SYNCHRONOUS IDLE
    {0x00}, //0x17  #   END OF TRANSMISSION BLOCK
    {KEY_CANCEL}, //0x18    #   CANCEL
    {0x00}, //0x19  #   END OF MEDIUM
    {0x00}, //0x1A  #   SUBSTITUTE
    {KEY_BACK}, //0x1B  #   ESCAPE
    {0x00}, //0x1C  #   FILE SEPARATOR
    {0x00}, //0x1D  #   GROUP SEPARATOR
    {0x00}, //0x1E  #   RECORD SEPARATOR
    {0x00}, //0x1F  #   UNIT SEPARATOR
};

#define UIBC_KEYCODE_UNKNOWN KEY_UNKNOWN

UibcMessage::UibcMessage(UibcMessage::MessageType type,
                         const char* inEventDesc,
                         double widthRatio,
                         double heightRatio)
    : mOutBuf(NULL),
      m_DataValid(false) {

    switch (type) {
    case GENERIC_TOUCH_DOWN:
    case GENERIC_TOUCH_UP:
    case GENERIC_TOUCH_MOVE:
        makeUIBCGenericTouchPacket(inEventDesc,
                                   widthRatio,
                                   heightRatio);
        break;

    case GENERIC_KEY_DOWN:
    case GENERIC_KEY_UP:
        makeUIBCGenericKeyPacket(inEventDesc);
        break;

    case GENERIC_ZOOM:
        makeUIBCGenericZoomPacket(inEventDesc);
        break;

    case GENERIC_VERTICAL_SCROLL:
    case GENERIC_HORIZONTAL_SCROLL:
        makeUIBCGenericScalePacket(inEventDesc);
        break;

    case GENERIC_ROTATE:
        makeUIBCGenericRotatePacket(inEventDesc);
        break;
    };
}

UibcMessage::~UibcMessage() {
}

char* UibcMessage::getPacketData() {
    return (char*)mOutBuf->data();
}

int UibcMessage::getPacketDataLen() {
    return mOutBuf->size();
}

bool UibcMessage::isDataValid() {
    return m_DataValid;
}

// format: "typeId, number of pointers, pointer Id1, X coordnate, Y coordnate, , pointer Id2, X coordnate, Y coordnate,..."
sp<ABuffer> UibcMessage::makeUIBCGenericTouchPacket(const char *inEventDesc,
        double widthRatio,
        double heightRatio) {
    ALOGD("getUIBCGenericTouchPacket (%s)", inEventDesc);
    int16_t typeId, genericPacketLen, timestamp, temp, numberOfPointers;

    char** splitedStr = UibcMessage::str_split((char*)inEventDesc, ",");

    typeId = atoi(*(splitedStr + 0));
    numberOfPointers = atoi(*(splitedStr + 1));

    genericPacketLen = sizeof(WFD_UIBC_FORMAT_HDR_TS) +
                       sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) +
                       1 + //number of pointers
                       numberOfPointers * 5;

    //ALOGD("getUIBCGenericTouchPacket genericPacketLen=[%d]\n", genericPacketLen);
    mOutBuf = new ABuffer(genericPacketLen);

    WFD_UIBC_FORMAT_HDR_TS* pHeader = (WFD_UIBC_FORMAT_HDR_TS*)mOutBuf->data();
    WFD_UIBC_GENERIC_MSG* pBody =
        (WFD_UIBC_GENERIC_MSG*)(mOutBuf->data() + sizeof(WFD_UIBC_FORMAT_HDR_TS));

    pHeader->byte1 = 0x00 | UIBC_TIMESTAMP_MASK; //Version (3 bits),T (1 bit),Reserved(4 bits)
    pHeader->byte2 = 0x00; //Reserved(4 bits),Input Category (4 bits)
    pHeader->length = htons(genericPacketLen);
    timestamp = getU16TickCountMs();
    pHeader->timestamp = htons(timestamp);
    ALOGD("getUIBCGenericTouchPacket timestamp=[%u]\n", timestamp);

    pBody->genericHeader.ieID = typeId;
    pBody->genericHeader.   length = htons(numberOfPointers * 5 + 1);
    pBody->touchMsg.numPointers = numberOfPointers;

    //ALOGD("getUIBCGenericTouchPacket numberOfPointers=[%d]\n", numberOfPointers);
    for (int i = 0; i < numberOfPointers; i++) {
        temp = atoi(*(splitedStr + i * 3 + 2));
        pBody->touchMsg.coordinates[i].pointerID = temp;
        //ALOGD("getUIBCGenericTouchPacket PointerId=[%d]\n",
              //pBody->touchMsg.coordinates[i].pointerID);

        temp = atoi(*(splitedStr + i * 3 + 3));
        temp = (int32_t)((double)temp / widthRatio);
        pBody->touchMsg.coordinates[i].x = htons(temp);
        //ALOGD("getUIBCGenericTouchPacket X-coordinate=[%d]\n",
              //pBody->touchMsg.coordinates[i].x);

        temp = atoi(*(splitedStr + i * 3 + 4));
        temp = (int32_t)((double)temp / heightRatio);
        pBody->touchMsg.coordinates[i].y = htons(temp);
        //ALOGD("getUIBCGenericTouchPacket Y-coordinate=[%d]\n",
              //pBody->touchMsg.coordinates[i].y);
    }

    for (int i = 0; * (splitedStr + i); i++) {
        free(*(splitedStr + i));
    }
    free(splitedStr);

    //hexdump(mOutBuf->data(), mOutBuf->size());
    m_DataValid = true;
    return mOutBuf;
}

// format: "typeId, Key code 1(0x00), Key code 2(0x00)"
sp<ABuffer> UibcMessage::makeUIBCGenericKeyPacket(const char *inEventDesc) {
    ALOGD("getUIBCGenericKeyPacket (%s)", inEventDesc);
    int16_t typeId, genericPacketLen, timestamp, temp;
    int32_t temp32;

    char** splitedStr = UibcMessage::str_split((char*)inEventDesc, ",");
    typeId = atoi(*(splitedStr + 0));

    genericPacketLen = sizeof(WFD_UIBC_FORMAT_HDR_TS) +
                       sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) +
                       sizeof(WFD_UIBC_GENERIC_BODY_KEY);

    //ALOGD("getUIBCGenericKeyPacket genericPacketLen=[%d]\n", genericPacketLen);
    mOutBuf = new ABuffer(genericPacketLen);

    WFD_UIBC_FORMAT_HDR_TS* pHeader = (WFD_UIBC_FORMAT_HDR_TS*)mOutBuf->data();
    WFD_UIBC_GENERIC_MSG* pBody =
        (WFD_UIBC_GENERIC_MSG*)(mOutBuf->data() + sizeof(WFD_UIBC_FORMAT_HDR_TS));

    pHeader->byte1 = 0x00 | UIBC_TIMESTAMP_MASK; //Version (3 bits),T (1 bit),Reserved(4 bits)
    pHeader->byte2 = 0x00; //Reserved(4 bits),Input Category (4 bits)
    pHeader->length = htons(genericPacketLen);
    timestamp = getU16TickCountMs();
    pHeader->timestamp = htons(timestamp);
    //ALOGD("getUIBCGenericKeyPacket timestamp=[%u]\n", timestamp);

    pBody->genericHeader.ieID = typeId;
    pBody->genericHeader.   length = htons(sizeof(WFD_UIBC_GENERIC_BODY_KEY));
    pBody->keyMsg.reserved = 0x00;

    sscanf(*(splitedStr + 1), " 0x%04X", &temp32);
    temp = temp32 & 0xFFFF;
    //ALOGD("getUIBCGenericKeyPacket pBody->keyMsg.code1=[%d]\n", htons(temp));
    pBody->keyMsg.code1 = htons(temp);

    sscanf(*(splitedStr + 2), " 0x%04X", &temp32);
    temp = temp32 & 0xFFFF;
    //ALOGD("getUIBCGenericKeyPacket pBody->keyMsg.code2=[%d]\n", htons(temp));
    pBody->keyMsg.code2 = htons(temp);

    for (int i = 0; * (splitedStr + i); i++) {
        free(*(splitedStr + i));
    }
    free(splitedStr);

    //hexdump(mOutBuf->data(), mOutBuf->size());
    m_DataValid = true;
    return mOutBuf;
}

// format: "typeId,  X coordnate, Y coordnate, integer part, fraction part"
sp<ABuffer> UibcMessage::makeUIBCGenericZoomPacket(const char *inEventDesc) {
    ALOGD("getUIBCGenericZoomPacket (%s)", inEventDesc);
    int16_t typeId, genericPacketLen, timestamp, temp;

    char** splitedStr = UibcMessage::str_split((char*)inEventDesc, ",");
    typeId = atoi(*(splitedStr + 0));

    genericPacketLen = sizeof(WFD_UIBC_FORMAT_HDR_TS) +
                       sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) +
                       sizeof(WFD_UIBC_GENERIC_BODY_ZOOM);

    //ALOGD("getUIBCGenericZoomPacket genericPacketLen=[%d]\n", genericPacketLen);
    mOutBuf = new ABuffer(genericPacketLen);

    WFD_UIBC_FORMAT_HDR_TS* pHeader = (WFD_UIBC_FORMAT_HDR_TS*)mOutBuf->data();
    WFD_UIBC_GENERIC_MSG* pBody =
        (WFD_UIBC_GENERIC_MSG*)(mOutBuf->data() + sizeof(WFD_UIBC_FORMAT_HDR_TS));

    pHeader->byte1 = 0x00 | UIBC_TIMESTAMP_MASK; //Version (3 bits),T (1 bit),Reserved(4 bits)
    pHeader->byte2 = 0x00; //Reserved(4 bits),Input Category (4 bits)
    pHeader->length = htons(genericPacketLen);
    timestamp = getU16TickCountMs();
    pHeader->timestamp = htons(timestamp);
    //ALOGD("getUIBCGenericZoomPacket timestamp=[%u]\n", timestamp);

    pBody->genericHeader.ieID = typeId;
    pBody->genericHeader.   length = htons(sizeof(WFD_UIBC_GENERIC_BODY_ZOOM));

    temp = sscanf(*(splitedStr + 1), " 0x%04X", (int*)&temp);
    pBody->zoomMsg.x = htons(temp);

    temp = sscanf(*(splitedStr + 2), " 0x%04X", (int*)&temp);
    pBody->zoomMsg.y = htons(temp);

    pBody->zoomMsg.intTimes = atoi(*(splitedStr + 3));
    pBody->zoomMsg.fractTimes = atoi(*(splitedStr + 4));

    for (int i = 0; * (splitedStr + i); i++) {
        free(*(splitedStr + i));
    }
    free(splitedStr);

    //hexdump(mOutBuf->data(), mOutBuf->size());
    m_DataValid = true;
    return mOutBuf;
}

// format: "typeId,  unit, direction, amount to scroll"
sp<ABuffer> UibcMessage::makeUIBCGenericScalePacket(const char *inEventDesc) {
    ALOGD("getUIBCGenericScalePacket (%s)", inEventDesc);
    int32_t typeId;
    int32_t uibcBodyLen, genericPacketLen;
    int32_t temp;

    char** splitedStr = UibcMessage::str_split((char*)inEventDesc, ",");

    for (int i = 0; * (splitedStr + i); i++) {
        //ALOGD("getUIBCGenericScalePacket splitedStr tokens=[%s]\n", *(splitedStr + i));
        switch (i) {
        case 0: {
            typeId = atoi(*(splitedStr + i));
            //ALOGD("getUIBCGenericScalePacket typeId=[%d]\n", typeId);
            genericPacketLen = 2;
            uibcBodyLen = genericPacketLen + 7; // Generic herder leh = 7
            //mOutBuf->data() = (char*)malloc(uibcBodyLen + 1);
            mOutBuf = new ABuffer(genericPacketLen);
            // UIBC header
            mOutBuf->data()[0] = 0x00; //Version (3 bits),T (1 bit),Reserved(8 bits),Input Category (4 bits)
            mOutBuf->data()[1] = 0x00; //Version (3 bits),T (1 bit),Reserved(8 bits),Input Category (4 bits)
            mOutBuf->data()[2] = (uibcBodyLen >> 8) & 0xFF; //Length(16 bits)
            mOutBuf->data()[3] = uibcBodyLen & 0xFF; //Length(16 bits)
            //Generic Input Body Format
            mOutBuf->data()[4] = typeId & 0xFF; // Tyoe ID, 1 octet
            mOutBuf->data()[5] = (genericPacketLen >> 8) & 0xFF; // Length, 2 octets
            mOutBuf->data()[6] = genericPacketLen & 0xFF; // Length, 2 octets
            mOutBuf->data()[7] = 0x00; // Clear the byte
            mOutBuf->data()[8] = 0x00; // Clear the byte
            /*
            B15B14; Scroll Unit Indication bits.
            0b00; the unit is a pixel (normalized with respect to the WFD Source display resolution that is conveyed in an RTSP M4 request message).
            0b01; the unit is a mouse notch (where the application is responsible for representing the number of pixels per notch).
            0b10-0b11; Reserved.

            B13; Scroll Direction Indication bit.
            0b0; Scrolling to the right. Scrolling to the right means the displayed content being shifted to the left from a user perspective.
            0b1; Scrolling to the left. Scrolling to the left means the displayed content being shifted to the right from a user perspective.

            B12:B0; Number of Scroll bits.
            Number of units for a Horizontal scroll.
            */
            break;
        }
        case 1: {
            temp = atoi(*(splitedStr + i));
            //ALOGD("getUIBCGenericScalePacket unit=[%d]\n", temp);
            mOutBuf->data()[7] = (temp >> 8) & 0xFF;
            break;
        }
        case 2: {
            temp = atoi(*(splitedStr + i));
            //ALOGD("getUIBCGenericScalePacket direction=[%d]\n", temp);
            mOutBuf->data()[7] |= ((temp >> 10) & 0xFF);
            break;

        }
        case 3: {
            temp = atoi(*(splitedStr + i));
            //ALOGD("getUIBCGenericScalePacket amount to scroll=[%d]\n", temp);
            mOutBuf->data()[7] |= ((temp >> 12) & 0xFF);
            mOutBuf->data()[8] = temp & 0xFF;
            break;
        }
        default: {
            break;
        }
        }
        free(*(splitedStr + i));
    }

    free(splitedStr);
    //hexdump(mOutBuf->data(), mOutBuf->size());
    m_DataValid = true;
    return mOutBuf;
}

// format: "typeId,  integer part, fraction part"
sp<ABuffer> UibcMessage::makeUIBCGenericRotatePacket(const char * inEventDesc) {
    ALOGD("getUIBCGenericRotatePacket (%s)", inEventDesc);
    int16_t typeId, genericPacketLen, timestamp;


    char** splitedStr = UibcMessage::str_split((char*)inEventDesc, ",");
    typeId = atoi(*(splitedStr + 0));

    genericPacketLen = sizeof(WFD_UIBC_FORMAT_HDR_TS) +
                       sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) +
                       sizeof(WFD_UIBC_GENERIC_BODY_ROTATE);

    //ALOGD("getUIBCGenericRotatePacket genericPacketLen=[%d]\n", genericPacketLen);
    mOutBuf = new ABuffer(genericPacketLen);

    WFD_UIBC_FORMAT_HDR_TS* pHeader = (WFD_UIBC_FORMAT_HDR_TS*)mOutBuf->data();
    WFD_UIBC_GENERIC_MSG* pBody =
        (WFD_UIBC_GENERIC_MSG*)(mOutBuf->data() + sizeof(WFD_UIBC_FORMAT_HDR_TS));

    pHeader->byte1 = 0x00 | UIBC_TIMESTAMP_MASK; //Version (3 bits),T (1 bit),Reserved(4 bits)
    pHeader->byte2 = 0x00; //Reserved(4 bits),Input Category (4 bits)
    pHeader->length = htons(genericPacketLen);
    timestamp = getU16TickCountMs();
    pHeader->timestamp = htons(timestamp);
    //ALOGD("getUIBCGenericRotatePacket timestamp=[%u]\n", timestamp);

    pBody->genericHeader.ieID = typeId;
    pBody->genericHeader.   length = htons(sizeof(WFD_UIBC_GENERIC_BODY_ROTATE));

    pBody->rotateMsg.intAmount = atoi(*(splitedStr + 1));
    pBody->rotateMsg.fractAmount = atoi(*(splitedStr + 2));

    for (int i = 0; * (splitedStr + i); i++) {
        free(*(splitedStr + i));
    }
    free(splitedStr);

    hexdump(mOutBuf->data(), mOutBuf->size());
    //m_DataValid = true;
    return mOutBuf;
}


//static
scanCodeBuild_t UibcMessage::asciiToScancodeBuild(UINT16 asciiCode) {
    scanCodeBuild_t ret = scanCode_DefaultMap[0];

    ALOGD("asciiCode: %d", asciiCode);

    ret = scanCode_DefaultMap[asciiCode];

    ALOGD("scanCode: %d", ret.scanCode);
    return ret;
}

//static
short UibcMessage::scancodeToAcsii(UINT8 scanCode) {
    short ret = UIBC_KEYCODE_UNKNOWN;

    ALOGD("scanCode : %d", scanCode);

    for (unsigned int i = 0; i < (sizeof(scanCode_DefaultMap) / sizeof(scanCode_DefaultMap[0])); i++) {
        if (scanCode == scanCode_DefaultMap[i].scanCode) {
            ret = i;
            break;
        }
    }

    ALOGD("asciiCode: %d", ret);
    return ret;
}

//static
void UibcMessage::getScreenResolution(int* x, int* y) {
    sp<IBinder> display = SurfaceComposerClient::getBuiltInDisplay(
                              ISurfaceComposer::eDisplayIdMain);
    DisplayInfo info;
    SurfaceComposerClient::getDisplayInfo(display, &info);
    *x = info.w;
    *y = info.h;
}

char** UibcMessage::str_split(char* pStr, const char* pDelim) {
    char** result    = 0;
    size_t count     = 0;
    char* tmp        = pStr;
    char* tmpStr     = NULL;
    char* last_comma = 0;

    /* Count how many elements will be extracted. */
    while (*tmp) {
        if (*pDelim == *tmp) {
            count++;
            last_comma = tmp;
        }
        tmp++;
    }

    /* Add space for trailing token. */
    count += last_comma < (pStr + strlen(pStr) - 1) ? 1 : 0;

    /* Add space for terminating null string so caller
       knows where the list of returned strings ends. */
    count++;
    //ALOGD("str_split count: %d", count);

    result = (char**)malloc(sizeof(char*) * count);

    tmp = tmpStr = strdup(pStr);
    size_t idx  = 0;
    char* token;
    while ((token = strsep(&tmp, pDelim)) != NULL) {
        //ALOGD("str_split token: \"%s\"", token);
        * (result + idx++) = strdup(token);
        //ALOGD("str_split  *(result + (idx-1)) : \"%s\"",  *(result + idx - 1) );
    }
    CHECK_EQ(idx , count - 1);
    *(result + idx) = 0;
    free(tmpStr);
    return result;
}

UINT16 UibcMessage::getU16TickCountMs() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (int64_t)(tv.tv_sec * 1000LL + tv.tv_usec / 1000);
}


}
