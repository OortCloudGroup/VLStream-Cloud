package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owner-only configuration; secret input is never returned, logged or validated by a live call. */
@Service
@RequiredArgsConstructor
public class ComputeProviderSettingsService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    private final ComputeCredentialCipher cipher;
    private final ObjectMapper json;

    @Transactional(rollbackFor = Exception.class)
    public void save(Long id, ComputeRequests.ProviderSettings request) {
        ComputeNode before = nodes.lockServer(id);
        nodes.requireManagement(before);
        if (!ComputeNodeService.tenant().equals(before.getTenantId())) throw new ServiceException("无权配置平台算力厂商接口");
        boolean api = "API".equals(request.getControlMode());
        if (api && !("autodl".equals(before.getProvider()) && "AUTODL_PRO".equals(request.getProviderProduct())
            || "aliyun".equals(before.getProvider()) && "ECS".equals(request.getProviderProduct())))
            throw new ServiceException("普通 AutoDL 或自有服务器不支持此云管理接口");
        String instance = trim(request.getProviderInstanceId());
        String region = trim(request.getProviderRegion());
        if (api && (instance == null || !instance.matches("[A-Za-z0-9_-]{1,100}"))) throw new ServiceException("请填写厂商实例 ID");
        if (api && "ECS".equals(request.getProviderProduct()) && (region == null || !region.matches("[a-z0-9-]{1,100}")
            || !("KeepCharging".equals(request.getRequestedStoppedMode()) || "StopCharging".equals(request.getRequestedStoppedMode()))))
            throw new ServiceException("ECS 必须明确配置 RegionId 和停机计费模式");
        boolean changedIdentity = !Objects.equals(instance, before.getProviderInstanceId())
            || !Objects.equals(region, before.getProviderRegion()) || !Objects.equals(request.getProviderProduct(), before.getProviderProduct());
        // A registered public key is immutable across every configuration entry point.
        // Replacing credentials cannot authorize rebinding a different vendor instance.
        if (before.getPublicInstanceKey() != null && changedIdentity)
            throw new ServiceException("公共实例身份已绑定，不能通过厂商配置修改实例ID、产品或区域；请保留已登记身份");
        // A vendor ID groups control aliases even in MANUAL mode. Authorize every locked target
        // before checking its activity, encrypting credentials or changing the source binding.
        if (instance != null) {
            List<ComputeNode> claimed = jdbc.query("SELECT * FROM vls_compute_node WHERE provider=? AND provider_instance_id=? ORDER BY id FOR UPDATE",
                new org.springframework.jdbc.core.BeanPropertyRowMapper<>(ComputeNode.class), before.getProvider(), instance);
            for (ComputeNode node : claimed) {
                if (!before.getTenantId().equals(node.getTenantId())) throw new ServiceException("厂商实例已归其他租户管理");
                nodes.requireManagement(node);
                if (node.getPublicInstanceKey()!=null && !Objects.equals(before.getPublicInstanceKey(),node.getPublicInstanceKey()))
                    throw new ServiceException("目标已登记为另一公共实例，不能通过不同身份或私有配置控制其厂商实例");
            }
            for (ComputeNode node : claimed) if (!Objects.equals(node.getId(), id)) nodes.assertIdle(node.getId());
        }
        nodes.assertIdle(id);
        String encrypted = request.isClearCredentials() ? null : before.getProviderCredentialCipher();
        if (changedIdentity) encrypted = null;
        Map<String,String> secret = new LinkedHashMap<>();
        if (trim(request.getToken()) != null) secret.put("token", checkedSecret(request.getToken()));
        if (trim(request.getAccessKeyId()) != null || trim(request.getAccessKeySecret()) != null) {
            if (trim(request.getAccessKeyId()) == null || trim(request.getAccessKeySecret()) == null) throw new ServiceException("AccessKey ID 与 Secret 必须一起更新");
            secret.put("accessKeyId", checkedSecret(request.getAccessKeyId())); secret.put("accessKeySecret", checkedSecret(request.getAccessKeySecret()));
        }
        if (!secret.isEmpty()) {
            if ("AUTODL_PRO".equals(request.getProviderProduct()) != secret.containsKey("token") || secret.size() != (secret.containsKey("token") ? 1 : 2))
                throw new ServiceException("凭据类型与厂商产品不匹配");
            try { encrypted = cipher.encrypt(json.writeValueAsString(secret)); }
            catch (Exception e) { throw new ServiceException("厂商凭据保存失败，请核对后端持久化密钥"); }
        }
        if (api && encrypted == null) throw new ServiceException("启用厂商 API 必须配置后端加密凭据");
        if (!api && request.isClearCredentials()) encrypted = null;
        jdbc.update("UPDATE vls_compute_node SET control_mode=?,provider_product=?,provider_instance_id=?,provider_region=?,requested_stopped_mode=?,provider_credential_cipher=?,provider_gpu_amount=NULL,provider_locked=NULL,actual_stopped_mode=NULL,provider_charge_type=NULL,capacity_state='UNKNOWN',capacity_checked_at=NULL,probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE tenant_id=? AND id=?",
            request.getControlMode(), request.getProviderProduct(), instance, region, request.getRequestedStoppedMode(), encrypted, ComputeNodeService.tenant(), id);
    }
    private static String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private static String checkedSecret(String value) {
        String secret = value.trim();
        if (secret.contains("\r") || secret.contains("\n") || secret.indexOf('\0') >= 0) throw new ServiceException("凭据包含非法字符");
        return secret;
    }
}
