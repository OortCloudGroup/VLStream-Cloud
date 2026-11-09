package com.ruoyi.vlstream.test.vlstream.compute;

/** Safe normalized errors. Never retain the vendor response body, password, token or URL. */
public class ComputeProviderException extends RuntimeException {
    public enum Kind { NO_STOCK, AUTHORIZATION, BALANCE, QUOTA, LOCKED, WRONG_STATE, UNKNOWN }
    private final Kind kind;
    private final Integer httpStatus;
    private final String diagnostic;
    private final String providerRequestId;
    private final String providerCode;
    public ComputeProviderException(Kind kind) { this(kind, null, "UNCLASSIFIED"); }
    public ComputeProviderException(Kind kind, Integer httpStatus, String diagnostic) {
        this(kind, httpStatus, diagnostic, null);
    }
    public ComputeProviderException(Kind kind, Integer httpStatus, String diagnostic, String providerRequestId) {
        this(kind, httpStatus, diagnostic, providerRequestId, null);
    }
    public ComputeProviderException(Kind kind, Integer httpStatus, String diagnostic, String providerRequestId, String providerCode) {
        super(message(kind)); this.kind = kind; this.httpStatus = httpStatus;
        this.diagnostic = diagnostic != null && diagnostic.matches("(?:UNCLASSIFIED|BUSINESS_(?:BAD_REQUEST|AUTHORIZATION|NOT_FOUND|OTHER)|HTTP_ERROR|EMPTY_RESPONSE|INVALID_RESPONSE|TIMEOUT|DNS|TLS|TRANSPORT)") ? diagnostic : "UNCLASSIFIED";
        // AutoDL documents an opaque hexadecimal request_id for supplier-side tracing.
        this.providerRequestId = providerRequestId != null && providerRequestId.matches("[a-fA-F0-9]{16,64}") ? providerRequestId : null;
        // The provider boundary must first exclude its active credential or echoed substrings.
        this.providerCode = providerCode != null && providerCode.matches("[A-Za-z][A-Za-z0-9_]{0,63}") ? providerCode : null;
    }
    public Kind getKind() { return kind; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getDiagnostic() { return diagnostic; }
    public String getProviderRequestId() { return providerRequestId; }
    public String getProviderCode() { return providerCode; }
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
