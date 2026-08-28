/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat.smartjavaai.stream;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import cn.smartjavaai.cls.model.ClsModel;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.entity.ObjectDetInfo;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.VideoSourceType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ClsStreamDetector implements AutoCloseable {

    private final CompatibleStreamDetector delegate;

    private ClsStreamDetector(Builder builder) {
        ClsModel model = Objects.requireNonNull(builder.model, "detectorModel must not be null");
        ClsStreamDetectionListener listener = Objects.requireNonNull(builder.listener, "listener must not be null");
        delegate = new CompatibleStreamDetector(builder.sourceType, builder.streamUrl, builder.interval,
            image -> {
                R<Classifications> result = model.detect(image);
                if (result == null || !result.isSuccess() || result.getData() == null
                    || result.getData().best() == null) {
                    return Collections.emptyList();
                }
                Classifications.Classification best = result.getData().best();
                DetectionInfo info = new DetectionInfo(
                    new DetectionRectangle(0, 0, image.getWidth(), image.getHeight()),
                    (float) best.getProbability()
                );
                info.setObjectDetInfo(new ObjectDetInfo(best.getClassName()));
                return Collections.singletonList(info);
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
        private ClsModel model;
        private ClsStreamDetectionListener listener;

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

        public Builder detectorModel(ClsModel value) {
            model = value;
            return this;
        }

        public Builder listener(ClsStreamDetectionListener value) {
            listener = value;
            return this;
        }

        public ClsStreamDetector build() {
            return new ClsStreamDetector(this);
        }
    }
}
