package com.ruoyi.vlstream.test.vlstream.compute;

/** Safe normalized errors. Never retain the vendor response body, password, token or URL. */
public class ComputeProviderException extends RuntimeException {
    public enum Kind { NO_STOCK, AUTHORIZATION, BALANCE, QUOTA, LOCKED, WRONG_STATE, UNKNOWN }
    private final Kind kind;
    public ComputeProviderException(Kind kind) { super(message(kind)); this.kind = kind; }
    public Kind getKind() { return kind; }
    public static Kind aliyunKind(String code) {
        if ("InvalidAccessKeyId.NotFound".equals(code) || "SignatureDoesNotMatch".equals(code)
            || "Forbidden.RAM".equals(code) || "Forbidden".equals(code)) return Kind.AUTHORIZATION;
        if ("OperationDenied.NoStock".equals(code) || "Invalid.PrivatePoolOptions.NoStock".equals(code)) return Kind.NO_STOCK;
        if ("InsufficientBalance".equals(code)) return Kind.BALANCE;
        if ("QuotaExceed.ElasticQuota".equals(code)) return Kind.QUOTA;
        if ("InstanceLockedForSecurity".equals(code) || "OperationDenied.HostRepairing".equals(code)) return Kind.LOCKED;
        if ("IncorrectInstanceStatus".equals(code)) return Kind.WRONG_STATE;
        return Kind.UNKNOWN;
    }
    private static String message(Kind kind) {
        switch (kind) {
            case NO_STOCK: return "厂商确认 GPU 无库存，请稍后刷新重试";
            case AUTHORIZATION: return "厂商授权不足，请联系实例管理员";
            case BALANCE: return "厂商账户余额不足，请联系实例管理员";
            case QUOTA: return "厂商资源配额不足，请联系实例管理员";
            case LOCKED: return "厂商实例锁定或维护中，请稍后核对";
            case WRONG_STATE: return "厂商实例状态已变化，请刷新核对";
            default: return "厂商操作结果未知，请核对控制台，勿重复开关机";
        }
    }
}
