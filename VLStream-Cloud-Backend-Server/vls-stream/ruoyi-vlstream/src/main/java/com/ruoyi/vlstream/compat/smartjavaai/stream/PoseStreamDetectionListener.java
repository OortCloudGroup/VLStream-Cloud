/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat.smartjavaai.stream;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.Joints;

public interface PoseStreamDetectionListener {
    void onPoseDetected(Joints[] joints, Image image);

    void onStreamEnded();

    void onStreamDisconnected();
}
