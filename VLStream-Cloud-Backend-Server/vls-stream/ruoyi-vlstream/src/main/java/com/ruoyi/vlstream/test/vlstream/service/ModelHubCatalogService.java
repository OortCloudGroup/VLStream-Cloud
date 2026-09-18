package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Fixed upstream operations; never expose gateway credentials or accept arbitrary download URLs. */
@Service
public class ModelHubCatalogService {
    private final ModelHubProperties properties;
    private final RestTemplate rest;

    @Autowired
    public ModelHubCatalogService(ModelHubProperties properties) {
        this(properties, new RestTemplate(factory()));
    }

    ModelHubCatalogService(ModelHubProperties properties, RestTemplate rest) {
        this.properties = properties;
        this.rest = rest;
    }

    private static SimpleClientHttpRequestFactory factory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);
        return factory;
    }

    public JSONObject list(int page, int size, String keyword) {
        return list(page, size, keyword, "");
    }

    public JSONObject list(int page, int size, String keyword, String category) {
        return post("modelList", new JSONObject().set("page", Math.max(1, page))
            .set("pagesize", Math.max(1, Math.min(size, 48))).set("keyword", StringUtils.left(StringUtils.trimToEmpty(keyword), 200))
            .set("category", StringUtils.left(StringUtils.trimToEmpty(category), 100)),
            properties.getGuestToken(), null).getJSONObject("data");
    }

    public Object categories() {
        String token = properties.getGuestToken();
        return post("categories", new JSONObject().set("accessToken", token), token, null).get("data");
    }

    public Object files(String uid, String path, String branch) {
        String token = properties.getGuestToken();
        return post("space", query(uid, path, branch).set("accessToken", token), token, null).get("data");
    }

    public void download(String uid, String path, String branch, String token, String tenant, HttpServletResponse output) {
        requireLogin(token);
        if (StringUtils.isBlank(path)) throw new ServiceException("请选择需要下载的文件");
        JSONObject body = query(uid, path, branch).set("accessToken", token);
        HttpHeaders headers = headers(token, tenant);
        try {
            rest.execute(url("downloadFile"), HttpMethod.POST, request -> {
                request.getHeaders().putAll(headers);
                StreamUtils.copy(body.toString(), StandardCharsets.UTF_8, request.getBody());
            }, response -> {
                MediaType type = response.getHeaders().getContentType();
                if (type != null && (type.includes(MediaType.APPLICATION_JSON) || type.getSubtype().endsWith("+json"))) {
                    JSONObject error = JSONUtil.parseObj(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
                    fail(error, false);
                    throw new ServiceException("Model Hub 未返回可下载的文件");
                }
                if (!response.getStatusCode().is2xxSuccessful()) throw new ServiceException("Model Hub 文件下载失败");
                output.setContentType("application/octet-stream");
                output.setHeader("Cache-Control", "no-store");
                output.setHeader("X-Content-Type-Options", "nosniff");
                output.setHeader("Content-Disposition", "attachment; filename=model-file.bin");
                StreamUtils.copy(response.getBody(), output.getOutputStream());
                return null;
            });
        } catch (RestClientResponseException ex) {
            throw upstreamFailure(ex, false);
        } catch (RestClientException ex) {
            throw new ServiceException("Model Hub 下载失败，请检查登录状态或稍后重试");
        }
    }

    private JSONObject query(String uid, String path, String branch) {
        if (StringUtils.isBlank(uid) || uid.length() > 128) throw new ServiceException("模型标识无效");
        if (StringUtils.length(path) > 1024 || StringUtils.length(branch) > 256) throw new ServiceException("文件参数过长");
        return new JSONObject().set("uid", uid).set("path", StringUtils.defaultString(path))
            .set("branch", StringUtils.defaultString(branch));
    }

    private JSONObject post(String operation, JSONObject body, String token, String tenant) {
        try {
            // Write directly: message-converter DEBUG logging must never include the user token in this body.
            JSONObject result = rest.execute(url(operation), HttpMethod.POST, request -> {
                request.getHeaders().putAll(headers(token, tenant));
                StreamUtils.copy(body.toString(), StandardCharsets.UTF_8, request.getBody());
            }, response -> {
                if (!response.getStatusCode().is2xxSuccessful()) throw new ServiceException("Model Hub 服务暂不可用");
                return JSONUtil.parseObj(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
            });
            fail(result, StringUtils.equals(token, properties.getGuestToken()));
            return result;
        } catch (RestClientResponseException ex) {
            throw upstreamFailure(ex, StringUtils.equals(token, properties.getGuestToken()));
        } catch (RestClientException ex) {
            throw new ServiceException(StringUtils.equals(token, properties.getGuestToken())
                ? "Model Hub 浏览请求失败，请稍后重试" : "Model Hub 请求失败，请检查登录状态或稍后重试");
        }
    }

    private void fail(JSONObject result, boolean guest) {
        if (!Integer.valueOf(200).equals(result.getInt("code"))) {
            if (Integer.valueOf(4101).equals(result.getInt("code"))) {
                throw new ServiceException("该历史模型尚未建立 Gitea 文件空间");
            }
            if (Integer.valueOf(4004).equals(result.getInt("code")) || Integer.valueOf(401).equals(result.getInt("code"))) {
                throw new ServiceException(guest ? "Model Hub 游客访问校验失败，请检查游客配置" : "OortCloud 登录已失效，请重新登录");
            }
            throw new ServiceException("Model Hub 操作失败，请核实模型权限或稍后重试");
        }
    }

    private ServiceException upstreamFailure(RestClientResponseException ex, boolean guest) {
        Integer code = null;
        try { code = JSONUtil.parseObj(ex.getResponseBodyAsString()).getInt("code"); }
        catch (RuntimeException ignored) { /* Do not expose raw response bodies or credentials. */ }
        if (Integer.valueOf(4004).equals(code) || Integer.valueOf(401).equals(code)) {
            if (guest) return new ServiceException("Model Hub 游客访问校验失败（" + code + "），请检查游客配置");
            return new ServiceException("Model Hub 服务未接受当前 OortCloud 登录身份（" + code + "），请核对该服务的 SSO 登录环境");
        }
        return new ServiceException("Model Hub 请求失败（HTTP " + ex.getRawStatusCode()
            + (code == null ? "" : "，业务码 " + code) + "）");
    }

    private void requireLogin(String token) {
        if (StringUtils.isBlank(token) || token.equals(properties.getGuestToken())) {
            throw new ServiceException("请先登录 OortCloud 后再查看或下载模型文件");
        }
    }

    private String url(String operation) {
        String base = StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getBaseUrl()), "/");
        if (StringUtils.isAnyBlank(base, properties.getGuestToken(), properties.getAppId(), properties.getSecretKey())) {
            throw new ServiceException("Model Hub 服务尚未配置，请联系管理员");
        }
        return base + "/aiModel/v1/" + operation;
    }

    private HttpHeaders headers(String token, String tenant) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accesstoken", token);
        headers.set("appid", properties.getAppId());
        headers.set("secretkey", properties.getSecretKey());
        headers.set("requesttype", "app");
        if (StringUtils.isNotBlank(tenant)) headers.set("tenantid", tenant);
        return headers;
    }
}
