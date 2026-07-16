package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.CameraDisplaySetting;
import com.ruoyi.vlstream.service.IVlsCameraDisplaySettingService;
import com.ruoyi.vlstream.service.VlsMqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Camera display settings with real persistence and MQTT delivery. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsCameraDisplaySetting")
public class VlsCameraDisplaySettingController extends VlsControllerSupport {

    private final IVlsCameraDisplaySettingService settingService;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    /** Return a stored display setting, or the source-compatible default for the device. */
    @GetMapping("/detail")
    public BladeResult<CameraDisplaySetting> detail(@RequestParam(required = false) Long id,
                                                     @RequestParam(required = false) Long deviceId) {
        CameraDisplaySetting setting = find(id, deviceId);
        return BladeResult.success(setting == null ? defaultSetting(deviceId) : setting);
    }

    /** Return a paged list of real display-setting rows. */
    @GetMapping("/list")
    public BladeResult<BladePage<CameraDisplaySetting>> list(@RequestParam(required = false) Long current,
                                                             @RequestParam(required = false) Long size,
                                                             @RequestParam(required = false) Long deviceId) {
        Page<CameraDisplaySetting> page = new Page<CameraDisplaySetting>(current(current), size(size));
        LambdaQueryWrapper<CameraDisplaySetting> query = new LambdaQueryWrapper<CameraDisplaySetting>();
        if (deviceId != null) {
            query.eq(CameraDisplaySetting::getDeviceId, deviceId);
        }
        query.orderByDesc(CameraDisplaySetting::getUpdateTime).orderByDesc(CameraDisplaySetting::getId);
        Page<CameraDisplaySetting> result = settingService.page(page, query);
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Persist a new setting and publish it to the source VLS MQTT topic. */
    @PostMapping("/save")
    public BladeResult<CameraDisplaySetting> save(@RequestBody CameraDisplaySetting setting) {
        return persistAndPublish(setting, false);
    }

    /** Update an existing setting using the source-compatible persistence route. */
    @PostMapping("/update")
    public BladeResult<CameraDisplaySetting> update(@RequestBody CameraDisplaySetting setting) {
        try {
            requireId(setting);
            if (!settingService.updateById(setting)) {
                return BladeResult.fail("Camera display setting update affected no rows");
            }
            return BladeResult.success(settingService.getById(setting.getId()));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Upsert a setting by device and publish the stored payload over MQTT. */
    @PostMapping("/submit")
    public BladeResult<CameraDisplaySetting> submit(@RequestBody CameraDisplaySetting setting) {
        return persistAndPublish(setting, true);
    }

    /** Logically delete display-setting rows by primary key. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(settingService.removeByIds(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Restore and persist the source VLS defaults for one device. */
    @GetMapping("/restoreDefault")
    public BladeResult<CameraDisplaySetting> restoreDefault(@RequestParam Long deviceId) {
        CameraDisplaySetting setting = defaultSetting(deviceId);
        CameraDisplaySetting existing = find(null, deviceId);
        if (existing != null) {
            setting.setId(existing.getId());
        }
        try {
            if (!settingService.saveOrUpdate(setting)) {
                return BladeResult.fail("Failed to restore camera display defaults");
            }
            return BladeResult.success(settingService.getById(setting.getId()));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Store the row, resolving source-style device upserts, then deliver it through MQTT. */
    private BladeResult<CameraDisplaySetting> persistAndPublish(CameraDisplaySetting setting, boolean upsertByDevice) {
        try {
            requireDevice(setting);
            if (upsertByDevice && setting.getId() == null) {
                CameraDisplaySetting existing = find(null, setting.getDeviceId());
                if (existing != null) {
                    setting.setId(existing.getId());
                }
            }
            boolean stored = upsertByDevice ? settingService.saveOrUpdate(setting) : settingService.save(setting);
            if (!stored) {
                return BladeResult.fail("Camera display setting was not persisted");
            }
            CameraDisplaySetting payload = settingService.getById(setting.getId());
            mqttPublishService.publishOrThrow(mqttProperties.getVlsCameraDisplaySettingTopic(), payload);
            return BladeResult.success(payload);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Find a setting using the source route's id/device-id query semantics. */
    private CameraDisplaySetting find(Long id, Long deviceId) {
        if (id == null && deviceId == null) {
            return null;
        }
        LambdaQueryWrapper<CameraDisplaySetting> query = new LambdaQueryWrapper<CameraDisplaySetting>();
        if (id != null) {
            query.eq(CameraDisplaySetting::getId, id);
        }
        if (deviceId != null) {
            query.eq(CameraDisplaySetting::getDeviceId, deviceId);
        }
        return settingService.getOne(query.last("limit 1"));
    }

    /** Validate the required device key. */
    private void requireDevice(CameraDisplaySetting setting) {
        if (setting == null || setting.getDeviceId() == null) {
            throw new IllegalArgumentException("Device primary key ID cannot be empty");
        }
    }

    /** Validate the primary key required by the update route. */
    private void requireId(CameraDisplaySetting setting) {
        if (setting == null || setting.getId() == null) {
            throw new IllegalArgumentException("Camera display setting ID is required");
        }
    }

    /** Build the exact default values exposed by the main-source VLS backend. */
    private CameraDisplaySetting defaultSetting(Long deviceId) {
        CameraDisplaySetting setting = new CameraDisplaySetting();
        setting.setDeviceId(deviceId);
        setting.setScene("Indoor");
        setting.setBrightness(50);
        setting.setContrast(50);
        setting.setSaturation(50);
        setting.setSharpness(50);
        setting.setExposureMode("Automatic");
        setting.setMaxShutterLimit("1/25");
        setting.setMinShutterLimit("1/3000");
        setting.setGainLimit(50);
        setting.setLowLightElectronicShutter("Close");
        setting.setFocusMode("Semi-automatic");
        setting.setMinFocusDistance("1.5m");
        setting.setDayNightSwitch("Automatic");
        setting.setSensitivity(2);
        setting.setAntiFillLightOverExposure("Close");
        setting.setInfraredLampMode("Automatic");
        setting.setBrightnessLimit(50);
        setting.setBacklightCompensation("Close");
        setting.setWideDynamic("Close");
        setting.setStrongLightSuppression("Close");
        setting.setWhiteBalance("Auto white balance");
        setting.setDigitalNoiseReduction("Normal mode");
        setting.setNoiseReductionLevel(50);
        setting.setDefogMode("Close");
        setting.setElectronicStabilization("Close");
        setting.setMirrorMode("Close");
        setting.setPal50hz("Close");
        setting.setLensInitialization("Close");
        setting.setZoomLimit(2);
        return setting;
    }
}
