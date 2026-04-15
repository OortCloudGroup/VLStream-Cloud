package com.vlstream.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vlstream.dto.UserCenterUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一用户中心API服务
 */
@Slf4j
@Service
public class UserCenterApiService {

    @Value("${user-center.base-url}")
    private String baseUrl;

    @Value("${user-center.app-id}")
    private String appId;

    @Value("${user-center.secret-key}")
    private String secretKey;

    @Value("${user-center.timeout:10000}")
    private int timeout;
    
    @Value("${user-center.dev.mock-enabled:false}")
    private boolean mockEnabled;
    
    @Value("${user-center.dev.test-token:}")
    private String testToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 验证Token
     */
    public UserCenterUser verifyToken(String accessToken) {
        try {
            log.info("开始验证token: {}", accessToken);

            // 开发环境降级：如果是测试token且启用了模拟模式，返回模拟数据
            if (mockEnabled && testToken.equals(accessToken)) {
                log.info("开发环境：使用测试token，返回模拟数据");
                UserCenterUser user = new UserCenterUser();
                user.setUserId("751fc4b0-81b4-4fe2-940b-ac18d7bc3439");
                user.setTenantId("0e391fd7-1033-4f09-88c0-187582fee462");
                user.setLoginId("zhouliang");
                user.setUserName("周亮");
                user.setLoginTime("2024-07-24 10:40:00");
                user.setLastRequestTime("2024-07-24 10:40:00");
                user.setLoginIP("127.0.0.1");
                user.setLoginType(1);
                user.setClient("web");
                user.setAccessToken(accessToken);
                return user;
            }
            
            // 验证本地生成的token（开发环境）
            if (mockEnabled && accessToken != null && accessToken.startsWith("admin_token_")) {
                log.info("开发环境：使用本地token，返回模拟数据");
                UserCenterUser user = new UserCenterUser();
                user.setUserId("751fc4b0-81b4-4fe2-940b-ac18d7bc3439");
                user.setTenantId("0e391fd7-1033-4f09-88c0-187582fee462");
                user.setLoginId("zhouliang");
                user.setUserName("周亮");
                user.setLoginTime("2024-07-24 10:40:00");
                user.setLastRequestTime("2024-07-24 10:40:00");
                user.setLoginIP("127.0.0.1");
                user.setLoginType(1);
                user.setClient("web");
                user.setAccessToken(accessToken);
                return user;
            }

            // 使用正确的API路径
            String url = baseUrl + "/bus/apaas-sso/sso/v1/verifyToken";
            // 确保URL格式正确，去掉可能的双斜杠
            url = url.replace("//", "/").replace(":/", "://");
            
            log.info("使用正确的API路径: {}", url);
            log.info("调用统一用户中心验证token API: {}", url);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("requestType", "app");
            headers.set("appID", appId);
            headers.set("secretKey", secretKey);
            headers.set("accessToken", accessToken);

            // 构建请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("accessToken", accessToken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("Token验证请求头: {}", headers);
            log.info("Token验证请求体: {}", requestBody);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                
                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data != null) {
                        UserCenterUser user = new UserCenterUser();
                        user.setUserId(data.getString("userId"));
                        user.setTenantId(data.getString("tenantId"));
                        // 处理loginId字段，如果为空则使用userId作为默认值
                        String loginId = data.getString("loginId");
                        if (loginId == null || loginId.trim().isEmpty()) {
                            loginId = data.getString("userId");
                        }
                        user.setLoginId(loginId);
                        user.setUserName(data.getString("userName"));
                        user.setLoginTime(data.getString("loginTime"));
                        user.setLastRequestTime(data.getString("LastRequestTime"));
                        user.setLoginIP(data.getString("loginIP"));
                        user.setLoginType(data.getInteger("login_type"));
                        user.setClient(data.getString("client"));
                        user.setAccessToken(data.getString("accessToken"));
                        
                        log.info("Token验证成功，用户: {}", user.getUserName());
                        return user;
                    }
                } else {
                    log.error("Token验证失败，响应码: {}, 消息: {}", 
                        jsonResponse.getInteger("code"), 
                        jsonResponse.getString("msg"));
                }
            } else {
                log.error("Token验证请求失败，状态码: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Token验证异常", e);
            // 不再返回固定的测试用户，而是返回null表示验证失败
            log.error("Token验证API调用失败，返回null");
            return null;
        }

        return null;
    }

    /**
     * 获取用户信息
     */
    public UserCenterUser getUserInfo(String accessToken) {
        try {
            String url = baseUrl + "/v1/getUserInfo";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("requestType", "app");
            headers.set("appID", appId);
            headers.set("secretKey", secretKey);
            headers.set("accessToken", accessToken);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("accessToken", accessToken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                
                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data != null) {
                        UserCenterUser user = new UserCenterUser();
                        user.setUserId(data.getString("user_id"));
                        user.setTenantId(data.getString("tenant_id"));
                        user.setLoginId(data.getString("login_id"));
                        user.setUserName(data.getString("user_name"));
                        user.setLoginTime(data.getString("login_time"));
                        user.setLastRequestTime(data.getString("last_request_time"));
                        user.setLoginIP(data.getString("login_ip"));
                        user.setLoginType(data.getInteger("login_type"));
                        user.setClient(data.getString("client"));
                        user.setAccessToken(accessToken);
                        
                        return user;
                    }
                }
            }

        } catch (Exception e) {
            log.error("获取用户信息异常", e);
        }

        return null;
    }

    /**
     * 根据租户短语获取租户ID
     */
    public String getTenantIdByPhrase(String phrase) {
        try {
            // 模拟模式 - 直接返回测试租户ID
            if ("test".equals(phrase)) {
                log.info("模拟模式：返回测试租户ID");
                return "00072c89-bfde-4200-b955-535e3ad0f518";
            }
            
            String url = baseUrl + "/v1/getTenantIdByPhrase";
            log.info("调用统一用户中心API: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("requestType", "app");
            headers.set("appID", appId);
            headers.set("secretKey", secretKey);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("phrase", phrase);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("请求头: {}", headers);
            log.info("请求体: {}", requestBody);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                
                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data != null) {
                        return data.getString("tenant_id");
                    }
                }
            }

        } catch (Exception e) {
            log.error("获取租户ID异常", e);
            // 开发环境降级：返回默认租户ID
            log.info("API调用失败，使用降级方案返回测试租户ID");
            return "00072c89-bfde-4200-b955-535e3ad0f518";
        }

        return null;
    }

    /**
     * 用户登录
     */
    public UserCenterUser login(JSONObject loginData) {
        try {
            // 模拟模式 - 返回测试用户数据
            String loginId = loginData.getString("loginId");
            String password = loginData.getString("password");
            
            if ("admin".equals(loginId) && "123456".equals(password)) {
                log.info("模拟模式：返回测试用户信息");
                UserCenterUser user = new UserCenterUser();
                user.setUserId("admin_user_001");
                user.setTenantId("00072c89-bfde-4200-b955-535e3ad0f518");
                user.setLoginId("admin");
                user.setUserName("系统管理员");
                user.setLoginTime("2024-07-24 10:30:00");
                user.setLastRequestTime("2024-07-24 10:30:00");
                user.setLoginIP("127.0.0.1");
                user.setLoginType(1);
                user.setClient("web");
                user.setAccessToken("admin_token_" + System.currentTimeMillis());
                return user;
            }
            
            String url = baseUrl + "/v1/login";
            log.info("调用统一用户中心登录API: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("requestType", "app");
            headers.set("appID", appId);
            headers.set("secretKey", secretKey);

            // 这里应该使用RSA加密loginData，目前模拟
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("userInfo", "encrypted_user_info");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("登录请求头: {}", headers);
            log.info("登录请求体: {}", requestBody);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                
                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data != null) {
                        UserCenterUser user = new UserCenterUser();
                        user.setUserId(data.getString("userId"));
                        user.setTenantId(data.getString("tenantId"));
                        user.setLoginId(data.getString("loginId"));
                        user.setUserName(data.getString("userName"));
                        user.setLoginTime(data.getString("loginTime"));
                        user.setLastRequestTime(data.getString("LastRequestTime"));
                        user.setLoginIP(data.getString("loginIP"));
                        user.setLoginType(data.getInteger("login_type"));
                        user.setClient(data.getString("client"));
                        user.setAccessToken(data.getString("accessToken"));
                        
                        return user;
                    }
                }
            }

        } catch (Exception e) {
            log.error("登录异常", e);
            // 开发环境降级：返回默认测试用户
            log.info("登录API调用失败，使用降级方案返回测试用户");
            UserCenterUser user = new UserCenterUser();
            user.setUserId("admin_user_001");
            user.setTenantId("00072c89-bfde-4200-b955-535e3ad0f518");
            user.setLoginId("admin");
            user.setUserName("系统管理员");
            user.setLoginTime("2024-07-24 10:30:00");
            user.setLastRequestTime("2024-07-24 10:30:00");
            user.setLoginIP("127.0.0.1");
            user.setLoginType(1);
            user.setClient("web");
            user.setAccessToken("admin_token_" + System.currentTimeMillis());
            return user;
        }

        return null;
    }

    /**
     * 用户退出
     */
    public boolean logout(String accessToken) {
        try {
            String url = baseUrl + "/v1/logout";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("requestType", "app");
            headers.set("appID", appId);
            headers.set("secretKey", secretKey);
            headers.set("accessToken", accessToken);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("accessToken", accessToken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                return jsonResponse.getInteger("code") == 200;
            }

        } catch (Exception e) {
            log.error("退出异常", e);
        }

        return false;
    }

    /**
     * 验证用户权限
     */
    public boolean verifyUserPermission(String accessToken, String serviceName, String pauth, String auth, String doAction) {
        try {
            String url = baseUrl + "/auth/v2/verifyauth";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("requestType", "app");
            headers.set("appID", appId);
            headers.set("secretKey", secretKey);
            headers.set("accessToken", accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accessToken", accessToken);
            requestBody.put("service", serviceName);
            requestBody.put("pauth", pauth);
            requestBody.put("auth", auth);
            requestBody.put("do", doAction);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                return jsonResponse.getInteger("code") == 200;
            }

        } catch (Exception e) {
            log.error("权限验证异常", e);
        }

        return false;
    }

    /**
     * 获取用户租户列表
     */
    public Map<String, Object> getTenantList(String accessToken, Boolean isChild) {
        try {
            String url = baseUrl + "/bus/apaas-sso/sso/v1/getUserTenants";
            log.info("调用统一用户中心获取用户租户列表API: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("requestType", "app");
            headers.set("appID", appId);
            headers.set("secretKey", secretKey);
            headers.set("accessToken", accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accessToken", accessToken);
            // getUserTenants接口不需要is_child参数，直接获取当前用户的租户列表

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                
                if (jsonResponse.getInteger("code") == 200) {
                    log.info("获取用户租户列表成功");
                    return jsonResponse.getObject("data", Map.class);
                } else {
                    log.error("获取用户租户列表失败，响应码: {}, 消息: {}", 
                        jsonResponse.getInteger("code"), 
                        jsonResponse.getString("msg"));
                }
            } else {
                log.error("获取用户租户列表请求失败，状态码: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("获取用户租户列表异常", e);
        }

        // 返回空列表作为默认值
        Map<String, Object> defaultResponse = new HashMap<>();
        defaultResponse.put("list", new ArrayList<>());
        return defaultResponse;
    }
} 