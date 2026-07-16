package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 旋转框（OBB）检测启动触发器：应用启动时触发一次旋转框检测任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.obb-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceObbDetectionRunner extends AbstractDetectionRunner<DeviceObbDetectionManager> {

	public DeviceObbDetectionRunner(ObjectProvider<DeviceObbDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
