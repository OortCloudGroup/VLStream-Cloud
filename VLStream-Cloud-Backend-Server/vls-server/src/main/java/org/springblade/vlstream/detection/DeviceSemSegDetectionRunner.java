package org.springblade.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Semantic segmentation start trigger: Triggers a semantic segmentation task refresh when the application starts.
 */
@Component
@ConditionalOnProperty(value = "vlstream.semseg-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceSemSegDetectionRunner extends AbstractDetectionRunner<DeviceSemSegDetectionManager> {

	public DeviceSemSegDetectionRunner(ObjectProvider<DeviceSemSegDetectionManager> detectionManagerProvider) {
		super(detectionManagerProvider);
	}
}
