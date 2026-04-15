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
 * WebRTC流控制器
 * 提供WebRTC流管理的API接口
 */
@Api(tags = "WebRTC流控制器")
@RestController
@RequestMapping("/api/webrtc")
@CrossOrigin(origins = "*")
public class WebRTCController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebRTCController.class);
    
    @Autowired
    private WebRTCService webrtcService;
    
    /**
     * 获取WebRTC服务配置
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getWebRTCConfig() {
        try {
            Map<String, Object> config = webrtcService.getServerStatus();
            logger.debug("获取WebRTC配置成功: {}", config);
            return Result.success(config);
        } catch (Exception e) {
            logger.error("获取WebRTC配置失败: {}", e.getMessage(), e);
            return Result.error("获取WebRTC配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查WebRTC服务状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getWebRTCStatus() {
        try {
            Map<String, Object> status = webrtcService.getServerStatus();
            logger.debug("获取WebRTC状态成功: {}", status);
            return Result.success(status);
        } catch (Exception e) {
            logger.error("获取WebRTC状态失败: {}", e.getMessage(), e);
            return Result.error("获取WebRTC状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动WebRTC流
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> startWebRTCStream(@RequestBody StreamRequest request) {
        logger.info("收到启动WebRTC流请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            if (request.getRtspUrl() == null || request.getRtspUrl().trim().isEmpty()) {
                return Result.error("RTSP URL不能为空");
            }
            
            if (!webrtcService.isValidRtspUrl(request.getRtspUrl())) {
                return Result.error("不是有效的RTSP URL");
            }
            
            // 启动WebRTC流
            String streamId = webrtcService.startWebRTCStream(
                request.getDeviceId(), 
                request.getRtspUrl()
            );
            
            // 获取流信息
            Map<String, Object> streamInfo = webrtcService.getStreamInfo(request.getDeviceId());
            
            logger.info("WebRTC流启动成功: deviceId={}, streamId={}", 
                       request.getDeviceId(), streamId);
            return Result.success(streamInfo);
            
        } catch (Exception e) {
            logger.error("启动WebRTC流失败: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("启动WebRTC流失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止WebRTC流
     */
    @PostMapping("/stop")
    public Result<Void> stopWebRTCStream(@RequestBody StopStreamRequest request) {
        logger.info("收到停止WebRTC流请求: {}", request);
        
        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }
            
            // 停止WebRTC流
            webrtcService.stopWebRTCStream(request.getDeviceId());
            
            logger.info("WebRTC流停止成功: deviceId={}", request.getDeviceId());
            return Result.success();
            
        } catch (Exception e) {
            logger.error("停止WebRTC流失败: deviceId={}, error={}", 
                        request.getDeviceId(), e.getMessage(), e);
            return Result.error("停止WebRTC流失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活跃的WebRTC流
     */
    @GetMapping("/active")
    public Result<Map<String, String>> getActiveStreams() {
        try {
            Map<String, String> activeStreams = webrtcService.getActiveStreams();
            logger.debug("获取活跃WebRTC流成功，数量: {}", activeStreams.size());
            return Result.success(activeStreams);
        } catch (Exception e) {
            logger.error("获取活跃WebRTC流失败: {}", e.getMessage(), e);
            return Result.error("获取活跃WebRTC流失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查设备WebRTC流状态
     */
    @GetMapping("/check/{deviceId}")
    public Result<Map<String, Object>> checkStreamStatus(@PathVariable String deviceId) {
        try {
            Map<String, Object> streamInfo = webrtcService.getStreamInfo(deviceId);
            logger.debug("检查WebRTC流状态: deviceId={}, info={}", deviceId, streamInfo);
            return Result.success(streamInfo);
        } catch (Exception e) {
            logger.error("检查WebRTC流状态失败: deviceId={}, error={}", 
                        deviceId, e.getMessage(), e);
            return Result.error("检查WebRTC流状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证RTSP流是否可用
     */
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateRtspStream(@RequestBody Map<String, String> request) {
        String rtspUrl = request.get("rtspUrl");
        
        try {
            // 验证RTSP URL格式
            if (!webrtcService.isValidRtspUrl(rtspUrl)) {
                return Result.error("无效的RTSP地址");
            }
            
            // 检查WebRTC服务是否可用
            boolean webrtcAvailable = webrtcService.isWebRTCServerAvailable();
            
            Map<String, Object> result = new HashMap<>();
            result.put("rtspUrl", rtspUrl);
            result.put("valid", true);
            result.put("webrtcAvailable", webrtcAvailable);
            
            logger.debug("验证RTSP流: {}", result);
            return Result.success(result);
            
        } catch (Exception e) {
            logger.error("验证RTSP流失败: rtspUrl={}, error={}", rtspUrl, e.getMessage(), e);
            return Result.error("验证RTSP流失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止所有WebRTC流（管理员功能）
     */
    @PostMapping("/stop-all")
    public Result<Void> stopAllStreams() {
        logger.info("收到停止所有WebRTC流请求");
        
        try {
            webrtcService.stopAllStreams();
            logger.info("所有WebRTC流已停止");
            return Result.success();
        } catch (Exception e) {
            logger.error("停止所有WebRTC流失败: {}", e.getMessage(), e);
            return Result.error("停止所有WebRTC流失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新WebRTC连接
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshWebRTCConnection() {
        logger.info("收到刷新WebRTC连接请求");
        
        try {
            // 检查WebRTC服务状态
            Map<String, Object> status = webrtcService.getServerStatus();
            
            logger.info("WebRTC连接刷新完成: {}", status);
            return Result.success(status);
            
        } catch (Exception e) {
            logger.error("刷新WebRTC连接失败: {}", e.getMessage(), e);
            return Result.error("刷新WebRTC连接失败: " + e.getMessage());
        }
    }
} 