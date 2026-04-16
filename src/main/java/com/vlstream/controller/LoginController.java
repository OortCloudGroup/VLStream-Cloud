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
 * Login Authentication Controller
 */
@Slf4j
@Api(tags = "Authentication Management")
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private LoginService loginService;

    /**
     * Get tenant ID
     */
    @PostMapping("/getTenantId")
    public ResponseEntity<Map<String, Object>> getTenantId(@RequestBody TenantRequest request) {
        try {
            log.info("Get tenant ID request: {}", request.getPhrase());
            
            String tenantId = loginService.getTenantIdByPhrase(request.getPhrase());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "Success");
            
            Map<String, Object> data = new HashMap<>();
            data.put("tenant_id", tenantId);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get tenant ID", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("msg", "Failed to get tenant ID: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get user tenant list
     */
    @PostMapping("/getTenantList")
    public ResponseEntity<Map<String, Object>> getTenantList(@RequestBody Map<String, Object> request) {
        try {
            log.info("Get user tenant list request");
            
            String accessToken = (String) request.get("accessToken");
            
            if (accessToken == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("msg", "accessToken cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify token and get user information
            LocalUser user = loginService.verifyToken(accessToken);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 4004);
                response.put("msg", "Invalid accessToken");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Call unified user center to get user tenant list
            Map<String, Object> tenantList = loginService.getTenantList(accessToken, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "Success");
            response.put("data", tenantList);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get user tenant list", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("msg", "Failed to get user tenant list: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("User login request");
            
            // Get client IP
            String clientIp = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            LoginResponse loginResponse = loginService.login(request.getUserInfo(), clientIp, userAgent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "Success");
            response.put("data", loginResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("User login failed", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4200);
            response.put("msg", "Login failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Verify token
     */
    @PostMapping("/verifyToken")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestBody VerifyTokenRequest request) {
        try {
            log.info("Verify token request");
            
            LocalUser user = loginService.verifyToken(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            if (user != null) {
                response.put("code", 200);
                response.put("msg", "Success");
                
                // Build return data
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
                response.put("msg", "Invalid accessToken");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Verify token failed", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4004);
            response.put("msg", "Invalid accessToken");
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * User logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody VerifyTokenRequest request) {
        try {
            log.info("User logout request");
            
            boolean success = loginService.logout(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("code", 200);
                response.put("msg", "Success");
            } else {
                response.put("code", 404);
                response.put("msg", "Logout failed");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("User logout failed", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("msg", "Logout failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get user information
     */
    @PostMapping("/getUserInfo")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestBody VerifyTokenRequest request) {
        try {
            log.info("Get user information request");
            
            LocalUser user = loginService.verifyToken(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            if (user != null) {
                response.put("code", 200);
                response.put("msg", "Success");
                
                // Build user information
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
                response.put("msg", "Invalid accessToken");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get user information", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4004);
            response.put("msg", "Invalid accessToken");
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Verify user permission
     */
    @PostMapping("/verifyAuth")
    public ResponseEntity<Map<String, Object>> verifyAuth(@RequestBody Map<String, Object> request) {
        try {
            log.info("Verify user permission request");
            
            String accessToken = (String) request.get("accessToken");
            String serviceName = (String) request.get("service");
            String pauth = (String) request.get("pauth");
            String auth = (String) request.get("auth");
            String doAction = (String) request.get("do");
            
            boolean hasPermission = loginService.verifyUserPermission(accessToken, serviceName, pauth, auth, doAction);
            
            Map<String, Object> response = new HashMap<>();
            if (hasPermission) {
                response.put("code", 200);
                response.put("msg", "Has permission");
                response.put("data", true);
            } else {
                response.put("code", 4001);
                response.put("msg", "No permission");
                response.put("data", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to verify user permission", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 4001);
            response.put("msg", "No permission");
            response.put("data", false);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Health check interface
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("msg", "Login authentication service running normally");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get client IP address
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