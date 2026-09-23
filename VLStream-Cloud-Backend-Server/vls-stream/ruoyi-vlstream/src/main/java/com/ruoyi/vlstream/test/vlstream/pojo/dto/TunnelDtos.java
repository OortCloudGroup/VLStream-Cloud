/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

public final class TunnelDtos {
    private TunnelDtos() {
    }

    @Data
    public static class EnrollmentView {
        private String deviceId;
        private String enrollmentCode;
        private Date expiresAt;
    }

    @Data
    public static class AgentRegisterRequest {
        @NotBlank
        @Size(max = 100)
        private String deviceId;
        @NotBlank
        @Size(max = 256)
        private String enrollmentCode;
        @NotBlank
        @Size(max = 64)
        private String agentInstanceId;
        @NotBlank
        @Size(max = 32)
        private String agentVersion;
        @NotBlank
        @Size(max = 32)
        private String architecture;
        @Size(max = 64)
        private String firmwareVersion;
        @Size(max = 32)
        private String initSystem;
        @NotBlank
        @Size(max = 8)
        private String localWebScheme;
        @NotNull
        @Min(1)
        @Max(65535)
        private Integer localWebPort;
    }

    @Data
    public static class AgentHeartbeatRequest {
        @NotBlank
        @Size(max = 64)
        private String agentInstanceId;
        @NotNull
        @Min(0)
        private Long configGeneration;
        @NotBlank
        @Size(max = 32)
        private String agentStatus;
        @NotBlank
        @Size(max = 32)
        private String tunnelStatus;
        @NotBlank
        @Size(max = 32)
        private String localWebStatus;
        private Long ratholePid;
        @Min(0)
        private Integer restartCount;
        @Size(max = 64)
        private String lastErrorCode;
        @Size(max = 255)
        private String lastErrorMessage;
        @Size(max = 64)
        private String reportedAt;
    }

    @Data
    public static class TunnelConfigView {
        private String remoteAddr;
        private String serviceName;
        private String serviceToken;
        private String transport;
        private String remotePublicKey;
        private String localAddr;
    }

    @Data
    public static class AgentConfigView {
        private String agentId;
        private String agentToken;
        private Long configGeneration;
        private String desiredState;
        private Integer heartbeatIntervalSeconds;
        private TunnelConfigView tunnel;
    }

    @Data
    public static class HeartbeatView {
        private String desiredState;
        private Long configGeneration;
        private boolean configChanged;
        private Integer nextHeartbeatSeconds;
    }

    @Data
    public static class EndpointView {
        private boolean featureEnabled;
        private boolean configured;
        private String endpointId;
        private String deviceId;
        private String deviceName;
        private String desiredState;
        private String agentStatus;
        private String tunnelStatus;
        private String localWebStatus;
        private String routeStatus;
        private Long configGeneration;
        private boolean agentOnline;
        private Date lastHeartbeatAt;
        private String lastErrorCode;
        private String lastErrorMessage;
    }

    @Data
    public static class AccessSessionView {
        private String sessionId;
        private String accessUrl;
        private Date expiresAt;
    }

    @Data
    public static class RouteView {
        private String endpointId;
        private String serviceName;
        private String serviceToken;
        private String bindAddr;
        private Long configGeneration;
        private String desiredState;
    }

    @Data
    public static class RouteStatusRequest {
        @NotNull
        @Min(1)
        private Long configGeneration;
        @NotBlank
        @Size(max = 16)
        private String routeStatus;
        @Size(max = 255)
        private String errorMessage;
    }

    @Data
    public static class SessionRouteView {
        private String sessionId;
        private String endpointId;
        private String upstreamHost;
        private Integer upstreamPort;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        private Date expiresAt;
    }

    @Data
    public static class ApiResponse<T> {
        private int code;
        private String msg;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<T>();
            response.setCode(200);
            response.setMsg("操作成功");
            response.setData(data);
            return response;
        }

        public static <T> ApiResponse<T> error(int code, String message) {
            ApiResponse<T> response = new ApiResponse<T>();
            response.setCode(code);
            response.setMsg(message);
            return response;
        }
    }
}
