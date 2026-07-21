package cn.smartjavaai.cls.stream;

import ai.djl.modality.Classifications;
import cn.smartjavaai.cls.model.ClsModel;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.entity.ObjectDetInfo;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.stream.CompatibleStreamDetector;
import java.util.Collections;

/** 为官方分类模型补充统一视频流生命周期。 */
public final class ClsStreamDetector implements AutoCloseable {
    private final CompatibleStreamDetector delegate;
    private ClsStreamDetector(Builder builder) {
        delegate = new CompatibleStreamDetector(builder.sourceType, builder.streamUrl, builder.interval,
            image -> {
                R<Classifications> result = builder.model.detect(image);
                if (result == null || !result.isSuccess() || result.getData() == null || result.getData().best() == null) return Collections.emptyList();
                Classifications.Classification best = result.getData().best();
                DetectionInfo info = new DetectionInfo(new DetectionRectangle(0, 0, image.getWidth(), image.getHeight()), (float) best.getProbability());
                info.setObjectDetInfo(new ObjectDetInfo(best.getClassName()));
                return Collections.singletonList(info);
            }, new CompatibleStreamDetector.DetectionListener() {
                public void onObjectDetected(java.util.List<DetectionInfo> values, ai.djl.modality.cv.Image image) { builder.listener.onObjectDetected(values, image); }
                public void onStreamEnded() { builder.listener.onStreamEnded(); }
                public void onStreamDisconnected() { builder.listener.onStreamDisconnected(); }
            });
    }
    public void startDetection() { delegate.startDetection(); } public void stopDetection() { delegate.stopDetection(); } public void close() { delegate.close(); }
    public static final class Builder {
        private VideoSourceType sourceType = VideoSourceType.STREAM; private String streamUrl; private int interval = 1; private ClsModel model; private ClsStreamDetectionListener listener;
        public Builder sourceType(VideoSourceType v) { sourceType=v; return this; } public Builder streamUrl(String v) { streamUrl=v; return this; }
        public Builder frameDetectionInterval(int v) { interval=v; return this; } public Builder detectorModel(ClsModel v) { model=v; return this; }
        public Builder listener(ClsStreamDetectionListener v) { listener=v; return this; } public ClsStreamDetector build() { return new ClsStreamDetector(this); }
    }
}
