package com.vlstream.controller;

import com.vlstream.common.Result;
import com.vlstream.entity.StreamRequest;
import com.vlstream.entity.StopStreamRequest;
import com.vlstream.service.WebRTCService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebRTC Stream Controller
 * Provides API interfaces for WebRTC stream management
 */
@Api(tags = "WebRTC Stream Controller")
@RestController
@RequestMapping("/api/webrtc")
@CrossOrigin(origins = "*")
public class WebRTCController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebRTCController.class);
    
    @Autowired
    private WebRTCService webrtcService;
    
    /**
     * Get WebRTC service configuration
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getWebRTCConfig() {
        try {
            Map<String, Object> config = webrtcService.getServerStatus();
            logger.debug("Get WebRTC configuration successful: {}", config);
            return Result.success(config);
        } catch (Exception e) {
            logger.error("Failed to get WebRTC configuration: {}", e.getMessage(), e);
            return Result.error("Failed to get WebRTC configuration: " + e.getMessage());
        }
    }
    
    /**
     * Check WebRTC service status
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getWebRTCStatus() {
        try {
            Map<String, Object> status = webrtcService.getServerStatus();
            logger.debug("Get WebRTC status successful: {}", status);
            return Result.success(status);
        } catch (Exception e) {
            logger.error("Failed to get WebRTC status: {}", e.getMessage(), e);
            return Result.error("Failed to get WebRTC status: " + e.getMessage());
        }
    }
    
    /**
     * Start WebRTC stream
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> startWebRTCStream(@RequestBody StreamRequest request) {
        logger.info("Received start WebRTC stream request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL cannot be empty");
            }
            
            if (!webrtcService.isValidRtspUrl(request.getRtspUrl())) {
                return Result.error("Invalid RTSP URL");
            }
            
            // Start WebRTC stream
            String streamId = webrtcService.startWebRTCStream(
                request.getDeviceId(), 
                request.getRtspUrl()
            );
            
            // Get stream information
            Map<String, Object> streamInfo = webrtcService.getStreamInfo(request.getDeviceId());
            
            logger.info("WebRTC stream started successfully: deviceId={}, streamId={}", 
                       request.getDeviceId(), streamId);
            return Result.success(streamInfo);
            
        } catch (Exception e) {
            logger.error("Failed to start WebRTC stream: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to start WebRTC stream: " + e.getMessage());
        }
    }
    
    /**
     * Stop WebRTC stream
     */
    @PostMapping("/stop")
    public Result<Void> stopWebRTCStream(@RequestBody StopStreamRequest request) {
        logger.info("Received stop WebRTC stream request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            // Stop WebRTC stream
            webrtcService.stopWebRTCStream(request.getDeviceId());
            
            logger.info("WebRTC stream stopped successfully: deviceId={}", request.getDeviceId());
            return Result.success();
            
        } catch (Exception e) {
            logger.error("Failed to stop WebRTC stream: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to stop WebRTC stream: " + e.getMessage());
        }
    }
    
    /**
     * Get active WebRTC streams
     */
    @GetMapping("/active")
    public Result<Map<String, String>> getActiveStreams() {
        try {
            Map<String, String> activeStreams = webrtcService.getActiveStreams();
            logger.debug("Get active WebRTC streams successful, count: {}", activeStreams.size());
            return Result.success(activeStreams);
        } catch (Exception e) {
            logger.error("Failed to get active WebRTC streams: {}", e.getMessage(), e);
            return Result.error("Failed to get active WebRTC streams: " + e.getMessage());
        }
    }
    
    /**
     * Check device WebRTC stream status
     */
    @GetMapping("/check/{deviceId}")
    public Result<Map<String, Object>> checkStreamStatus(@PathVariable String deviceId) {
        try {
            Map<String, Object> streamInfo = webrtcService.getStreamInfo(deviceId);
            logger.debug("Check WebRTC stream status: deviceId={}, info={}", deviceId, streamInfo);
            return Result.success(streamInfo);
        } catch (Exception e) {
            logger.error("Failed to check WebRTC stream status: deviceId={}, error={}", 
                        deviceId, e.getMessage(), e);
            return Result.error("Failed to check WebRTC stream status: " + e.getMessage());
        }
    }
    
    /**
     * Validate if RTSP stream is available
     */
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateRtspStream(@RequestBody Map<String, String> request) {
        String rtspUrl = request.get("rtspUrl");
        
        try {
            // Validate RTSP URL format
            if (!webrtcService.isValidRtspUrl(rtspUrl)) {
                return Result.error("Invalid RTSP address");
            }
            
            // Check if WebRTC service is available
            boolean webrtcAvailable = webrtcService.isWebRTCServerAvailable();
            
            Map<String, Object> result = new HashMap<>();
            result.put("rtspUrl", rtspUrl);
            result.put("valid", true);
            result.put("webrtcAvailable", webrtcAvailable);
            
            logger.debug("Validate RTSP stream: {}", result);
            return Result.success(result);
            
        } catch (Exception e) {
            logger.error("Failed to validate RTSP stream: rtspUrl={}, error={}", rtspUrl, e.getMessage(), e);
            return Result.error("Failed to validate RTSP stream: " + e.getMessage());
        }
    }
    
    /**
     * Stop all WebRTC streams (admin function)
     */
    @PostMapping("/stop-all")
    public Result<Void> stopAllStreams() {
        logger.info("Received stop all WebRTC streams request");
        
        try {
            webrtcService.stopAllStreams();
            logger.info("All WebRTC streams stopped");
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to stop all WebRTC streams: {}", e.getMessage(), e);
            return Result.error("Failed to stop all WebRTC streams: " + e.getMessage());
        }
    }
    
    /**
     * Refresh WebRTC connection
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshWebRTCConnection() {
        logger.info("Received refresh WebRTC connection request");
        
        try {
            // Check WebRTC service status
            Map<String, Object> status = webrtcService.getServerStatus();
            
            logger.info("WebRTC connection refresh completed: {}", status);
            return Result.success(status);
            
        } catch (Exception e) {
            logger.error("Failed to refresh WebRTC connection: {}", e.getMessage(), e);
            return Result.error("Failed to refresh WebRTC connection: " + e.getMessage());
        }
    }
} 