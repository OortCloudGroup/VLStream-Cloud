package cn.smartjavaai.instanceseg.stream;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.entity.DetectionInfo;
import java.util.List;

/** 实例分割视频流检测回调。 */
public interface InstanceSegStreamDetectionListener {
    void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image);
    void onStreamEnded();
    void onStreamDisconnected();
}
