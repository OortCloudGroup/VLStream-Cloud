/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.compat.BladeResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class LocationTaskCompatController {

    @PostMapping("/task/v1/event_item_list")
    public BladeResult<Map<String, Object>> eventItemList(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(listPayload());
    }

    @PostMapping("/task/v1/event_list")
    public BladeResult<Map<String, Object>> eventList(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(listPayload());
    }

    @PostMapping("/task/v2/myevent_list")
    public BladeResult<Map<String, Object>> myEventList(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(listPayload());
    }

    @PostMapping("/task/v2/event_group_list")
    public BladeResult<Map<String, Object>> eventGroupList(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(listPayload());
    }

    @PostMapping("/task/v2/event_group_info")
    public BladeResult<Map<String, Object>> eventGroupInfo(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(workflowConfigPayload(body));
    }

    @PostMapping("/task/v1/workflowConfigGet")
    public BladeResult<Map<String, Object>> workflowConfigGet(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(workflowConfigPayload(body));
    }

    @PostMapping("/task/v1/workflowConfigSet")
    public BladeResult<Map<String, Object>> workflowConfigSet(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(workflowConfigPayload(body));
    }

    @PostMapping("/task/v1/event_group_save")
    public BladeResult<Map<String, Object>> eventGroupSave(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(ackPayload(body));
    }

    @PostMapping("/task/v1/event_group_del")
    public BladeResult<Map<String, Object>> eventGroupDel(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(ackPayload(body));
    }

    @PostMapping("/task/v1/event_item_setting_save")
    public BladeResult<Map<String, Object>> eventItemSettingSave(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(ackPayload(body));
    }

    @PostMapping("/task/v1/event_item_status")
    public BladeResult<Map<String, Object>> eventItemStatus(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(ackPayload(body));
    }

    @PostMapping("/task/v1/event_group_deptuser_save")
    public BladeResult<Map<String, Object>> eventGroupDeptuserSave(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(ackPayload(body));
    }

    @PostMapping("/task/v1/event_group_deptuser_list")
    public BladeResult<Map<String, Object>> eventGroupDeptuserList(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(listPayload());
    }

    @PostMapping("/task/v1/event_group_deptuser_status")
    public BladeResult<Map<String, Object>> eventGroupDeptuserStatus(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(ackPayload(body));
    }

    @PostMapping("/task/v2/event_group_setting_save")
    public BladeResult<Map<String, Object>> eventGroupSettingSave(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(workflowConfigPayload(body));
    }

    @PostMapping("/task/v2/event_group_status")
    public BladeResult<Map<String, Object>> eventGroupStatus(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(workflowConfigPayload(body));
    }

    @PostMapping("/task/v1/event_statistics")
    public BladeResult<Map<String, Object>> eventStatistics(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("count", 0);
        payload.put("total", 0);
        payload.put("pending", 0);
        payload.put("finished", 0);
        return BladeResult.success(payload);
    }

    private static Map<String, Object> listPayload() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("list", new ArrayList<Object>());
        payload.put("count", 0);
        payload.put("total", 0);
        return payload;
    }

    private static Map<String, Object> workflowConfigPayload(Map<String, Object> body) {
        Map<String, Object> payload = ackPayload(body);
        payload.put("process_id", stringValue(body, "process_id"));
        payload.put("auto_to_work", booleanValue(body, "auto_to_work"));
        return payload;
    }

    private static Map<String, Object> ackPayload(Map<String, Object> body) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("saved", true);
        if (body != null) {
            payload.put("request", new LinkedHashMap<String, Object>(body));
        }
        return payload;
    }

    private static String stringValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key));
    }

    private static Boolean booleanValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return Boolean.FALSE;
        }
        Object value = body.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }
}
