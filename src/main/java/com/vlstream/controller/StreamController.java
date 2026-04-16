package com.vlstream.controller;

import com.vlstream.common.Result;
import com.vlstream.entity.StreamRequest;
import com.vlstream.entity.StopStreamRequest;
import com.vlstream.service.RTSPConverterService;
import com.vlstream.service.WebRTCService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Stream Conversion Controller
 * Provides API interfaces for converting RTSP streams to HLS/WebRTC
 */
@Api(tags = "Stream Conversion Controller")
@RestController
@RequestMapping("/api/stream")
@CrossOrigin(origins = "*")
public class StreamController {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);
    
    @Autowired
    private RTSPConverterService converterService;
    
    @Autowired
    private WebRTCService webrtcService;
    
    @Value("${webrtc.streamer.enabled:true}")
    private boolean webrtcEnabled;
    
    @Value("${stream.preferred-mode:webrtc}")
    private String preferredMode;
    
    /**
     * Start stream conversion (intelligently select WebRTC or HLS)
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> startStream(@RequestBody StreamRequest request) {
        logger.info("Received start stream conversion request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL cannot be empty");
            }
            
            if (!request.getRtspUrl().toLowerCase().startsWith("rtsp://")) {
                return Result.error("Not a valid RTSP URL");
            }
            
            // Determine conversion mode
            String mode = request.getMode() != null ? request.getMode() : preferredMode;
            
            Map<String, Object> result = new HashMap<>();
            
            if ("webrtc".equalsIgnoreCase(mode) && webrtcEnabled && webrtcService.isWebRTCServerAvailable()) {
                // Use WebRTC solution
                String streamId = webrtcService.startWebRTCStream(request.getDeviceId(), request.getRtspUrl());
                Map<String, Object> streamInfo = webrtcService.getStreamInfo(request.getDeviceId());
                
                result.put("mode", "webrtc");
                result.put("streamId", streamId);
                result.put("streamInfo", streamInfo);
                result.put("playUrl", webrtcService.getWebRTCPlayUrl(request.getDeviceId()));
                
                logger.info("WebRTC stream conversion started successfully: deviceId={}, streamId={}", request.getDeviceId(), streamId);
                
            } else {
                // Fallback to HLS solution
                String quality = request.getQuality() != null ? request.getQuality() : "medium";
                String hlsUrl = converterService.startHLSStream(request.getDeviceId(), request.getRtspUrl(), quality);
                
                result.put("mode", "hls");
                result.put("hlsUrl", hlsUrl);
                result.put("quality", quality);
                
                logger.info("HLS stream conversion started successfully: deviceId={}, hlsUrl={}", request.getDeviceId(), hlsUrl);
            }
            
            result.put("deviceId", request.getDeviceId());
            result.put("rtspUrl", request.getRtspUrl());
            
            return Result.success(result);
            
        } catch (Exception e) {
            logger.error("Failed to start stream conversion: deviceId={}, error={}", request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to start stream conversion: " + e.getMessage());
        }
    }
    
    /**
     * Start HLS stream conversion (compatible with old interface)
     */
    @PostMapping("/start-hls")
    public Result<String> startHLSStream(@RequestBody StreamRequest request) {
        logger.info("Received start HLS conversion request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL cannot be empty");
            }
            
            if (!request.getRtspUrl().toLowerCase().startsWith("rtsp://")) {
                return Result.error("Not a valid RTSP URL");
            }
            
            // Set default quality
            String quality = request.getQuality();
            if (quality == null || quality.trim().isEmpty()) {
                quality = "medium";
            }
            
            // Call service to start conversion
            String hlsUrl = converterService.startHLSStream(
                request.getDeviceId(), 
                request.getRtspUrl(),
                quality
            );
            
            logger.info("HLS conversion started successfully: deviceId={}, hlsUrl={}", request.getDeviceId(), hlsUrl);
            return Result.success(hlsUrl);
            
        } catch (Exception e) {
            logger.error("Failed to start HLS stream conversion: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to start HLS stream conversion: " + e.getMessage());
        }
    }
    
    /**
     * Start WebRTC stream conversion
     */
    @PostMapping("/start-webrtc")
    public Result<Map<String, Object>> startWebRTCStream(@RequestBody StreamRequest request) {
        logger.info("Received start WebRTC conversion request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL cannot be empty");
            }
            
            if (!webrtcEnabled) {
                return Result.error("WebRTC feature is not enabled");
            }
            
            if (!webrtcService.isWebRTCServerAvailable()) {
                return Result.error("WebRTC service is not available");
            }
            
            // Start WebRTC stream
            String streamId = webrtcService.startWebRTCStream(request.getDeviceId(), request.getRtspUrl());
            Map<String, Object> streamInfo = webrtcService.getStreamInfo(request.getDeviceId());
            
            logger.info("WebRTC stream conversion started successfully: deviceId={}, streamId={}", request.getDeviceId(), streamId);
            return Result.success(streamInfo);
            
        } catch (Exception e) {
            logger.error("Failed to start WebRTC stream conversion: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to start WebRTC stream conversion: " + e.getMessage());
        }
    }
    
    /**
     * Stop stream conversion (intelligently stop WebRTC and HLS)
     */
    @PostMapping("/stop")
    public Result<Void> stopStream(@RequestBody StopStreamRequest request) {
        logger.info("Received stop stream conversion request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            // Stop both WebRTC and HLS streams (if they exist)
            boolean webrtcStopped = false;
            boolean hlsStopped = false;
            
            // Stop WebRTC stream
            if (webrtcService.isStreamActive(request.getDeviceId())) {
                webrtcService.stopWebRTCStream(request.getDeviceId());
                webrtcStopped = true;
            }
            
            // Stop HLS stream
            if (converterService.isStreamActive(request.getDeviceId())) {
                converterService.stopHLSStream(request.getDeviceId());
                hlsStopped = true;
            }
            
            logger.info("Stream conversion stopped successfully: deviceId={}, webrtc={}, hls={}", 
                       request.getDeviceId(), webrtcStopped, hlsStopped);
            return Result.success();
            
        } catch (Exception e) {
            logger.error("Failed to stop stream conversion: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to stop stream conversion: " + e.getMessage());
        }
    }
    
    /**
     * Stop HLS stream conversion (compatible with old interface)
     */
    @PostMapping("/stop-hls")
    public Result<Void> stopHLSStream(@RequestBody StopStreamRequest request) {
        logger.info("Received stop HLS conversion request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            // Call service to stop conversion
            converterService.stopHLSStream(request.getDeviceId());
            
            logger.info("HLS conversion stopped successfully: deviceId={}", request.getDeviceId());
            return Result.success();
            
        } catch (Exception e) {
            logger.error("Failed to stop HLS stream conversion: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to stop HLS stream conversion: " + e.getMessage());
        }
    }

    /**
     * Stop WebRTC stream conversion
     */
    @PostMapping("/stop-webrtc")
    public Result<Void> stopWebRTCStream(@RequestBody StopStreamRequest request) {
        logger.info("Received stop WebRTC conversion request: {}", request);
        
        try {
            // Parameter validation
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("Device ID cannot be empty");
            }
            
            // Stop WebRTC stream
            webrtcService.stopWebRTCStream(request.getDeviceId());
            
            logger.info("WebRTC conversion stopped successfully: deviceId={}", request.getDeviceId());
            return Result.success();
            
        } catch (Exception e) {
            logger.error("Failed to stop WebRTC stream conversion: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("Failed to stop WebRTC stream conversion: " + e.getMessage());
        }
    }
    
    /**
     * Get active stream information
     */
    @GetMapping("/active")
    public Result<Map<String, Object>> getActiveStreams() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get HLS streams
            Map<String, String> hlsStreams = converterService.getActiveStreams();
            result.put("hls", hlsStreams);
            
            // Get WebRTC streams
            Map<String, String> webrtcStreams = webrtcService.getActiveStreams();
            result.put("webrtc", webrtcStreams);
            
            // Total
            result.put("total", hlsStreams.size() + webrtcStreams.size());
            
            logger.debug("Retrieved active stream information successfully: HLS={}, WebRTC={}", hlsStreams.size(), webrtcStreams.size());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("Failed to retrieve active stream information: {}", e.getMessage(), e);
            return Result.error("Failed to retrieve active stream information: " + e.getMessage());
        }
    }
    
    /**
     * Check if device stream is active
     */
    @GetMapping("/check/{deviceId}")
    public Result<Map<String, Object>> checkStreamStatus(@PathVariable String deviceId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Check HLS stream
            boolean hlsActive = converterService.isStreamActive(deviceId);
            result.put("hls", hlsActive);
            
            // Check WebRTC stream
            boolean webrtcActive = webrtcService.isStreamActive(deviceId);
            result.put("webrtc", webrtcActive);
            
            // Active if any one is active
            result.put("active", hlsActive || webrtcActive);
            result.put("deviceId", deviceId);
            
            // If WebRTC is active, get detailed information
            if (webrtcActive) {
                result.put("webrtcInfo", webrtcService.getStreamInfo(deviceId));
            }
            
            logger.debug("Checked stream status: deviceId={}, hls={}, webrtc={}", deviceId, hlsActive, webrtcActive);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("Failed to check stream status: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return Result.error("Failed to check stream status: " + e.getMessage());
        }
    }
    
    /**
     * Get stream conversion configuration
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getStreamConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // WebRTC configuration
            config.put("webrtc", webrtcService.getServerStatus());
            
            // HLS configuration
            Map<String, Object> hlsConfig = new HashMap<>();
            hlsConfig.put("enabled", true);
            hlsConfig.put("activeStreams", converterService.getActiveStreams().size());
            config.put("hls", hlsConfig);
            
            // Preferred mode
            config.put("preferredMode", preferredMode);
            
            logger.debug("Retrieved stream conversion configuration successfully: {}", config);
            return Result.success(config);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve stream conversion configuration: {}", e.getMessage(), e);
            return Result.error("Failed to retrieve stream conversion configuration: " + e.getMessage());
        }
    }

    /**
     * Stop all conversions (admin function)
     */
    @PostMapping("/stop-all")
    public Result<Void> stopAllStreams() {
        logger.info("Received stop all conversions request");
        
        try {
            // Stop all HLS conversions
            converterService.stopAllStreams();
            
            // Stop all WebRTC conversions
            webrtcService.stopAllStreams();
            
            logger.info("All stream conversions have been stopped");
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to stop all conversions: {}", e.getMessage(), e);
            return Result.error("Failed to stop all conversions: " + e.getMessage());
        }
    }
} 