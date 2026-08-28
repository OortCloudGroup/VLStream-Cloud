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
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.entity.ObjectDetInfo;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.pose.model.PoseModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class PoseStreamDetector implements AutoCloseable {

    private final CompatibleStreamDetector delegate;

    private PoseStreamDetector(Builder builder) {
        PoseModel model = Objects.requireNonNull(builder.model, "detectorModel must not be null");
        PoseStreamDetectionListener listener = Objects.requireNonNull(builder.listener, "listener must not be null");
        AtomicReference<Joints[]> latestJoints = new AtomicReference<>();
        delegate = new CompatibleStreamDetector(builder.sourceType, builder.streamUrl, builder.interval,
            image -> {
                R<Joints[]> result = model.detect(image);
                if (result == null || !result.isSuccess() || result.getData() == null
                    || result.getData().length == 0) {
                    return Collections.emptyList();
                }
                latestJoints.set(result.getData());
                DetectionInfo info = new DetectionInfo(
                    new DetectionRectangle(0, 0, image.getWidth(), image.getHeight()),
                    1.0f
                );
                info.setObjectDetInfo(new ObjectDetInfo("pose:" + result.getData().length));
                return Collections.singletonList(info);
            }, new CompatibleStreamDetector.DetectionListener() {
                @Override
                public void onObjectDetected(List<DetectionInfo> values, Image image) {
                    listener.onPoseDetected(latestJoints.getAndSet(null), image);
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
        private PoseModel model;
        private PoseStreamDetectionListener listener;

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

        public Builder detectorModel(PoseModel value) {
            model = value;
            return this;
        }

        public Builder listener(PoseStreamDetectionListener value) {
            listener = value;
            return this;
        }

        public PoseStreamDetector build() {
            return new PoseStreamDetector(this);
        }
    }
}
