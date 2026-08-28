/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.compat.smartjavaai.stream;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.entity.DetectionInfo;

import java.util.List;

public interface ObbDetStreamDetectionListener {
    void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image);

    void onStreamEnded();

    void onStreamDisconnected();
}
