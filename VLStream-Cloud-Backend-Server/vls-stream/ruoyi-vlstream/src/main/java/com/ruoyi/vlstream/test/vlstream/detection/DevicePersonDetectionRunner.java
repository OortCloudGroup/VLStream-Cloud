package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 人体检测启动触发器：应用启动时触发一次人体检测任务刷新。
 */
@Component
@ConditionalOnProperty(value = "vlstream.person-detection.enabled", havingValue = "true", matchIfMissing = true)
public class DevicePersonDetectionRunner extends AbstractDetectionRunner<DevicePersonDetectionManager> {

	public DevicePersonDetectionRunner(ObjectProvider<DevicePersonDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}

}
