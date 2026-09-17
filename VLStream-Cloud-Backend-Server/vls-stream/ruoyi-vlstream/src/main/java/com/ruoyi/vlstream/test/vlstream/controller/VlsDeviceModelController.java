package com.ruoyi.vlstream.test.vlstream.controller;

import cn.hutool.json.JSONArray;
import com.ruoyi.vlstream.test.vlstream.service.DeviceModelService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsDeviceModels")
public class VlsDeviceModelController {
    private final DeviceModelService service;

    @GetMapping
    public R<JSONArray> query(@RequestParam String deviceId) {
        return R.data(service.query(deviceId));
    }

    @DeleteMapping
    public R<Void> delete(@RequestParam String deviceId, @RequestParam String modelId) {
        service.delete(deviceId, modelId);
        return R.success("设备模型已删除");
    }
}
