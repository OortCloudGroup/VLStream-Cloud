package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class CloudDeviceCoordinatorTest {
    @Test void liveOrUnknownRunIsNeverDuplicated() {
        for(String state:Arrays.asList("QUEUED","RUNNING","UNKNOWN","STOPPING")) {
            JdbcTemplate jdbc=mock(JdbcTemplate.class);CloudDeviceService service=mock(CloudDeviceService.class);
            Map<String,Object> group=new HashMap<>();group.put("id","group");group.put("scope","localhost:single");group.put("run_id","run");
            when(service.executionScope()).thenReturn("localhost:single");
            when(jdbc.queryForMap(contains("vls_cloud_device_group"),eq("group"))).thenReturn(group);
            when(jdbc.queryForList(contains("vls_cloud_device_run WHERE id"),eq("run"))).thenReturn(Collections.singletonList(Collections.singletonMap("state",state)));
            new CloudDeviceCoordinator(jdbc,service,null).reconcile(group);
            verify(service,never()).aggregate(anyString());
        }
    }
    @Test void otherExecutionModeCannotLaunch() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);CloudDeviceService service=mock(CloudDeviceService.class);
        Map<String,Object> group=new HashMap<>();group.put("id","group");group.put("scope","localhost:multi");
        when(service.executionScope()).thenReturn("localhost:single");
        when(jdbc.queryForMap(contains("vls_cloud_device_group"),eq("group"))).thenReturn(group);
        new CloudDeviceCoordinator(jdbc,service,null).reconcile(group);
        verify(service,never()).aggregate(anyString());
    }
    @Test void stoppedBindingsDoNotRestart() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);CloudDeviceService service=mock(CloudDeviceService.class);
        Map<String,Object> group=new HashMap<>();group.put("id","group");group.put("scope","localhost:single");group.put("run_id","run");
        when(service.executionScope()).thenReturn("localhost:single");
        when(jdbc.queryForMap(contains("vls_cloud_device_group"),eq("group"))).thenReturn(group);
        when(jdbc.queryForList(contains("vls_cloud_device_run WHERE id"),eq("run"))).thenReturn(Collections.singletonList(Collections.singletonMap("state","CANCELLED")));
        when(service.aggregate("group")).thenReturn(new ObjectMapper().createObjectNode().putArray("rules").objectNode());
        new CloudDeviceCoordinator(jdbc,service,null).reconcile(group);
        verify(jdbc,never()).update(startsWith("INSERT INTO vls_cloud_device_run"),any(Object[].class));
    }
}
