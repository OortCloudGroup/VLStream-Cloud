package com.vlstream.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 通用响应码
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    
    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    
    // 业务错误码 1xxx
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "缺少必要参数"),
    
    // 用户相关错误 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    PASSWORD_ERROR(2004, "密码错误"),
    
    // 设备相关错误 3xxx
    DEVICE_NOT_FOUND(3001, "设备不存在"),
    DEVICE_OFFLINE(3002, "设备离线"),
    
    // 算法相关错误 4xxx
    ALGORITHM_NOT_FOUND(4001, "算法不存在"),
    ALGORITHM_FAILED(4004, "算法执行失败"),
    
    // 系统错误 9xxx
    SYSTEM_ERROR(9001, "系统内部错误"),
    DATABASE_ERROR(9002, "数据库错误");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;
} 