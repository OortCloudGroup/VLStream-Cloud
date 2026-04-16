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
 * User Sync Controller
 */
@Slf4j
@Api(tags = "User Sync Controller")
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserSyncController {

    @Autowired
    private UserSyncService userSyncService;

    /**
     * Sync user information to local
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser(@RequestBody UserSyncRequest request) {
        try {
            log.info("Received user sync request: {}", request.getUserId());
            
            LocalUser localUser = userSyncService.syncUserToLocal(request.getAccessToken());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "User sync successful");
            response.put("data", localUser);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("User sync failed", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "User sync failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get local user information by user ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable String userId) {
        try {
            log.info("Get user information: {}", userId);
            
            LocalUser localUser = userSyncService.getLocalUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            if (localUser != null) {
                response.put("code", 200);
                response.put("message", "Get user information successful");
                response.put("data", localUser);
            } else {
                response.put("code", 404);
                response.put("message", "User does not exist");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get user information", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Failed to get user information: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Update local user information
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String userId, @RequestBody LocalUser userData) {
        try {
            log.info("Update user information: {}", userId);
            
            LocalUser localUser = userSyncService.updateLocalUser(userId, userData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Update user information successful");
            response.put("data", localUser);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to update user information", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Failed to update user information: " + e.getMessage());
            
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
        response.put("message", "User sync service running normally");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
} 