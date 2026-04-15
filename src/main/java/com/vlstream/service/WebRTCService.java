package com.vlstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC流服务
 * 负责管理WebRTC-streamer服务和RTSP流处理
 */
@Service
public class WebRTCService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebRTCService.class);
    
    @Value("${webrtc.streamer.url:http://localhost:8000}")
    private String webrtcServerUrl;

    @Value("${webrtc.streamer.internetUrl}")
    private String webrtcInternetServerUrl;

    @Value("${webrtc.streamer.enabled:true}")
    private boolean webrtcEnabled;
    
    @Value("${webrtc.streamer.timeout:30000}")
    private int timeout;
    
    @Value("${webrtc.streamer.retry-count:3}")
    private int retryCount;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, String> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, String> streamUrls = new ConcurrentHashMap<>();
    
    public WebRTCService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 检查WebRTC服务状态
     */
    public boolean isWebRTCServerAvailable() {
        if (!webrtcEnabled) {
            logger.info("WebRTC服务已禁用");
            return false;
        }
        
        try {
            String url = webrtcServerUrl + "/api/getIceServers";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            boolean available = response.getStatusCode().is2xxSuccessful();
            logger.debug("WebRTC服务状态检查: {}", available ? "可用" : "不可用");
            return available;
        } catch (Exception e) {
            logger.error("WebRTC服务不可用: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 启动WebRTC流
     */
    public String startWebRTCStream(String deviceId, String rtspUrl) {
        logger.info("启动WebRTC流: deviceId={}, rtspUrl={}", deviceId, rtspUrl);
        
        if (!isWebRTCServerAvailable()) {
            throw new RuntimeException("WebRTC服务不可用");
        }
        
        if (!isValidRtspUrl(rtspUrl)) {
            throw new RuntimeException("无效的RTSP地址: " + rtspUrl);
        }
        
        try {
            // 如果已有流在运行，先停止
            if (activeStreams.containsKey(deviceId)) {
                stopWebRTCStream(deviceId);
            }
            
            // 调用WebRTC-streamer API添加流
            String streamId = addStreamToWebRTC(deviceId, rtspUrl);
            
            // 记录活跃流和RTSP URL
            activeStreams.put(deviceId, streamId);
            streamUrls.put(deviceId, rtspUrl);
            
            logger.info("WebRTC流启动成功: deviceId={}, streamId={}", deviceId, streamId);
            return streamId;
            
        } catch (Exception e) {
            logger.error("启动WebRTC流失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new RuntimeException("启动WebRTC流失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 停止WebRTC流
     */
    public void stopWebRTCStream(String deviceId) {
        logger.info("停止WebRTC流: deviceId={}", deviceId);
        
        String streamId = activeStreams.remove(deviceId);
        streamUrls.remove(deviceId);
        if (streamId != null) {
            try {
                removeStreamFromWebRTC(streamId);
                logger.info("WebRTC流停止成功: deviceId={}, streamId={}", deviceId, streamId);
            } catch (Exception e) {
                logger.error("停止WebRTC流失败: deviceId={}, streamId={}, error={}", 
                           deviceId, streamId, e.getMessage(), e);
            }
        } else {
            logger.warn("未找到设备的WebRTC流: deviceId={}", deviceId);
        }
    }
    
    /**
     * 获取活跃的WebRTC流
     */
    public Map<String, String> getActiveStreams() {
        return new HashMap<>(activeStreams);
    }
    
    /**
     * 检查设备WebRTC流是否活跃
     */
    public boolean isStreamActive(String deviceId) {
        return activeStreams.containsKey(deviceId);
    }
    
    /**
     * 停止所有WebRTC流
     */
    public void stopAllStreams() {
        logger.info("停止所有WebRTC流，当前活跃流数量: {}", activeStreams.size());
        
        for (String deviceId : activeStreams.keySet()) {
            try {
                stopWebRTCStream(deviceId);
            } catch (Exception e) {
                logger.error("停止WebRTC流失败: deviceId={}, error={}", deviceId, e.getMessage());
            }
        }
        
        activeStreams.clear();
        streamUrls.clear();
        logger.info("所有WebRTC流已停止");
    }
    
    /**
     * 获取WebRTC服务器URL
     */
    public String getWebRTCServerUrl() {
        return webrtcServerUrl;
    }
    
    /**
     * 验证RTSP URL格式
     */
    public boolean isValidRtspUrl(String url) {
        return url != null && url.toLowerCase().startsWith("rtsp://");
    }
    
    /**
     * 创建WebRTC连接 - 简化版本，返回播放URL给前端
     */
    private String addStreamToWebRTC(String deviceId, String rtspUrl) throws Exception {
        String streamId = "stream_" + deviceId;
        
        // 验证webrtc-streamer服务是否可用
        String iceServersUrl = webrtcServerUrl + "/api/getIceServers";
        ResponseEntity<String> iceResponse = restTemplate.getForEntity(iceServersUrl, String.class);
        if (!iceResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("WebRTC服务不可用: " + iceResponse.getBody());
        }
        
        logger.info("WebRTC服务验证成功，准备创建流: streamId={}, rtspUrl={}", streamId, rtspUrl);
        
        // 直接返回streamId，让前端处理WebRTC连接
        // 前端将使用: webrtc-streamer.html?video=rtsp://...
        return streamId;
    }
    
    /**
     * 获取WebRTC播放URL - 让前端直接连接webrtc-streamer
     */
    public String getWebRTCPlayUrl(String deviceId) {
        String streamId = activeStreams.get(deviceId);
        if (streamId != null) {
            // 获取原始RTSP URL
            String rtspUrl = getOriginalRtspUrl(deviceId);
            if (rtspUrl != null) {
                try {
                    String encodedUrl = java.net.URLEncoder.encode(rtspUrl, "UTF-8");
                    return webrtcInternetServerUrl + "/webrtcstreamer.html?video=" + encodedUrl;
                } catch (Exception e) {
                    logger.error("编码RTSP URL失败: {}", e.getMessage());
                }
            }
            // 备选方案：使用streamId
            return webrtcInternetServerUrl + "/webrtcstreamer.html?video=" + streamId;
        }
        return null;
    }
    
    /**
     * 获取原始RTSP URL
     */
    private String getOriginalRtspUrl(String deviceId) {
        return streamUrls.get(deviceId);
    }
    
    /**
     * 断开WebRTC连接
     */
    private void removeStreamFromWebRTC(String streamId) throws Exception {
        String url = String.format("%s/api/hangup?peerid=%s",
            webrtcServerUrl,
            streamId
        );
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("断开WebRTC连接失败: " + response.getBody());
        }
        
        logger.info("WebRTC连接断开成功: streamId={}", streamId);
    }
    
    /**
     * 获取WebRTC流信息
     */
    public Map<String, Object> getStreamInfo(String deviceId) {
        Map<String, Object> info = new HashMap<>();
        String streamId = activeStreams.get(deviceId);
        String rtspUrl = streamUrls.get(deviceId);
        
        info.put("deviceId", deviceId);
        info.put("streamId", streamId);
        info.put("rtspUrl", rtspUrl);
        info.put("active", streamId != null);
        info.put("webrtcUrl", getWebRTCPlayUrl(deviceId));
        info.put("serverUrl", webrtcInternetServerUrl);
        
        return info;
    }
    
    /**
     * 获取WebRTC服务器状态
     */
    public Map<String, Object> getServerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", webrtcEnabled);
        status.put("serverUrl", webrtcServerUrl);
        status.put("available", isWebRTCServerAvailable());
        status.put("activeStreams", activeStreams.size());
        status.put("timeout", timeout);
        status.put("retryCount", retryCount);
        
        return status;
    }
} 