package com.vlstream.entity;

/**
 * 停止流转换请求实体
 */
public class StopStreamRequest {
    private String deviceId;
    
    public StopStreamRequest() {}
    
    public StopStreamRequest(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    @Override
    public String toString() {
        return "StopStreamRequest{" +
                "deviceId='" + deviceId + '\'' +
                '}';
    }
} 