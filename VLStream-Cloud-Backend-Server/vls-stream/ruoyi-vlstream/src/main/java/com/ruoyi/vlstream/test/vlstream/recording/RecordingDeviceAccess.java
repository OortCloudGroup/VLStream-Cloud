package com.ruoyi.vlstream.test.vlstream.recording;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiService;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.*;

/** Reuses WVP's authorized, tenant-scoped protocol catalog, including offline devices.
 * Shared cross-tenant preview grants are deliberately not a recording entitlement.
 */
@Service
@RequiredArgsConstructor
public class RecordingDeviceAccess {
    private final WvpVlStreamDeviceProperties properties;
    public void require(String protocol, String rowId) {
        String token = StpUtil.getTokenValue();
        if (token == null || token.isEmpty()) throw new ServiceException("当前设备访问会话缺失", 403);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-VLStream-Token", token); headers.set("X-WVP-Auth-Source", "vlstream"); headers.set("tenantid", CloudAiService.tenant());
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            javax.servlet.http.HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            for (String name : new String[]{"accesstoken", "requesttype", "appid", "secretkey"}) {
                String value = request.getHeader(name); if (value != null && !value.isEmpty()) headers.set(name, value);
            }
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        RestTemplate client = new RestTemplate(factory);
        try {
            for (int page = 1; page <= 2000; page++) {
                JsonNode body = client.exchange(properties.getBaseUrl().replaceAll("/+$", "") + "/video/device/list?pageNum=" + page + "&pageSize=500",
                    HttpMethod.GET, new HttpEntity<Void>(headers), JsonNode.class).getBody();
                if (body == null || body.path("code").asInt() != 200 || !body.path("data").path("rows").isArray()) break;
                JsonNode data = body.path("data");
                for (JsonNode device : data.path("rows")) for (JsonNode source : device.path("sources")) {
                    if (protocol.equals(source.path("protocol").asText()) && rowId.equals(source.path("rowId").asText())) return;
                }
                if (data.path("rows").size() == 0 || (long) page * 500 >= data.path("total").asLong()) break;
            }
        } catch (org.springframework.web.client.RestClientException failure) {
            throw new ServiceException("无法核对设备权限，请检查设备服务连接", 503);
        }
        throw new ServiceException("设备不存在或无权访问其录像", 403);
    }
}
