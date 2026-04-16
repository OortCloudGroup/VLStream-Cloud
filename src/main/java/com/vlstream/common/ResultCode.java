package com.vlstream.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response Code Enum
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // Common response codes
    SUCCESS(200, "Operation successful"),
    ERROR(500, "Operation failed"),
    
    // Client errors 4xx
    BAD_REQUEST(400, "Invalid request parameters"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden access"),
    NOT_FOUND(404, "Resource not found"),
    
    // Business error codes 1xxx
    PARAM_ERROR(1001, "Parameter error"),
    PARAM_MISSING(1002, "Missing required parameters"),
    
    // User-related errors 2xxx
    USER_NOT_FOUND(2001, "User not found"),
    PASSWORD_ERROR(2004, "Password error"),
    
    // Device-related errors 3xxx
    DEVICE_NOT_FOUND(3001, "Device not found"),
    DEVICE_OFFLINE(3002, "Device offline"),
    
    // Algorithm-related errors 4xxx
    ALGORITHM_NOT_FOUND(4001, "Algorithm not found"),
    ALGORITHM_FAILED(4004, "Algorithm execution failed"),
    
    // System errors 9xxx
    SYSTEM_ERROR(9001, "System internal error"),
    DATABASE_ERROR(9002, "Database error");

    /**
     * Response code
     */
    private final Integer code;

    /**
     * Response message
     */
    private final String message;
} 