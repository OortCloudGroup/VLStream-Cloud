package com.vlstream.controller;

import com.vlstream.dto.LoginRequest;
import com.vlstream.dto.LoginResponse;
import com.vlstream.dto.TenantRequest;
import com.vlstream.dto.VerifyTokenRequest;
import com.vlstream.entity.LocalUser;
import com.vlstream.service.LoginService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录认证控制器
 */
@Slf4j
@Api(tags = "鉴权管理")
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private LoginService loginService;

    /**
     * 获取租户ID
     */
    @PostMapping("/getTenantId")
    public ResponseEntity<Map<String, Object>> getTenantId(@RequestBody TenantRequest request) {
        try {
            log.info("获取租户ID请求: {}", request.getPhrase());
            
            String tenantId = loginService.getTenantIdByPhrase(request.getPhrase());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("tenant_id", tenantId);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取租户ID失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("msg", "获取租户ID失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取用户租户列表
     */
    @PostMapping("/getTenantList")
    public ResponseEntity<Map<String, Object>> getTenantList(@RequestBody Map<String, Object> request) {
        try {
            log.info("获取用户租户列表请求");
            
            String accessToken = (String) request.get("accessToken");
            
            if (accessToken == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("msg", "accessToken不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证token并获取用户信息
            LocalUser user = loginService.verifyToken(accessToken);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 4004);
                response.put("msg", "无效的accessToken");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 调用统一用户中心获取用户租户列表
            Map<String, Object> tenantList = loginService.getTenantList(accessToken, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "成功");
            response.put("data", tenantList);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户租户列表失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("msg", "获取用户租户列表失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("用户登录请求");
            
            // 获取客户端IP
            String clientIp = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            LoginResponse loginResponse = loginService.login(request.getUserInfo(), clientIp, userAgent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "成功");
            response.put("data", loginResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户登录失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4200);
            response.put("msg", "登录失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 验证Token
     */
    @PostMapping("/verifyToken")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestBody VerifyTokenRequest request) {
        try {
            log.info("验证Token请求");
            
            LocalUser user = loginService.verifyToken(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            if (user != null) {
                response.put("code", 200);
                response.put("msg", "成功");
                
                // 构建返回数据
                Map<String, Object> data = new HashMap<>();
                data.put("userId", user.getUserId());
                data.put("tenantId", user.getTenantId());
                data.put("loginId", user.getLoginId());
                data.put("userName", user.getUserName());
                data.put("loginTime", user.getLoginTime());
                data.put("loginIP", user.getLoginIp());
                data.put("login_type", user.getLoginType());
                data.put("client", user.getClient());
                data.put("accessToken", user.getAccessToken());
                
                response.put("data", data);
            } else {
                response.put("code", 4004);
                response.put("msg", "无效的accesstoken");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("验证Token失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4004);
            response.put("msg", "无效的accesstoken");
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 用户退出
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody VerifyTokenRequest request) {
        try {
            log.info("用户退出请求");
            
            boolean success = loginService.logout(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("code", 200);
                response.put("msg", "成功");
            } else {
                response.put("code", 404);
                response.put("msg", "退出失败");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户退出失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("msg", "退出失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取用户信息
     */
    @PostMapping("/getUserInfo")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestBody VerifyTokenRequest request) {
        try {
            log.info("获取用户信息请求");
            
            LocalUser user = loginService.verifyToken(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            if (user != null) {
                response.put("code", 200);
                response.put("msg", "成功");
                
                // 构建用户信息
                Map<String, Object> data = new HashMap<>();
                data.put("user_id", user.getUserId());
                data.put("tenant_id", user.getTenantId());
                data.put("login_id", user.getLoginId());
                data.put("user_name", user.getUserName());
                data.put("user_name_py", user.getUserNamePy());
                data.put("user_name_fpy", user.getUserNameFpy());
                data.put("photo", user.getPhoto());
                data.put("status", user.getStatus());
                data.put("form", user.getForm());
                
                response.put("data", data);
            } else {
                response.put("code", 4004);
                response.put("msg", "无效的accesstoken");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4004);
            response.put("msg", "无效的accesstoken");
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 校验用户权限
     */
    @PostMapping("/verifyAuth")
    public ResponseEntity<Map<String, Object>> verifyAuth(@RequestBody Map<String, Object> request) {
        try {
            log.info("校验用户权限请求");
            
            String accessToken = (String) request.get("accessToken");
            String serviceName = (String) request.get("service");
            String pauth = (String) request.get("pauth");
            String auth = (String) request.get("auth");
            String doAction = (String) request.get("do");
            
            boolean hasPermission = loginService.verifyUserPermission(accessToken, serviceName, pauth, auth, doAction);
            
            Map<String, Object> response = new HashMap<>();
            if (hasPermission) {
                response.put("code", 200);
                response.put("msg", "有权限");
                response.put("data", true);
            } else {
                response.put("code", 4001);
                response.put("msg", "没有权限");
                response.put("data", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("校验用户权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4001);
            response.put("msg", "没有权限");
            response.put("data", false);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("msg", "登录认证服务正常运行");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_CLIENT_IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        
        return clientIp;
    }
} 