package com.ruoyi.vlstream.test.vlstream.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiService;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/** The same user-scoped WVP directory that supplies the video square. */
@Service
@RequiredArgsConstructor
public class AuthorizedVideoCatalog {
    private final WvpVlStreamDeviceProperties properties;
    public static final int PAGE_SIZE = 500;

    public JsonNode page(int page) {
        String token = StpUtil.getTokenValue();
        if (StringUtils.isBlank(token)) throw new ServiceException("当前设备访问会话缺失", 403);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-VLStream-Token", token);
        headers.set("X-WVP-Auth-Source", "vlstream");
        headers.set("tenantid", CloudAiService.tenant());
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            for (String name : new String[]{"accesstoken", "requesttype", "appid", "secretkey"}) {
                String value = request.getHeader(name);
                if (StringUtils.isNotBlank(value)) headers.set(name, value);
            }
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        String url = StringUtils.stripEnd(properties.getBaseUrl(), "/") + "/video/device/list?pageNum=" + page + "&pageSize=" + PAGE_SIZE;
        try {
            JsonNode body = new RestTemplate(factory).exchange(url, HttpMethod.GET, new HttpEntity<Void>(headers), JsonNode.class).getBody();
            if (body != null && body.path("code").asInt() == 200) {
                JsonNode data = body.path("data");
                if (data.path("rows").isArray() && data.path("total").asLong(-1) >= 0) return data;
            }
        } catch (RestClientException ignored) {
            // Do not return WVP's raw response or gateway authentication details to the browser.
        }
        throw new ServiceException("无法核对设备权限，请检查设备服务连接", 503);
    }
}
