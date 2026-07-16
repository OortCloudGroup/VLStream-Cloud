package cn.smartjavaai.obb.stream;

import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.obb.model.ObbDetModel;
import cn.smartjavaai.stream.CompatibleStreamDetector;

import java.util.Collections;

/** 为官方 OBB 模型补充统一视频流生命周期。 */
public final class ObbDetStreamDetector implements AutoCloseable {
    private final CompatibleStreamDetector delegate;

    private ObbDetStreamDetector(Builder builder) {
        delegate = new CompatibleStreamDetector(builder.sourceType, builder.streamUrl, builder.interval,
            image -> {
                R<DetectionResponse> result = builder.model.detect(image);
                return result != null && result.isSuccess() && result.getData() != null
                    ? result.getData().getDetectionInfoList() : Collections.emptyList();
            }, listener(builder.listener));
    }

    public void startDetection() { delegate.startDetection(); }
    public void stopDetection() { delegate.stopDetection(); }
    public void close() { delegate.close(); }

    private static CompatibleStreamDetector.DetectionListener listener(ObbDetStreamDetectionListener target) {
        return new CompatibleStreamDetector.DetectionListener() {
            public void onObjectDetected(java.util.List<cn.smartjavaai.common.entity.DetectionInfo> values, ai.djl.modality.cv.Image image) { target.onObjectDetected(values, image); }
            public void onStreamEnded() { target.onStreamEnded(); }
            public void onStreamDisconnected() { target.onStreamDisconnected(); }
        };
    }

    public static final class Builder {
        private VideoSourceType sourceType = VideoSourceType.STREAM;
        private String streamUrl;
        private int interval = 1;
        private ObbDetModel model;
        private ObbDetStreamDetectionListener listener;
        public Builder sourceType(VideoSourceType value) { sourceType = value; return this; }
        public Builder streamUrl(String value) { streamUrl = value; return this; }
        public Builder frameDetectionInterval(int value) { interval = value; return this; }
        public Builder detectorModel(ObbDetModel value) { model = value; return this; }
        public Builder listener(ObbDetStreamDetectionListener value) { listener = value; return this; }
        public ObbDetStreamDetector build() { return new ObbDetStreamDetector(this); }
    }
}
