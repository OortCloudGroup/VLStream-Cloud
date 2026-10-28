package com.ruoyi.web.controller.compat.tenant;

/** A locally established, explicit authentication failure, not a transport or parsing failure. */
public class PlatformSessionValidationException extends IllegalStateException {
    public enum Reason { SESSION_NOT_BOUND, PLATFORM_USER_CHANGED, PLATFORM_TENANT_CHANGED }
    private final Reason reason;

    public PlatformSessionValidationException(Reason reason) {
        super(reason == Reason.SESSION_NOT_BOUND ? "多租户平台会话不存在或已过期" : "平台会话身份已变更，请重新登录");
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
