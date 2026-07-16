package com.ruoyi.vlstream.test.vlstream.detection;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 场景治理检测编排器：按摄像头（设备）ID集合触发 detection 包下全部算法的会话刷新/停止。
 */
@Service
@RequiredArgsConstructor
public class DeviceDetectionOrchestrator {

    private final ObjectProvider<DeviceClassifyDetectionManager> classifyDetectionManagerProvider;
    private final ObjectProvider<DeviceObjectDetectionManager> objectDetectionManagerProvider;
    private final ObjectProvider<DevicePersonDetectionManager> personDetectionManagerProvider;
    private final ObjectProvider<DeviceFaceDetectionManager> faceDetectionManagerProvider;
    private final ObjectProvider<DevicePoseDetectionManager> poseDetectionManagerProvider;
    private final ObjectProvider<DeviceSemSegDetectionManager> semSegDetectionManagerProvider;
    private final ObjectProvider<DeviceObbDetectionManager> obbDetectionManagerProvider;
    private final ObjectProvider<DeviceInstanceSegDetectionManager> instanceSegDetectionManagerProvider;

    public void refreshAllForDeviceIds(List<Long> deviceIds) {
        refreshNow(classifyDetectionManagerProvider, deviceIds);
        refreshNow(objectDetectionManagerProvider, deviceIds);
        refreshNow(personDetectionManagerProvider, deviceIds);
        refreshNow(faceDetectionManagerProvider, deviceIds);
        refreshNow(poseDetectionManagerProvider, deviceIds);
        refreshNow(semSegDetectionManagerProvider, deviceIds);
        refreshNow(obbDetectionManagerProvider, deviceIds);
        refreshNow(instanceSegDetectionManagerProvider, deviceIds);
    }

    public void stopAllForDeviceIds(List<Long> deviceIds, String reason) {
        stopNow(classifyDetectionManagerProvider, deviceIds, reason);
        stopNow(objectDetectionManagerProvider, deviceIds, reason);
        stopNow(personDetectionManagerProvider, deviceIds, reason);
        stopNow(faceDetectionManagerProvider, deviceIds, reason);
        stopNow(poseDetectionManagerProvider, deviceIds, reason);
        stopNow(semSegDetectionManagerProvider, deviceIds, reason);
        stopNow(obbDetectionManagerProvider, deviceIds, reason);
        stopNow(instanceSegDetectionManagerProvider, deviceIds, reason);
    }

    private void refreshNow(ObjectProvider<? extends AbstractDeviceDetectionManager<?>> provider, List<Long> deviceIds) {
        AbstractDeviceDetectionManager<?> manager = provider.getIfAvailable();
        if (manager == null) {
            return;
        }
        manager.setExternalControlEnabled(true);
        manager.refreshNowForDeviceIds(deviceIds);
    }

    private void stopNow(ObjectProvider<? extends AbstractDeviceDetectionManager<?>> provider, List<Long> deviceIds, String reason) {
        AbstractDeviceDetectionManager<?> manager = provider.getIfAvailable();
        if (manager == null) {
            return;
        }
        manager.setExternalControlEnabled(true);
        manager.stopNowForDeviceIds(deviceIds, reason);
    }
}
