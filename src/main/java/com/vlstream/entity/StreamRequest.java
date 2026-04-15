package com.vlstream.entity;

/**
 * 流转换请求实体
 */
public class StreamRequest {
    private String deviceId;
    private String rtspUrl;
    private String quality; // 可选：low, medium, high
    private String mode; // 可选：webrtc, hls
    
    public StreamRequest() {}
    
    public StreamRequest(String deviceId, String rtspUrl) {
        this.deviceId = deviceId;
        this.rtspUrl = rtspUrl;
        this.quality = "medium"; // 默认中等质量
        this.mode = "webrtc"; // 默认WebRTC模式
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getRtspUrl() {
        return rtspUrl;
    }
    
    public void setRtspUrl(String rtspUrl) {
        this.rtspUrl = rtspUrl;
    }
    
    public String getQuality() {
        return quality;
    }
    
    public void setQuality(String quality) {
        this.quality = quality;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    @Override
    public String toString() {
        return "StreamRequest{" +
                "deviceId='" + deviceId + '\'' +
                ", rtspUrl='" + rtspUrl + '\'' +
                ", quality='" + quality + '\'' +
                ", mode='" + mode + '\'' +
                '}';
    }
} 