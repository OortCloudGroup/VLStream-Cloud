/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.compat.smartjavaai.stream;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.Joints;

public interface PoseStreamDetectionListener {
    void onPoseDetected(Joints[] joints, Image image);

    void onStreamEnded();

    void onStreamDisconnected();
}
