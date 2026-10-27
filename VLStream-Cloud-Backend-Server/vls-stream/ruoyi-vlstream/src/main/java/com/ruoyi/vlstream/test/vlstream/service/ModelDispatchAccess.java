package com.ruoyi.vlstream.test.vlstream.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Uses the same federated WVP identity and device scope as the device detail page. */
@Service
@RequiredArgsConstructor
public class ModelDispatchAccess {
    private final WvpVlStreamDeviceResolver resolver;
    private final WvpVlStreamDeviceProperties properties;

    public String tenant() {
        String tenant = TenantContextHolder.getTenantId();
        if (StringUtils.isBlank(tenant)) tenant = actor().getTenantId();
        if (StringUtils.isBlank(tenant)) throw new ServiceException("当前会话缺少租户上下文");
        return tenant;
    }

    public String owner() { return actor().getUserId(); }

    protected LoginUser actor() {
        LoginUser user = LoginHelper.getLoginUser();
        if (user == null || StringUtils.isBlank(user.getUserId())) throw new ServiceException("请重新登录");
        return user;
    }

    public DeviceInfo requireDevice(String deviceId, String modelType, boolean strictCapability) {
        actor();
        DeviceInfo device = resolver.resolveOnline(deviceId);
        if (!tenant().equals(device.getTenantId())) throw new ServiceException("设备不属于当前租户或无权访问");
        JSONObject detail = authorizedDetail(device.getId());
        JSONObject current = detail == null ? null : detail.getJSONObject("device");
        if (current == null || !deviceId.equals(current.getStr("deviceId"))
            || !Boolean.TRUE.equals(current.getBool("online"))) {
            throw new ServiceException("设备详情不一致或已离线");
        }
        if (strictCapability) {
            Object raw = current.get("capabilitiesJson");
            cn.hutool.json.JSONArray capabilities;
            try { capabilities = JSONUtil.parseArray(raw == null ? "[]" : raw.toString()); }
            catch (RuntimeException ex) { throw new ServiceException("设备 AI 能力尚未确认"); }
            if (!capabilities.contains("aiInfer")) throw new ServiceException("设备未声明 aiInfer 能力，不能批量部署");
            // This is the only board/runtime combination confirmed in this repository.
            String hardware = StringUtils.defaultString(current.getStr("deviceModel"));
            if (!"Hi3519DV500".equalsIgnoreCase(hardware) || !"om".equals(modelType)) {
                throw new ServiceException("无法确认该设备与模型格式兼容；当前批量支持 Hi3519DV500 的 OM 模型");
            }
        }
        return device;
    }

    protected JSONObject authorizedDetail(Long rowId) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(500, properties.getConnectTimeoutMillis()));
        factory.setReadTimeout(Math.max(500, properties.getReadTimeoutMillis()));
        HttpHeaders headers = new HttpHeaders();
        String token = StpUtil.getTokenValue();
        if (StringUtils.isBlank(token)) throw new ServiceException("当前设备访问会话缺失");
        headers.set("X-VLStream-Token", token);
        headers.set("X-WVP-Auth-Source", "vlstream");
        headers.set("tenantid", tenant());
        org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
            javax.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest();
            for (String name : new String[]{"accesstoken", "requesttype", "appid", "secretkey"}) {
                String value = request.getHeader(name);
                if (StringUtils.isNotBlank(value)) headers.set(name, value);
            }
        }
        // Never forward a client-selected tenant or use service credentials to bypass WVP scope.
        try {
            String body = new RestTemplate(factory).exchange(UriComponentsBuilder
                .fromHttpUrl(StringUtils.removeEnd(properties.getBaseUrl(), "/"))
                .pathSegment("vlstream", "device", String.valueOf(rowId), "detail")
                .build().encode().toUri(), HttpMethod.GET, new HttpEntity<Void>(headers), String.class).getBody();
            JSONObject result = JSONUtil.parseObj(body);
            if (!Integer.valueOf(200).equals(result.getInt("code"))) throw new ServiceException("无权访问该设备详情");
            return result.getJSONObject("data");
        } catch (Exception ex) {
            throw new ServiceException("WVP 设备权限校验未通过，请核对设备访问权限和服务状态");
        }
    }
}
