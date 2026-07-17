/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("dev")
class BladeJobCompatControllerTest {

    @Test
    void jobServerSelectReturnsExecutorOptionForJobForms() {
        BladeJobCompatController controller = new BladeJobCompatController();

        BladeResult<List<Map<String, Object>>> result = controller.jobServerSelect();
        Map<String, Object> row = result.getData().get(0);

        assertEquals(200, result.getCode());
        assertFalse(result.getData().isEmpty());
        assertEquals(1, row.get("id"));
        assertEquals("xxl-job-executor", row.get("jobAppName"));
        assertEquals("http://127.0.0.1:18080", row.get("jobServerUrl"));
    }

    @Test
    void jobInfoEndpointsReturnSpringBladeCompatibleSuccessPayloads() {
        BladeJobCompatController controller = new BladeJobCompatController();

        BladeResult<BladePage<Map<String, Object>>> list = controller.jobInfoList(new MockHttpServletRequest());
        BladeResult<Map<String, Object>> detail = controller.jobInfoDetail("job-1");

        assertEquals(200, list.getCode());
        assertEquals(0, list.getData().getTotal());
        assertEquals(200, detail.getCode());
        assertEquals("job-1", detail.getData().get("id"));
        assertEquals(200, controller.jobInfoRemove("job-1").getCode());
        assertEquals(200, controller.jobInfoSubmit(new java.util.LinkedHashMap<String, Object>()).getCode());
        assertEquals(200, controller.jobInfoChange("job-1", 1).getCode());
        assertEquals(200, controller.jobInfoRun("job-1").getCode());
        assertEquals(200, controller.jobInfoSync(new java.util.LinkedHashMap<String, Object>()).getCode());
    }

    @Test
    void exposesRoutesUsedByJobPages() throws Exception {
        assertGetRoute("jobServerSelect", "/blade-job/job-server/select");
        assertGetRoute("jobInfoList", "/blade-job/job-info/list", javax.servlet.http.HttpServletRequest.class);
        assertGetRoute("jobInfoDetail", "/blade-job/job-info/detail", String.class);
        assertPostRoute("jobInfoRemove", "/blade-job/job-info/remove", String.class);
        assertPostRoute("jobInfoSubmit", "/blade-job/job-info/submit", Map.class);
        assertPostRoute("jobInfoChange", "/blade-job/job-info/change", String.class, Integer.class);
        assertPostRoute("jobInfoRun", "/blade-job/job-info/run", String.class);
        assertPostRoute("jobInfoSync", "/blade-job/job-info/sync", Map.class);
    }

    private static void assertGetRoute(String methodName, String path, Class<?>... parameterTypes) throws Exception {
        Method method = BladeJobCompatController.class.getDeclaredMethod(methodName, parameterTypes);

        assertArrayEquals(new String[] {path}, method.getAnnotation(GetMapping.class).value());
    }

    private static void assertPostRoute(String methodName, String path, Class<?>... parameterTypes) throws Exception {
        Method method = BladeJobCompatController.class.getDeclaredMethod(methodName, parameterTypes);

        assertArrayEquals(new String[] {path}, method.getAnnotation(PostMapping.class).value());
    }
}
