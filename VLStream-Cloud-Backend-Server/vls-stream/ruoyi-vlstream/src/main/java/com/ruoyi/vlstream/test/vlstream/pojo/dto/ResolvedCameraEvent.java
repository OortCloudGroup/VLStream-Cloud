/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import cn.hutool.json.JSONObject;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Internal camera report after validation and authoritative WVP ownership lookup. */
@Getter
@AllArgsConstructor
public class ResolvedCameraEvent {
    private final EventManagement event;
    private final DeviceInfo device;
    private final JSONObject payload;
}
