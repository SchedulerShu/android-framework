/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include <cutils/log.h>
#include <utils/Errors.h>
#include <utils/Trace.h>
#include <ui/gralloc_extra.h>

#include "SurfaceFlinger.h"
#include "Layer.h"
#include "graphics_mtk_defs.h"
#include "IPQService.h"

namespace android {

static const char *white[] = {
    "com.yxcorp.gifshow.activity.PhotoActivity"
};

bool SurfaceFlinger::isGlobalPQEnabled() {
    int32_t pq = 0;

    if (mPQService == NULL) {
        const String16 service_name("PQ");
        sp<IBinder> bs = defaultServiceManager()->checkService(service_name);
        if (bs == NULL) {
            return false;
        }
        mPQService = interface_cast<IPQService>(bs);
    }
    status_t status = mPQService->getGlobalPQSwitch(&pq);
    if (status != NO_ERROR) {
        if (status == DEAD_OBJECT) {
            mPQService = NULL;
        }
        return false;
    } else {
        return pq != 0;
    }
}

sp<Layer> SurfaceFlinger::selectPQLayerSortedByZ(const Vector< sp<Layer> >& layers) {
    sp<Layer> select = NULL;

    // lowest & highest rgbx layer
    sp<Layer> rgbx_l = NULL;
    sp<Layer> rgbx_h = NULL;

    // reject when multi-display
    for (size_t dpy = 1; dpy < mDisplays.size(); dpy++) {
        const sp<DisplayDevice>& hw(mDisplays[dpy]);
        if (hw->isDisplayOn()) {
            return NULL;
        }
    }

    for (size_t i = 0; i < layers.size(); i++) {
        const sp<Layer>& layer(layers[i]);

        const sp<GraphicBuffer>& buffer(layer->getActiveBuffer());
        if (buffer == NULL) {
            continue;
        }

        unsigned int format = 0;
        gralloc_extra_ion_sf_info_t info;

        int err = gralloc_extra_query(buffer->handle, GRALLOC_EXTRA_GET_FORMAT, &format);
        err |= gralloc_extra_query(buffer->handle, GRALLOC_EXTRA_GET_IOCTL_ION_SF_INFO, &info);
        int type = (info.status & GRALLOC_EXTRA_MASK_TYPE);
        if (err ||
            (type == GRALLOC_EXTRA_BIT_TYPE_VIDEO ||
            type == GRALLOC_EXTRA_BIT_TYPE_CAMERA ||
            format == HAL_PIXEL_FORMAT_YV12)) {
            // video case no ui pq
            select = NULL;
            break;
        }

        if (HAL_PIXEL_FORMAT_RGBX_8888 == format ||
            HAL_PIXEL_FORMAT_BGRX_8888 == format ||
            HAL_PIXEL_FORMAT_RGB_888 == format ||
            HAL_PIXEL_FORMAT_RGB_565 == format) {
            // select the highest z-order rgbx layer
            select = layer;
            if (format == HAL_PIXEL_FORMAT_RGBX_8888) {
                if (rgbx_l == NULL)
                    rgbx_l = layer;
                rgbx_h = layer;
            }
        } else if (HAL_PIXEL_FORMAT_RGBA_8888 == format ||
            HAL_PIXEL_FORMAT_BGRA_8888 == format) {
            // select the lowest z-order rgba layer
            if (select == NULL) {
                select = layer;
            }
        }
    }

    // check white list app
    if (select != NULL) {
        for (size_t i = 0; i < layers.size(); i++) {
            const sp<Layer>& layer(layers[i]);

            if (layer->isInWhiteList()) {
                select = layer;
                break;
            }
        }
    }

    // special case for animation from app to home screen
    if ((select != NULL) && (select == rgbx_h)) {
         // more than 2 rgbx layers
        if (rgbx_l != rgbx_h) {
            Rect r_l = rgbx_l->getDisplayFrame();
            sp<const DisplayDevice> hw(mDisplays[0]);
            if ((r_l.getHeight() * r_l.getWidth()) == (hw->getWidth() * hw->getHeight())) {
                Rect r_h = rgbx_h->getDisplayFrame();
                if ((r_h.getHeight() * r_h.getWidth() * 10) < (hw->getWidth() * hw->getHeight() * 7)) {
                    select = rgbx_l;
                }
            }
        }
    }

    return select;
}

void SurfaceFlinger::checkAndSetPQFlagForDisplay() {
    ATRACE_CALL();
    // only need handle main display
    sp<const DisplayDevice> hw(mDisplays[0]);
    if (!hw->isDisplayOn()) {
        return;
    }

    if (false == isGlobalPQEnabled())
        return;

    sp<Layer> layer = selectPQLayerSortedByZ(hw->getVisibleLayersSortedByZ());
    if (layer != NULL) {
        ALOGD("handleUIPQ select layer %s", layer->getName().string());
        if (layer->getName().contains("ScreenshotSurface"))
            ALOGD("handleUIPQ select special layer, so no pq");
        else
            layer->setPQForUI(true);
    } else {
        ALOGD("handleUIPQ select none layer");
    }
}

sp<Layer> SurfaceFlinger::checkAndSetPQFlagForCapture(
    const sp<const DisplayDevice>& hw,
    const sp<IGraphicBufferProducer>& producer,
    const uint32_t& minLayerZ, const uint32_t& maxLayerZ) {
    ATRACE_CALL();

    if (false == isGlobalPQEnabled())
        return NULL;

    Vector< sp<Layer> > captureLayersSortedByZ;
    const LayerVector& layers(mDrawingState.layersSortedByZ);

    for (size_t i = 0; i < layers.size(); ++i) {
        const sp<Layer>& layer(layers[i]);
        const Layer::State& state(layer->getDrawingState());
        if (state.layerStack == hw->getLayerStack()) {
            if (state.z >= minLayerZ && state.z <= maxLayerZ) {
                if (layer->isVisible()) {
                    captureLayersSortedByZ.add(layer);
                }
            }
        }
    }

    // disable PQ of video layers when capture result is saved in a file
    int usage = 0;
    producer->query(NATIVE_WINDOW_CONSUMER_USAGE_BITS, &usage);
    if (!(usage & GRALLOC_USAGE_HW_COMPOSER)) {
        for (size_t i = 0; i < captureLayersSortedByZ.size(); ++i) {
            const sp<Layer>& layer(captureLayersSortedByZ[i]);
            layer->setPQForVideo(false);
        }
        return NULL;
    }

    sp<Layer> layer = selectPQLayerSortedByZ(captureLayersSortedByZ);
    if (layer != NULL) {
        layer->setPQForUI(true);
        ALOGD("setPQLayerForCapture set layer %s", layer->getName().string());
    } else {
        ALOGD("setPQLayerForCapture set none layer");
    }

    return layer;
}

void Layer::checkLayerInWhiteList() {
    if (isDim())
        return;

    for (size_t i = 0; i < sizeof(white) / sizeof(char *); i++) {
        if (mName.contains(white[i])) {
            mInWhiteList = true;
            break;
        }
    }
}

void Layer::setPQForUI(const bool& need) {
    if (mActiveBuffer == NULL) {
        return;
    }

    if (need && !mUINeedPQ) {
        mUINeedPQ = true;
        gralloc_extra_ion_sf_info_t sf_info;
        gralloc_extra_query(mActiveBuffer->handle, GRALLOC_EXTRA_GET_IOCTL_ION_SF_INFO, &sf_info);
        gralloc_extra_sf_set_status2(&sf_info, GRALLOC_EXTRA_MASK2_UI_PQ, GRALLOC_EXTRA_BIT2_UI_PQ_ON);
        gralloc_extra_perform(mActiveBuffer->handle, GRALLOC_EXTRA_SET_IOCTL_ION_SF_INFO, &sf_info);
        ATRACE_NAME(getName().string());
    } else if (!need && mUINeedPQ) {
        mUINeedPQ = false;
        gralloc_extra_ion_sf_info_t sf_info;
        gralloc_extra_query(mActiveBuffer->handle, GRALLOC_EXTRA_GET_IOCTL_ION_SF_INFO, &sf_info);
        gralloc_extra_sf_set_status2(&sf_info, GRALLOC_EXTRA_MASK2_UI_PQ, GRALLOC_EXTRA_BIT2_UI_PQ_OFF);
        gralloc_extra_perform(mActiveBuffer->handle, GRALLOC_EXTRA_SET_IOCTL_ION_SF_INFO, &sf_info);
    }

}

void Layer::setPQForVideo(const bool& need) {
    if (mActiveBuffer == NULL) {
        return;
    }

    if (need && !mVideoNeedPQ) {
        mVideoNeedPQ = true;
        // set video need pq
        gralloc_extra_ion_sf_info_t sf_info;
        gralloc_extra_query(mActiveBuffer->handle, GRALLOC_EXTRA_GET_IOCTL_ION_SF_INFO, &sf_info);
        gralloc_extra_sf_set_status2(&sf_info, GRALLOC_EXTRA_MASK2_VIDEO_PQ, GRALLOC_EXTRA_BIT2_VIDEO_PQ_ON);
        gralloc_extra_perform(mActiveBuffer->handle, GRALLOC_EXTRA_SET_IOCTL_ION_SF_INFO, &sf_info);
    } else if (!need && mVideoNeedPQ) {
        mVideoNeedPQ = false;
        // set video no need pq
        gralloc_extra_ion_sf_info_t sf_info;
        gralloc_extra_query(mActiveBuffer->handle, GRALLOC_EXTRA_GET_IOCTL_ION_SF_INFO, &sf_info);
        gralloc_extra_sf_set_status2(&sf_info, GRALLOC_EXTRA_MASK2_VIDEO_PQ, GRALLOC_EXTRA_BIT2_VIDEO_PQ_OFF);
        gralloc_extra_perform(mActiveBuffer->handle, GRALLOC_EXTRA_SET_IOCTL_ION_SF_INFO, &sf_info);
    }
}

}; // namespace android
