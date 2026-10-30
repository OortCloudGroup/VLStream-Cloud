package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeCredentialCipher;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudAiSources {
    private final WvpVlStreamDeviceProperties properties;
    private final ComputeCredentialCipher cipher;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JsonNode bind(String token) {
        JsonNode value = call("resolve", token, null);
        verify(value, CloudAiService.tenant(), CloudAiService.actor());
        String id = value.path("grantId").asText();
        if (!id.matches("[a-f0-9-]{36}")) throw new ServiceException("设备授权返回无效标识");
        ObjectNode safe = safe(value);
        jdbc.update("INSERT IGNORE INTO vls_cloud_ai_grant(id,tenant_id,actor_id,token_cipher,source_json) VALUES(?,?,?,?,?)",
            id, CloudAiService.tenant(), CloudAiService.actor(), cipher.encrypt(token), safe.toString());
        return safe;
    }

    public JsonNode owned(String id) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT source_json FROM vls_cloud_ai_grant WHERE id=? AND tenant_id=? AND actor_id=?",
            id, CloudAiService.tenant(), CloudAiService.actor());
        if (rows.isEmpty()) throw new ServiceException("摄像机未授权给当前用户，请重新选择", 403);
        return read(String.valueOf(rows.get(0).get("source_json")));
    }

    public JsonNode resolve(String id, String tenant, String actor) {
        return resolve(id,tenant,actor,null);
    }
    public JsonNode resolve(String id,String tenant,String actor,String runId) {
        List<String> rows = jdbc.queryForList("SELECT token_cipher FROM vls_cloud_ai_grant WHERE id=? AND tenant_id=? AND actor_id=?", String.class, id, tenant, actor);
        if (rows.isEmpty()) throw new ServiceException("摄像机取流授权不存在", 403);
        JsonNode source = call("resolve", cipher.decrypt(rows.get(0)),runId);
        verify(source, tenant, actor); return source;
    }
    public void release(String id,String tenant,String actor,String runId) {
        List<String> rows=jdbc.queryForList("SELECT token_cipher FROM vls_cloud_ai_grant WHERE id=? AND tenant_id=? AND actor_id=?",String.class,id,tenant,actor);
        if(!rows.isEmpty())call("release",cipher.decrypt(rows.get(0)),runId);
    }

    private static void verify(JsonNode source, String tenant, String actor) {
        if (!tenant.equals(source.path("tenantId").asText()) || !actor.equals(source.path("actorId").asText()))
            throw new ServiceException("摄像机取流授权与当前用户或租户不一致", 403);
    }
    public static ObjectNode safe(JsonNode value) {
        ObjectNode safe = ((ObjectNode) value).deepCopy();
        safe.remove(Arrays.asList("url", "socketUrl", "cameraId", "actorId", "tenantId")); return safe;
    }
    private JsonNode call(String action, String token,String runId) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000); factory.setReadTimeout(runId==null?10000:35000);
            String base = properties.getBaseUrl().replaceAll("/+$", "");
            Map<String,String> request=new LinkedHashMap<>();request.put("token",token);if(runId!=null)request.put("runId",runId);
            JsonNode response = new RestTemplate(factory).postForObject(base + "/video/cloud-ai/grants/" + action,request, JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || !response.path("data").isObject())
                throw new ServiceException(response == null ? "设备服务未返回取流授权" : response.path("msg").asText("取流授权失败"), response == null ? 503 : response.path("code").asInt(503));
            return response.path("data");
        } catch (org.springframework.web.client.RestClientException error) {
            // Exception text can include request credentials or a raw camera URL.
            throw new ServiceException("无法核验后台取流授权，请检查设备服务连接");
        }
    }
    private JsonNode read(String value) { try { return json.readTree(value); } catch (Exception error) { throw new ServiceException("摄像机授权记录损坏"); } }
}
