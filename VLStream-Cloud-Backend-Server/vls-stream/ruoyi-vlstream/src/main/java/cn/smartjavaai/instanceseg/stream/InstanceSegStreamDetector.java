package cn.smartjavaai.instanceseg.stream;

import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.instanceseg.model.InstanceSegModel;
import cn.smartjavaai.stream.CompatibleStreamDetector;
import java.util.Collections;

/** 为官方实例分割模型补充统一视频流生命周期。 */
public final class InstanceSegStreamDetector implements AutoCloseable {
    private final CompatibleStreamDetector delegate;
    private InstanceSegStreamDetector(Builder builder) {
        delegate = new CompatibleStreamDetector(builder.sourceType, builder.streamUrl, builder.interval,
            image -> {
                R<DetectionResponse> result = builder.model.detect(image);
                return result != null && result.isSuccess() && result.getData() != null
                    ? result.getData().getDetectionInfoList() : Collections.emptyList();
            }, new CompatibleStreamDetector.DetectionListener() {
                public void onObjectDetected(java.util.List<cn.smartjavaai.common.entity.DetectionInfo> values, ai.djl.modality.cv.Image image) { builder.listener.onObjectDetected(values, image); }
                public void onStreamEnded() { builder.listener.onStreamEnded(); }
                public void onStreamDisconnected() { builder.listener.onStreamDisconnected(); }
            });
    }
    public void startDetection() { delegate.startDetection(); }
    public void stopDetection() { delegate.stopDetection(); }
    public void close() { delegate.close(); }
    public static final class Builder {
        private VideoSourceType sourceType = VideoSourceType.STREAM; private String streamUrl; private int interval = 1;
        private InstanceSegModel model; private InstanceSegStreamDetectionListener listener;
        public Builder sourceType(VideoSourceType v) { sourceType=v; return this; } public Builder streamUrl(String v) { streamUrl=v; return this; }
        public Builder frameDetectionInterval(int v) { interval=v; return this; } public Builder detectorModel(InstanceSegModel v) { model=v; return this; }
        public Builder listener(InstanceSegStreamDetectionListener v) { listener=v; return this; } public InstanceSegStreamDetector build() { return new InstanceSegStreamDetector(this); }
    }
}
