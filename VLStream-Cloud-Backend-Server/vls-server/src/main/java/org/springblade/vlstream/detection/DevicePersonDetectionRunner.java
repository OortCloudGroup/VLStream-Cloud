package org.springblade.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Human body detection startup trigger: triggers a human body detection task refresh once when the application starts.
 */
@Component
@ConditionalOnProperty(value = "vlstream.person-detection.enabled", havingValue = "true", matchIfMissing = true)
public class DevicePersonDetectionRunner extends AbstractDetectionRunner<DevicePersonDetectionManager> {

	public DevicePersonDetectionRunner(ObjectProvider<DevicePersonDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}

}
