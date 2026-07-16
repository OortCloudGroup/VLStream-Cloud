/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;

/**
 * 检测启动触发器基类：应用启动时触发一次检测任务刷新。
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
