package com.ruoyi.web.controller.compat.tenant;

/** Retains safe protocol facts without storing an upstream response or credential in the message. */
public class PlatformTenantCallException extends IllegalStateException {
    public enum Kind { CONFIGURATION, HTTP_STATUS, BUSINESS_CODE, RESPONSE_FORMAT, TRANSPORT }

    private final Kind kind;
    private final Integer httpStatus;
    private final Integer businessCode;

    public PlatformTenantCallException(Kind kind, Integer httpStatus, Integer businessCode, Throwable cause) {
        super("平台身份接口调用失败: " + kind.name(), cause);
        this.kind = kind;
        this.httpStatus = httpStatus;
        this.businessCode = businessCode;
    }

    public Kind getKind() { return kind; }
    public Integer getHttpStatus() { return httpStatus; }
    public Integer getBusinessCode() { return businessCode; }

    public int responseCode() {
        if (Integer.valueOf(401).equals(httpStatus)) return 401;
        if (Integer.valueOf(403).equals(httpStatus)) return 403;
        if (kind == Kind.BUSINESS_CODE && businessCode != null) {
            if (businessCode == 401 || businessCode == 4004 || businessCode == 4444) return businessCode;
            if (businessCode == 403) return 403;
        }
        return kind == Kind.TRANSPORT || kind == Kind.CONFIGURATION ? 503 : 502;
    }
}
