package cn.smartjavaai.cls.stream;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.entity.DetectionInfo;
import java.util.List;

/** 图像分类视频流检测回调。 */
public interface ClsStreamDetectionListener {
    void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image);
    void onStreamEnded();
    void onStreamDisconnected();
}
