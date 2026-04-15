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
 * 流转换控制器
 * 提供RTSP流转换为HLS/WebRTC的API接口
 */
@Api(tags = "流转换控制器")
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
     * 启动流转换（智能选择WebRTC或HLS）
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> startStream(@RequestBody StreamRequest request) {
        logger.info("收到启动流转换请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL不能为空");
            }
            
            if (!request.getRtspUrl().toLowerCase().startsWith("rtsp://")) {
                return Result.error("不是有效的RTSP URL");
            }
            
            // 确定转换模式
            String mode = request.getMode() != null ? request.getMode() : preferredMode;
            
            Map<String, Object> result = new HashMap<>();
            
            if ("webrtc".equalsIgnoreCase(mode) && webrtcEnabled && webrtcService.isWebRTCServerAvailable()) {
                // 使用WebRTC方案
                String streamId = webrtcService.startWebRTCStream(request.getDeviceId(), request.getRtspUrl());
                Map<String, Object> streamInfo = webrtcService.getStreamInfo(request.getDeviceId());
                
                result.put("mode", "webrtc");
                result.put("streamId", streamId);
                result.put("streamInfo", streamInfo);
                result.put("playUrl", webrtcService.getWebRTCPlayUrl(request.getDeviceId()));
                
                logger.info("WebRTC流转换启动成功: deviceId={}, streamId={}", request.getDeviceId(), streamId);
                
            } else {
                // 回退到HLS方案
                String quality = request.getQuality() != null ? request.getQuality() : "medium";
                String hlsUrl = converterService.startHLSStream(request.getDeviceId(), request.getRtspUrl(), quality);
                
                result.put("mode", "hls");
                result.put("hlsUrl", hlsUrl);
                result.put("quality", quality);
                
                logger.info("HLS流转换启动成功: deviceId={}, hlsUrl={}", request.getDeviceId(), hlsUrl);
            }
            
            result.put("deviceId", request.getDeviceId());
            result.put("rtspUrl", request.getRtspUrl());
            
            return Result.success(result);
            
        } catch (Exception e) {
            logger.error("启动流转换失败: deviceId={}, error={}", request.getDeviceId(), e.getMessage(), e);
            return Result.error("启动流转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动HLS流转换（兼容旧接口）
     */
    @PostMapping("/start-hls")
    public Result<String> startHLSStream(@RequestBody StreamRequest request) {
        logger.info("收到启动HLS转换请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL不能为空");
            }
            
            if (!request.getRtspUrl().toLowerCase().startsWith("rtsp://")) {
                return Result.error("不是有效的RTSP URL");
            }
            
            // 设置默认质量
            String quality = request.getQuality();
            if (quality == null || quality.trim().isEmpty()) {
                quality = "medium";
            }
            
            // 调用服务启动转换
            String hlsUrl = converterService.startHLSStream(
                request.getDeviceId(), 
                request.getRtspUrl(),
                quality
            );
            
            logger.info("HLS转换启动成功: deviceId={}, hlsUrl={}", request.getDeviceId(), hlsUrl);
            return Result.success(hlsUrl);
            
        } catch (Exception e) {
            logger.error("启动HLS流转换失败: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("启动HLS流转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动WebRTC流转换
     */
    @PostMapping("/start-webrtc")
    public Result<Map<String, Object>> startWebRTCStream(@RequestBody StreamRequest request) {
        logger.info("收到启动WebRTC转换请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL不能为空");
            }
            
            if (!webrtcEnabled) {
                return Result.error("WebRTC功能未启用");
            }
            
            if (!webrtcService.isWebRTCServerAvailable()) {
                return Result.error("WebRTC服务不可用");
            }
            
            // 启动WebRTC流
            String streamId = webrtcService.startWebRTCStream(request.getDeviceId(), request.getRtspUrl());
            Map<String, Object> streamInfo = webrtcService.getStreamInfo(request.getDeviceId());
            
            logger.info("WebRTC流转换启动成功: deviceId={}, streamId={}", request.getDeviceId(), streamId);
            return Result.success(streamInfo);
            
        } catch (Exception e) {
            logger.error("启动WebRTC流转换失败: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("启动WebRTC流转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止流转换（智能停止WebRTC和HLS）
     */
    @PostMapping("/stop")
    public Result<Void> stopStream(@RequestBody StopStreamRequest request) {
        logger.info("收到停止流转换请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            // 同时停止WebRTC和HLS流（如果存在）
            boolean webrtcStopped = false;
            boolean hlsStopped = false;
            
            // 停止WebRTC流
            if (webrtcService.isStreamActive(request.getDeviceId())) {
                webrtcService.stopWebRTCStream(request.getDeviceId());
                webrtcStopped = true;
            }
            
            // 停止HLS流
            if (converterService.isStreamActive(request.getDeviceId())) {
                converterService.stopHLSStream(request.getDeviceId());
                hlsStopped = true;
            }
            
            logger.info("流转换停止成功: deviceId={}, webrtc={}, hls={}", 
                       request.getDeviceId(), webrtcStopped, hlsStopped);
            return Result.success();
            
        } catch (Exception e) {
            logger.error("停止流转换失败: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("停止流转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止HLS流转换（兼容旧接口）
     */
    @PostMapping("/stop-hls")
    public Result<Void> stopHLSStream(@RequestBody StopStreamRequest request) {
        logger.info("收到停止HLS转换请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            // 调用服务停止转换
            converterService.stopHLSStream(request.getDeviceId());
            
            logger.info("HLS转换停止成功: deviceId={}", request.getDeviceId());
            return Result.success();
            
        } catch (Exception e) {
            logger.error("停止HLS流转换失败: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("停止HLS流转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止WebRTC流转换
     */
    @PostMapping("/stop-webrtc")
    public Result<Void> stopWebRTCStream(@RequestBody StopStreamRequest request) {
        logger.info("收到停止WebRTC转换请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            // 停止WebRTC流
            webrtcService.stopWebRTCStream(request.getDeviceId());
            
            logger.info("WebRTC转换停止成功: deviceId={}", request.getDeviceId());
            return Result.success();
            
        } catch (Exception e) {
            logger.error("停止WebRTC流转换失败: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("停止WebRTC流转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活跃的流信息
     */
    @GetMapping("/active")
    public Result<Map<String, Object>> getActiveStreams() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 获取HLS流
            Map<String, String> hlsStreams = converterService.getActiveStreams();
            result.put("hls", hlsStreams);
            
            // 获取WebRTC流
            Map<String, String> webrtcStreams = webrtcService.getActiveStreams();
            result.put("webrtc", webrtcStreams);
            
            // 总计
            result.put("total", hlsStreams.size() + webrtcStreams.size());
            
            logger.debug("获取活跃流信息成功: HLS={}, WebRTC={}", hlsStreams.size(), webrtcStreams.size());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取活跃流信息失败: {}", e.getMessage(), e);
            return Result.error("获取活跃流信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查设备流是否活跃
     */
    @GetMapping("/check/{deviceId}")
    public Result<Map<String, Object>> checkStreamStatus(@PathVariable String deviceId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 检查HLS流
            boolean hlsActive = converterService.isStreamActive(deviceId);
            result.put("hls", hlsActive);
            
            // 检查WebRTC流
            boolean webrtcActive = webrtcService.isStreamActive(deviceId);
            result.put("webrtc", webrtcActive);
            
            // 任意一个活跃则为活跃
            result.put("active", hlsActive || webrtcActive);
            result.put("deviceId", deviceId);
            
            // 如果WebRTC活跃，获取详细信息
            if (webrtcActive) {
                result.put("webrtcInfo", webrtcService.getStreamInfo(deviceId));
            }
            
            logger.debug("检查流状态: deviceId={}, hls={}, webrtc={}", deviceId, hlsActive, webrtcActive);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("检查流状态失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return Result.error("检查流状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取流转换配置
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getStreamConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // WebRTC配置
            config.put("webrtc", webrtcService.getServerStatus());
            
            // HLS配置
            Map<String, Object> hlsConfig = new HashMap<>();
            hlsConfig.put("enabled", true);
            hlsConfig.put("activeStreams", converterService.getActiveStreams().size());
            config.put("hls", hlsConfig);
            
            // 首选模式
            config.put("preferredMode", preferredMode);
            
            logger.debug("获取流转换配置成功: {}", config);
            return Result.success(config);
            
        } catch (Exception e) {
            logger.error("获取流转换配置失败: {}", e.getMessage(), e);
            return Result.error("获取流转换配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止所有转换（管理员功能）
     */
    @PostMapping("/stop-all")
    public Result<Void> stopAllStreams() {
        logger.info("收到停止所有转换请求");
        
        try {
            // 停止所有HLS转换
            converterService.stopAllStreams();
            
            // 停止所有WebRTC转换
            webrtcService.stopAllStreams();
            
            logger.info("所有流转换已停止");
            return Result.success();
        } catch (Exception e) {
            logger.error("停止所有转换失败: {}", e.getMessage(), e);
            return Result.error("停止所有转换失败: " + e.getMessage());
        }
    }
} 