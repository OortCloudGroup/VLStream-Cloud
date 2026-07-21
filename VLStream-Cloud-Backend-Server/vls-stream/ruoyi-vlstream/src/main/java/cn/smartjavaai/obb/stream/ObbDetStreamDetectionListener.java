package cn.smartjavaai.obb.stream;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.entity.DetectionInfo;

import java.util.List;

/** OBB 视频流检测回调。 */
public interface ObbDetStreamDetectionListener {
    void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image);
    void onStreamEnded();
    void onStreamDisconnected();
}
