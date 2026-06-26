package org.springblade.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Image classification startup trigger: Trigger image classification task refresh once when application starts.
 */
@Component
@ConditionalOnProperty(value = "vlstream.classify-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceClassifyDetectionRunner extends AbstractDetectionRunner<DeviceClassifyDetectionManager> {

	public DeviceClassifyDetectionRunner(ObjectProvider<DeviceClassifyDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
