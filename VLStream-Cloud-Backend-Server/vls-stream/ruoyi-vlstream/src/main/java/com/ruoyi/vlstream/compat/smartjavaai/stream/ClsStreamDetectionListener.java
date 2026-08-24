/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat.smartjavaai.stream;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.entity.DetectionInfo;

import java.util.List;

public interface ClsStreamDetectionListener {
    void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image);

    void onStreamEnded();

    void onStreamDisconnected();
}
