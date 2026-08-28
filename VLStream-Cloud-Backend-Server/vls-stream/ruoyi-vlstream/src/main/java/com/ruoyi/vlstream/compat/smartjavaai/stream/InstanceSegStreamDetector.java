/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat.smartjavaai.stream;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.instanceseg.model.InstanceSegModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class InstanceSegStreamDetector implements AutoCloseable {

    private final CompatibleStreamDetector delegate;

    private InstanceSegStreamDetector(Builder builder) {
        InstanceSegModel model = Objects.requireNonNull(builder.model, "detectorModel must not be null");
        InstanceSegStreamDetectionListener listener = Objects.requireNonNull(builder.listener, "listener must not be null");
        delegate = new CompatibleStreamDetector(builder.sourceType, builder.streamUrl, builder.interval,
            image -> {
                R<DetectionResponse> result = model.detect(image);
                return result != null && result.isSuccess() && result.getData() != null
                    ? result.getData().getDetectionInfoList() : Collections.emptyList();
            }, new CompatibleStreamDetector.DetectionListener() {
                @Override
                public void onObjectDetected(List<DetectionInfo> values, Image image) {
                    listener.onObjectDetected(values, image);
                }

                @Override
                public void onStreamEnded() {
                    listener.onStreamEnded();
                }

                @Override
                public void onStreamDisconnected() {
                    listener.onStreamDisconnected();
                }
            });
    }

    public void startDetection() {
        delegate.startDetection();
    }

    public void stopDetection() {
        delegate.stopDetection();
    }

    @Override
    public void close() {
        delegate.close();
    }

    public static final class Builder {
        private VideoSourceType sourceType = VideoSourceType.STREAM;
        private String streamUrl;
        private int interval = 1;
        private InstanceSegModel model;
        private InstanceSegStreamDetectionListener listener;

        public Builder sourceType(VideoSourceType value) {
            sourceType = value;
            return this;
        }

        public Builder streamUrl(String value) {
            streamUrl = value;
            return this;
        }

        public Builder frameDetectionInterval(int value) {
            interval = value;
            return this;
        }

        public Builder detectorModel(InstanceSegModel value) {
            model = value;
            return this;
        }

        public Builder listener(InstanceSegStreamDetectionListener value) {
            listener = value;
            return this;
        }

        public InstanceSegStreamDetector build() {
            return new InstanceSegStreamDetector(this);
        }
    }
}
