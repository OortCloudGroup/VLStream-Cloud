package org.springblade.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pose estimation startup trigger: Trigger pose estimation task refresh once when application starts.
 */
@Component
@ConditionalOnProperty(value = "vlstream.pose-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DevicePoseDetectionRunner extends AbstractDetectionRunner<DevicePoseDetectionManager> {

	public DevicePoseDetectionRunner(ObjectProvider<DevicePoseDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
