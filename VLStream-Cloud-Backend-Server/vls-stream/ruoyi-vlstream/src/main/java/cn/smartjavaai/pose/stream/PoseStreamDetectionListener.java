package cn.smartjavaai.pose.stream;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.Joints;

/** 姿态识别视频流检测回调。 */
public interface PoseStreamDetectionListener {
    void onPoseDetected(Joints[] joints, Image image);
    void onStreamEnded();
    void onStreamDisconnected();
}
