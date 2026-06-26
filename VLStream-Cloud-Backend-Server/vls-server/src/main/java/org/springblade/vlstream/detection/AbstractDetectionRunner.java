package org.springblade.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;

/**
 * Detection startup trigger base class: triggers a detection task refresh once when the application starts.
 */
public abstract class AbstractDetectionRunner<M extends AbstractDeviceDetectionManager<?>> implements CommandLineRunner {

    private final ObjectProvider<M> detectionManagerProvider;

    protected AbstractDetectionRunner(ObjectProvider<M> detectionManagerProvider) {
        this.detectionManagerProvider = detectionManagerProvider;
    }

    @Override
    public void run(String... args) {
        M detectionManager = detectionManagerProvider.getIfAvailable();
        if (detectionManager == null) {
            return;
        }
        detectionManager.refreshNow();
    }
}
