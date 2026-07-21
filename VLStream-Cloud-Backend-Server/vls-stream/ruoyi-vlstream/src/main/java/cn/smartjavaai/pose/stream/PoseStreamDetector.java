package cn.smartjavaai.pose.stream;

import ai.djl.modality.cv.output.Joints;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.entity.ObjectDetInfo;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.pose.model.PoseModel;
import cn.smartjavaai.stream.CompatibleStreamDetector;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/** 为官方姿态模型补充统一视频流生命周期。 */
public final class PoseStreamDetector implements AutoCloseable {
    private final CompatibleStreamDetector delegate;
    private PoseStreamDetector(Builder builder) {
        AtomicReference<Joints[]> latestJoints = new AtomicReference<>();
        delegate = new CompatibleStreamDetector(builder.sourceType, builder.streamUrl, builder.interval,
            image -> {
                R<Joints[]> result = builder.model.detect(image);
                if (result == null || !result.isSuccess() || result.getData() == null || result.getData().length == 0) return Collections.emptyList();
                latestJoints.set(result.getData());
                DetectionInfo info = new DetectionInfo(new DetectionRectangle(0, 0, image.getWidth(), image.getHeight()), 1.0f);
                info.setObjectDetInfo(new ObjectDetInfo("pose:" + result.getData().length));
                return Collections.singletonList(info);
            }, new CompatibleStreamDetector.DetectionListener() {
                public void onObjectDetected(java.util.List<DetectionInfo> values, ai.djl.modality.cv.Image image) { builder.listener.onPoseDetected(latestJoints.getAndSet(null), image); }
                public void onStreamEnded() { builder.listener.onStreamEnded(); }
                public void onStreamDisconnected() { builder.listener.onStreamDisconnected(); }
            });
    }
    public void startDetection() { delegate.startDetection(); } public void stopDetection() { delegate.stopDetection(); } public void close() { delegate.close(); }
    public static final class Builder {
        private VideoSourceType sourceType = VideoSourceType.STREAM; private String streamUrl; private int interval = 1; private PoseModel model; private PoseStreamDetectionListener listener;
        public Builder sourceType(VideoSourceType v) { sourceType=v; return this; } public Builder streamUrl(String v) { streamUrl=v; return this; }
        public Builder frameDetectionInterval(int v) { interval=v; return this; } public Builder detectorModel(PoseModel v) { model=v; return this; }
        public Builder listener(PoseStreamDetectionListener v) { listener=v; return this; } public PoseStreamDetector build() { return new PoseStreamDetector(this); }
    }
}
