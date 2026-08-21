/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("dev")
class LocationTaskCompatServiceTest {

    @Test
    void addsDistinctDeviceIdsInRequestOrder() {
        List<String> where = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("device_filter_active", true);
        body.put("device_ids", Arrays.asList("device-2", "", "device-1", "device-2"));

        LocationTaskResult<?> error = LocationTaskCompatService
            .appendDeviceClassificationFilter(where, args, body);

        assertNull(error);
        assertEquals(Arrays.asList("te.device_id IN (?,?)"), where);
        assertEquals(Arrays.asList("device-2", "device-1"), args);
    }

    @Test
    void activeEmptyClassificationMatchesNoEvents() {
        List<String> where = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("device_filter_active", true);
        body.put("device_ids", new ArrayList<Object>());

        LocationTaskResult<?> error = LocationTaskCompatService
            .appendDeviceClassificationFilter(where, args, body);

        assertNull(error);
        assertEquals(Arrays.asList("1 = 0"), where);
        assertEquals(0, args.size());
    }

    @Test
    void inactiveClassificationDoesNotChangeLegacyQuery() {
        List<String> where = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("device_filter_active", false);

        LocationTaskResult<?> error = LocationTaskCompatService
            .appendDeviceClassificationFilter(where, args, body);

        assertNull(error);
        assertEquals(0, where.size());
        assertEquals(0, args.size());
    }

    @Test
    void rejectsNonArrayDeviceIds() {
        List<String> where = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("device_filter_active", true);
        body.put("device_ids", "device-1");

        LocationTaskResult<?> error = LocationTaskCompatService
            .appendDeviceClassificationFilter(where, args, body);

        assertEquals(4101, error.getCode());
        assertEquals("参数错误 device_ids必须为数组", error.getMsg());
    }
}
