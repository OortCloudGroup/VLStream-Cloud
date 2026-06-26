package org.springblade.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Rotated bounding box (OBB) detection startup trigger: Triggers a refresh of rotated bounding box detection tasks once when the application starts.
 */
@Component
@ConditionalOnProperty(value = "vlstream.obb-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceObbDetectionRunner extends AbstractDetectionRunner<DeviceObbDetectionManager> {

	public DeviceObbDetectionRunner(ObjectProvider<DeviceObbDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
