package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiService;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LlmReviewScopes {
    private final JdbcTemplate jdbc;
    private final WvpVlStreamDeviceResolver devices;
    private final CloudAiService scenes;

    public void validateTarget(String type, String id) {
        if (id == null || id.isEmpty() || id.length() > 100) throw new ServiceException("复核对象编号无效");
        if ("SCENE".equals(type)) {
            scenes.scene(id);
        } else if ("DEVICE".equals(type)) {
            if (!CloudAiService.tenant().equals(devices.resolveEventDevice(id).getTenantId()))
                throw new ServiceException("设备不存在或无权访问", 403);
        } else if (Arrays.asList("REGION", "GROUP", "TAG").contains(type) && id.matches("[1-9][0-9]{0,18}")) {
            devices.requireCategory(type, id);
        } else {
            throw new ServiceException("复核对象类型或编号无效");
        }
    }

    public boolean enabled(String tenant, String key) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM vls_llm_review_scope WHERE tenant_id=? AND scope_key=?)",
            Boolean.class, tenant, key));
    }

    public void setEnabled(String key, boolean enabled) {
        String tenant = CloudAiService.tenant();
        if (enabled) {
            jdbc.update("INSERT INTO vls_llm_review_scope(tenant_id,scope_key) VALUES(?,?) ON DUPLICATE KEY UPDATE scope_key=VALUES(scope_key)", tenant, key);
        } else {
            jdbc.update("DELETE FROM vls_llm_review_scope WHERE tenant_id=? AND scope_key=?", tenant, key);
        }
    }

    public boolean matchesDevice(DeviceInfo device) {
        List<String> keys = new ArrayList<>();
        keys.add("DEVICE:" + device.getDeviceId());
        if (device.getCategoryKeys() != null) {
            keys.addAll(device.getCategoryKeys());
        } else if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM vls_llm_review_scope WHERE tenant_id=?"
                + " AND (scope_key LIKE 'REGION:%' OR scope_key LIKE 'GROUP:%' OR scope_key LIKE 'TAG:%'))", Boolean.class, device.getTenantId()))) {
            throw new ServiceException("WVP 未返回设备分类，无法判断已启用的分类复核规则");
        }
        Map<String,Object> params = new HashMap<>();
        params.put("tenant", device.getTenantId());
        params.put("keys", keys);
        return Boolean.TRUE.equals(new NamedParameterJdbcTemplate(jdbc).queryForObject(
            "SELECT EXISTS(SELECT 1 FROM vls_llm_review_scope WHERE tenant_id=:tenant AND scope_key IN (:keys))", params, Boolean.class));
    }
}
