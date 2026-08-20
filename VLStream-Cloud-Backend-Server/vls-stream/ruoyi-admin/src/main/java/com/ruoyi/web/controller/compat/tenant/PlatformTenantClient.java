/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat.tenant;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.TokenProperties;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一平台多租户 HTTP 客户端。敏感头仅透传，不写入日志。
 */
@Component
public class PlatformTenantClient {

    private final TokenProperties tokenProperties;

    public PlatformTenantClient(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    public PlatformGatewayHeaders resolveGatewayHeaders(HttpServletRequest request) {
        String requestType = firstHeader(request, "requestType", "requesttype");
        String appId = firstHeader(request, "appId", "appid");
        String secretKey = firstHeader(request, "secretKey", "secretkey");
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("缺少平台网关 appid 或 secretkey");
        }
        return new PlatformGatewayHeaders(StringUtils.isBlank(requestType) ? "app" : requestType, appId, secretKey);
    }

    public PlatformIdentity verifyToken(String platformToken, PlatformGatewayHeaders headers) {
        JSONObject body = new JSONObject().set("accessToken", platformToken);
        JSONObject response = post(tokenProperties.getMultiTenantVerifyTokenAddress(), body, platformToken, headers);
        JSONObject data = dataObject(response);
        PlatformIdentity identity = new PlatformIdentity();
        identity.setUserId(firstValue(data, "userId", "user_id", "userid", "uniqueId"));
        identity.setTenantId(firstValue(data, "tenantId", "tenant_id", "tenantid"));
        identity.setUserName(firstValue(data, "userName", "user_name", "username", "name"));
        identity.setAccessToken(firstValue(data, "accessToken", "access_token", "token"));
        if (StringUtils.isBlank(identity.getAccessToken())) {
            identity.setAccessToken(platformToken);
        }
        if (StringUtils.isBlank(identity.getUserId()) || StringUtils.isBlank(identity.getTenantId())) {
            throw new IllegalStateException("平台 verifyToken 未返回 userId 或 tenantId");
        }
        return identity;
    }

    public List<PlatformTenant> getUserTenants(String platformToken, PlatformGatewayHeaders headers) {
        JSONObject body = new JSONObject()
            .set("accessToken", platformToken)
            .set("isUniqueId", 1);
        JSONObject response = post(tokenProperties.getMultiTenantUserTenantsUrl(), body, platformToken, headers);
        JSONObject data = dataObject(response);
        JSONArray list = data.getJSONArray("list");
        List<PlatformTenant> tenants = new ArrayList<PlatformTenant>();
        if (list == null) {
            return tenants;
        }
        for (Object value : list) {
            JSONObject item = JSONUtil.parseObj(value);
            PlatformTenant tenant = new PlatformTenant();
            tenant.setTenantId(firstValue(item, "tenant_id", "tenantId", "tenantid"));
            tenant.setTenantName(firstValue(item, "tenant_name", "tenantName", "name"));
            tenant.setUserId(firstValue(item, "user_id", "userId", "userid", "unique_id"));
            tenant.setUserName(firstValue(item, "user_name", "userName", "username"));
            tenant.setPhrase(firstValue(item, "phrase", "tenant_phrase"));
            tenant.setStatus(item.getInt("status", 1));
            if (StringUtils.isNotBlank(tenant.getTenantId())) {
                tenants.add(tenant);
            }
        }
        return tenants;
    }

    public String selectTenant(String platformToken, String tenantId, PlatformGatewayHeaders headers) {
        JSONObject body = new JSONObject()
            .set("accessToken", platformToken)
            .set("tenant_id", tenantId);
        JSONObject response = post(tokenProperties.getMultiTenantLoginUrl(), body, platformToken, headers);
        JSONObject data = dataObject(response);
        String selectedToken = firstValue(data, "accessToken", "access_token", "token");
        return StringUtils.isBlank(selectedToken) ? platformToken : selectedToken;
    }

    public String loadDisplayName(String platformToken, String platformUserId, PlatformGatewayHeaders headers) {
        if (StringUtils.isBlank(tokenProperties.getMultiTenantAdminUserUrl())) {
            return null;
        }
        JSONObject body = new JSONObject()
            .set("desensitize", true)
            .set("user_id", platformUserId);
        JSONObject response = post(tokenProperties.getMultiTenantAdminUserUrl(), body, platformToken, headers);
        JSONObject data = dataObject(response);
        String name = firstValue(data, "user_name", "userName", "username", "name", "real_name", "realName");
        JSONObject userInfo = objectValue(data.get("userInfo"));
        return StringUtils.isNotBlank(name) ? name
            : firstValue(userInfo, "user_name", "userName", "username", "name", "real_name", "realName");
    }

    private JSONObject post(String url, JSONObject body, String platformToken, PlatformGatewayHeaders headers) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalStateException("多租户平台接口地址未配置");
        }
        int timeout = Math.max(tokenProperties.getMultiTenantConnectTimeoutMillis(),
            tokenProperties.getMultiTenantReadTimeoutMillis());
        try (HttpResponse httpResponse = HttpRequest.post(url)
            .header("Content-Type", "application/json")
            .header("requestType", headers.getRequestType())
            .header("appid", headers.getAppId())
            .header("secretkey", headers.getSecretKey())
            .header("AccessToken", platformToken)
            .body(body.toString())
            .timeout(timeout)
            .execute()) {
            if (!httpResponse.isOk()) {
                throw new IllegalStateException("平台接口 HTTP 状态异常: " + httpResponse.getStatus());
            }
            JSONObject response = JSONUtil.parseObj(httpResponse.body());
            Integer code = response.getInt("code");
            if (code == null || code.intValue() != 200) {
                String message = firstValue(response, "msg", "message");
                throw new IllegalStateException(StringUtils.isBlank(message) ? "平台接口校验失败" : message);
            }
            return response;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("调用多租户平台失败: " + exception.getMessage(), exception);
        }
    }

    private static JSONObject dataObject(JSONObject response) {
        JSONObject data = objectValue(response == null ? null : response.get("data"));
        return data == null ? new JSONObject() : data;
    }

    private static JSONObject objectValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        try {
            return JSONUtil.parseObj(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstValue(JSONObject object, String... keys) {
        if (object == null) {
            return null;
        }
        for (String key : keys) {
            String value = object.getStr(key);
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
