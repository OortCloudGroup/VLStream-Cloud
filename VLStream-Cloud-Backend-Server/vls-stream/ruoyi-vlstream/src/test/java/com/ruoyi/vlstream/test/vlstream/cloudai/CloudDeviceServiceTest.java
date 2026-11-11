package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class CloudDeviceServiceTest {
    private Map<String,Object> binding(String id,String key,String grant,String model,double confidence) {
        Map<String,Object> b=new HashMap<>();b.put("id",id);b.put("camera_key",key);
        b.put("camera_json","{\"grantId\":\""+grant+"\"}");
        b.put("rule_json","{\"modelId\":\""+model+"\",\"sha256\":\"fixed\",\"confidence\":"+confidence+",\"preSeconds\":5,\"postSeconds\":10}");
        return b;
    }
    @Test void aggregatesWeightsAndStreamsButKeepsEachBindingRule() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        Map<String,Object> group=new HashMap<>();group.put("resource_json","{}");group.put("scope","local:single");group.put("revision",4);
        when(jdbc.queryForMap(anyString(),eq("g"))).thenReturn(group);
        when(jdbc.queryForList(anyString(),eq("g"))).thenReturn(Arrays.asList(
            binding("a","camera-a","grant-a","1",0.3),binding("b","camera-a","another-grant-a","2",0.7),binding("c","camera-b","grant-b","1",0.9)));
        JsonNode snapshot=new CloudDeviceService(jdbc,new ObjectMapper(),null,null,null,null,null).aggregate("g");
        assertEquals(2,snapshot.path("cameras").size());assertEquals(2,snapshot.path("models").size());assertEquals(3,snapshot.path("rules").size());
        assertEquals("grant-a",snapshot.path("rules").get(1).path("cameraKey").asText());
        assertEquals(0.9,snapshot.path("rules").get(2).path("confidence").asDouble());
        assertEquals(4,snapshot.path("sceneRevision").asInt());
    }
    @Test void cumulativeCapacityCannotBeBypassedWithSmallBatches() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        Map<String,Object> group=new HashMap<>();group.put("resource_json","{}");group.put("scope","local:single");group.put("revision",1);
        when(jdbc.queryForMap(anyString(),eq("g"))).thenReturn(group);
        List<Map<String,Object>> bindings=new ArrayList<>();for(int i=0;i<33;i++)bindings.add(binding("b"+i,"c"+i,"g"+i,"1",0.5));
        when(jdbc.queryForList(anyString(),eq("g"))).thenReturn(bindings);
        assertThrows(com.ruoyi.common.exception.ServiceException.class,()->new CloudDeviceService(jdbc,new ObjectMapper(),null,null,null,null,null).aggregate("g"));
    }
}
