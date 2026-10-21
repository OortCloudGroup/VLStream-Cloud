package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;

/** Typed external boundary. A successful fake never establishes production wallet capability. */
public interface ExternalBillingGateway {
    enum Status { SUCCEEDED, PROCESSING, REJECTED, NOT_FOUND, UNKNOWN, NOT_AUTHORIZED, CONFLICT }

    final class Result {
        public final Status status;
        public final String errorCode;
        public final JsonNode charge;
        public Result(Status status, String errorCode, JsonNode charge) {
            this.status = status; this.errorCode = errorCode; this.charge = charge;
        }
        public static Result of(Status status) { return new Result(status, "", null); }
    }

    Result byKey(String key);
    Result byReference(String reference);
    Result balance(String verifiedUserId);
    Result capture(String originalKey, String immutableBody);
    /** Must mean an implemented, verified atomic hold-to-charge operation, not separate release+charge. */
    boolean supportsAtomicCapture();
    default Result ledger(long fromSecond, long toSecond, String cursor) { return Result.of(Status.UNKNOWN); }
}
