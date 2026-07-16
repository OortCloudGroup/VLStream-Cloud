package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.CameraOsdSetting;
import com.ruoyi.vlstream.service.IVlsCameraOsdSettingService;
import com.ruoyi.vlstream.service.VlsMqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Camera OSD settings with real persistence and MQTT delivery. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsCameraOsdSetting")
public class VlsCameraOsdSettingController extends VlsControllerSupport {

    private final IVlsCameraOsdSettingService settingService;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    /** Return a stored OSD setting, or the source-compatible default for the device. */
    @GetMapping("/detail")
    public BladeResult<CameraOsdSetting> detail(@RequestParam(required = false) Long id,
                                                 @RequestParam(required = false) Long deviceId) {
        CameraOsdSetting setting = find(id, deviceId);
        return BladeResult.success(setting == null ? defaultSetting(deviceId) : setting);
    }

    /** Return a paged list of persisted OSD settings. */
    @GetMapping("/list")
    public BladeResult<BladePage<CameraOsdSetting>> list(@RequestParam(required = false) Long current,
                                                         @RequestParam(required = false) Long size,
                                                         @RequestParam(required = false) Long deviceId) {
        Page<CameraOsdSetting> page = new Page<CameraOsdSetting>(current(current), size(size));
        LambdaQueryWrapper<CameraOsdSetting> query = new LambdaQueryWrapper<CameraOsdSetting>();
        if (deviceId != null) {
            query.eq(CameraOsdSetting::getDeviceId, deviceId);
        }
        query.orderByDesc(CameraOsdSetting::getUpdateTime).orderByDesc(CameraOsdSetting::getId);
        Page<CameraOsdSetting> result = settingService.page(page, query);
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Persist a new OSD setting and publish it to the device topic. */
    @PostMapping("/save")
    public BladeResult<CameraOsdSetting> save(@RequestBody CameraOsdSetting setting) {
        return persistAndPublish(setting, false);
    }

    /** Update an existing OSD setting using the source-compatible persistence route. */
    @PostMapping("/update")
    public BladeResult<CameraOsdSetting> update(@RequestBody CameraOsdSetting setting) {
        try {
            if (setting == null || setting.getId() == null) {
                throw new IllegalArgumentException("Camera OSD setting ID is required");
            }
            if (!settingService.updateById(setting)) {
                return BladeResult.fail("Camera OSD setting update affected no rows");
            }
            return BladeResult.success(settingService.getById(setting.getId()));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Upsert an OSD setting by device and publish the stored payload. */
    @PostMapping("/submit")
    public BladeResult<CameraOsdSetting> submit(@RequestBody CameraOsdSetting setting) {
        return persistAndPublish(setting, true);
    }

    /** Logically delete OSD-setting rows by primary key. */
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

    /** Restore and persist the source VLS OSD defaults for one device. */
    @GetMapping("/restoreDefault")
    public BladeResult<CameraOsdSetting> restoreDefault(@RequestParam Long deviceId) {
        CameraOsdSetting setting = defaultSetting(deviceId);
        CameraOsdSetting existing = find(null, deviceId);
        if (existing != null) {
            setting.setId(existing.getId());
        }
        try {
            if (!settingService.saveOrUpdate(setting)) {
                return BladeResult.fail("Failed to restore camera OSD defaults");
            }
            return BladeResult.success(settingService.getById(setting.getId()));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Store the row, resolving source-style device upserts, then deliver it through MQTT. */
    private BladeResult<CameraOsdSetting> persistAndPublish(CameraOsdSetting setting, boolean upsertByDevice) {
        try {
            if (setting == null || setting.getDeviceId() == null) {
                throw new IllegalArgumentException("Device primary key ID cannot be empty");
            }
            if (upsertByDevice && setting.getId() == null) {
                CameraOsdSetting existing = find(null, setting.getDeviceId());
                if (existing != null) {
                    setting.setId(existing.getId());
                }
            }
            boolean stored = upsertByDevice ? settingService.saveOrUpdate(setting) : settingService.save(setting);
            if (!stored) {
                return BladeResult.fail("Camera OSD setting was not persisted");
            }
            CameraOsdSetting payload = settingService.getById(setting.getId());
            mqttPublishService.publishOrThrow(mqttProperties.getVlsCameraOsdSettingTopic(), payload);
            return BladeResult.success(payload);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Find one OSD setting using the source route's id/device-id semantics. */
    private CameraOsdSetting find(Long id, Long deviceId) {
        if (id == null && deviceId == null) {
            return null;
        }
        LambdaQueryWrapper<CameraOsdSetting> query = new LambdaQueryWrapper<CameraOsdSetting>();
        if (id != null) {
            query.eq(CameraOsdSetting::getId, id);
        }
        if (deviceId != null) {
            query.eq(CameraOsdSetting::getDeviceId, deviceId);
        }
        return settingService.getOne(query.last("limit 1"));
    }

    /** Build the exact OSD defaults exposed by the main-source VLS backend. */
    private CameraOsdSetting defaultSetting(Long deviceId) {
        CameraOsdSetting setting = new CameraOsdSetting();
        setting.setDeviceId(deviceId);
        setting.setShowName(1);
        setting.setShowDate(1);
        setting.setShowWeek(1);
        setting.setChannelName("IPdpme");
        setting.setTimeFormat("24-hour format");
        setting.setDateFormat("XXXX-XX-XX");
        setting.setOverlay1Enabled(1);
        setting.setOverlay1Text("");
        setting.setOverlay2Enabled(1);
        setting.setOverlay2Text("");
        setting.setOverlay3Enabled(1);
        setting.setOverlay3Text("");
        setting.setOsdProperty("Opaque, non-flashing");
        setting.setOsdFont("Adaptive");
        setting.setOsdColor("Black & White Auto");
        setting.setAlignMode("Adaptive");
        return setting;
    }
}
