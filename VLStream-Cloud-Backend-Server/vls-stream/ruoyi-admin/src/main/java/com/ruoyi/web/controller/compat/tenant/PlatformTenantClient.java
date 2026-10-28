/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * HTTP . , log.
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
            throw new PlatformTenantCallException(PlatformTenantCallException.Kind.RESPONSE_FORMAT, 200, 200, null);
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
            throw new PlatformTenantCallException(PlatformTenantCallException.Kind.CONFIGURATION, null, null, null);
        }
        if (headers == null) {
            throw new PlatformTenantCallException(PlatformTenantCallException.Kind.CONFIGURATION, null, null, null);
        }
        int timeout = Math.max(tokenProperties.getMultiTenantConnectTimeoutMillis(),
            tokenProperties.getMultiTenantReadTimeoutMillis());
        try (HttpResponse httpResponse = HttpRequest.post(url)
            .header("Content-Type", "application/json")
            .header("requesttype", headers.getRequestType())
            .header("appid", headers.getAppId())
            .header("secretkey", headers.getSecretKey())
            .header("accesstoken", platformToken)
            .body(body.toString())
            .timeout(timeout)
            .execute()) {
            if (!httpResponse.isOk()) {
                throw new PlatformTenantCallException(PlatformTenantCallException.Kind.HTTP_STATUS,
                    httpResponse.getStatus(), null, null);
            }
            JSONObject response;
            Integer code;
            try {
                String responseBody = httpResponse.body();
                if (StringUtils.isBlank(responseBody) || !JSONUtil.isTypeJSONObject(responseBody)) {
                    throw new IllegalArgumentException("Expected a JSON object");
                }
                response = JSONUtil.parseObj(responseBody);
                code = response.getInt("code");
            } catch (Exception exception) {
                throw new PlatformTenantCallException(PlatformTenantCallException.Kind.RESPONSE_FORMAT,
                    httpResponse.getStatus(), null, exception);
            }
            if (code == null || code.intValue() != 200) {
                throw new PlatformTenantCallException(code == null ? PlatformTenantCallException.Kind.RESPONSE_FORMAT
                    : PlatformTenantCallException.Kind.BUSINESS_CODE, httpResponse.getStatus(), code, null);
            }
            return response;
        } catch (PlatformTenantCallException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PlatformTenantCallException(PlatformTenantCallException.Kind.TRANSPORT, null, null, exception);
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
