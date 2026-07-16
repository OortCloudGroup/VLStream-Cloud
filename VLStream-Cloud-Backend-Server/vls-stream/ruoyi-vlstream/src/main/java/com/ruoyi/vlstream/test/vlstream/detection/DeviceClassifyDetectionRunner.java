package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 图像分类启动触发器：应用启动时触发一次图像分类任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.classify-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceClassifyDetectionRunner extends AbstractDetectionRunner<DeviceClassifyDetectionManager> {

	public DeviceClassifyDetectionRunner(ObjectProvider<DeviceClassifyDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
