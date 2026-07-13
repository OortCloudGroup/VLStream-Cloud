package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.compat.BladeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class LocationTaskCompatControllerTest {

    @Test
    void activeSafetyReadEndpointsReturnLegacyPageShapes() {
        LocationTaskCompatController controller = new LocationTaskCompatController();
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("group_type", 2);
        body.put("mod_type", 2);

        BladeResult<Map<String, Object>> eventItems = controller.eventItemList(body);
        BladeResult<Map<String, Object>> events = controller.eventList(body);
        BladeResult<Map<String, Object>> groups = controller.eventGroupList(body);
        BladeResult<Map<String, Object>> workflowConfig = controller.workflowConfigGet(body);

        assertEquals(200, eventItems.getCode());
        assertTrue(((List<?>) eventItems.getData().get("list")).isEmpty());
        assertEquals(0, eventItems.getData().get("count"));
        assertTrue(((List<?>) events.getData().get("list")).isEmpty());
        assertEquals(0, events.getData().get("count"));
        assertTrue(((List<?>) groups.getData().get("list")).isEmpty());
        assertEquals(false, workflowConfig.getData().get("auto_to_work"));
        assertEquals("", workflowConfig.getData().get("process_id"));
    }

    @Test
    void activeSafetyWriteEndpointsReturnSuccess() {
        LocationTaskCompatController controller = new LocationTaskCompatController();

        assertEquals(200, controller.eventGroupSave(new HashMap<String, Object>()).getCode());
        assertEquals(200, controller.eventGroupStatus(new HashMap<String, Object>()).getCode());
        assertEquals(200, controller.workflowConfigSet(new HashMap<String, Object>()).getCode());
        assertEquals(200, controller.eventGroupSettingSave(new HashMap<String, Object>()).getCode());
    }

    @Test
    void exposesLegacyLocationTaskRoutesUsedByActiveSafetyPages() throws Exception {
        assertPostRoute("eventItemList", "/task/v1/event_item_list");
        assertPostRoute("eventList", "/task/v1/event_list");
        assertPostRoute("myEventList", "/task/v2/myevent_list");
        assertPostRoute("eventGroupList", "/task/v2/event_group_list");
        assertPostRoute("eventGroupInfo", "/task/v2/event_group_info");
        assertPostRoute("workflowConfigGet", "/task/v1/workflowConfigGet");
        assertPostRoute("workflowConfigSet", "/task/v1/workflowConfigSet");
        assertPostRoute("eventGroupSave", "/task/v1/event_group_save");
        assertPostRoute("eventGroupDel", "/task/v1/event_group_del");
        assertPostRoute("eventItemSettingSave", "/task/v1/event_item_setting_save");
        assertPostRoute("eventItemStatus", "/task/v1/event_item_status");
        assertPostRoute("eventGroupDeptuserSave", "/task/v1/event_group_deptuser_save");
        assertPostRoute("eventGroupDeptuserList", "/task/v1/event_group_deptuser_list");
        assertPostRoute("eventGroupDeptuserStatus", "/task/v1/event_group_deptuser_status");
        assertPostRoute("eventGroupSettingSave", "/task/v2/event_group_setting_save");
        assertPostRoute("eventGroupStatus", "/task/v2/event_group_status");
        assertPostRoute("eventStatistics", "/task/v1/event_statistics");
    }

    private static void assertPostRoute(String methodName, String path) throws Exception {
        Method method = LocationTaskCompatController.class.getDeclaredMethod(methodName, Map.class);

        assertArrayEquals(new String[] {path}, method.getAnnotation(PostMapping.class).value());
    }
}
