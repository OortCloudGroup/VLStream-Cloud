package com.vlstream.controller;

import com.vlstream.dto.UserSyncRequest;
import com.vlstream.entity.LocalUser;
import com.vlstream.service.UserSyncService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户同步控制器
 */
@Slf4j
@Api(tags = "用户同步控制器")
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserSyncController {

    @Autowired
    private UserSyncService userSyncService;

    /**
     * 同步用户信息到本地
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser(@RequestBody UserSyncRequest request) {
        try {
            log.info("收到用户同步请求: {}", request.getUserId());
            
            LocalUser localUser = userSyncService.syncUserToLocal(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "用户同步成功");
            response.put("data", localUser);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户同步失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "用户同步失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据用户ID获取本地用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable String userId) {
        try {
            log.info("获取用户信息: {}", userId);
            
            LocalUser localUser = userSyncService.getLocalUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            if (localUser != null) {
                response.put("code", 200);
                response.put("message", "获取用户信息成功");
                response.put("data", localUser);
            } else {
                response.put("code", 404);
                response.put("message", "用户不存在");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取用户信息失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 更新本地用户信息
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String userId, @RequestBody LocalUser userData) {
        try {
            log.info("更新用户信息: {}", userId);
            
            LocalUser localUser = userSyncService.updateLocalUser(userId, userData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "更新用户信息成功");
            response.put("data", localUser);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "更新用户信息失败: " + e.getMessage());
            
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
        response.put("message", "用户同步服务正常运行");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
} 