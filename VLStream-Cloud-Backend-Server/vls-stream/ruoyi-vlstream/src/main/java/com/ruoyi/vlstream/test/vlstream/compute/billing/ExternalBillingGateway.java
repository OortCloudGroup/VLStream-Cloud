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
    /** Implemented, explicitly enabled hold-to-charge transport. Target wallet verification is a separate gate. */
    boolean supportsAtomicCapture();
    default Result ledger(long fromSecond, long toSecond, String cursor) { return Result.of(Status.UNKNOWN); }
    default Result holdById(String holdId) { return Result.of(Status.UNKNOWN); }
    default Result holdByReference(String merchantHoldId) { return Result.of(Status.UNKNOWN); }
    default Result holdOperation(String operation, String businessReference) { return Result.of(Status.UNKNOWN); }
    default Result createHold(String originalKey, String immutableBody) { return Result.of(Status.UNKNOWN); }
    default Result topupHold(String holdId, String originalKey, String immutableBody) { return Result.of(Status.UNKNOWN); }
    default Result releaseHold(String holdId, String originalKey, String immutableBody) { return Result.of(Status.UNKNOWN); }
    default Result refund(String chargeId, String originalKey, String immutableBody) { return Result.of(Status.UNKNOWN); }
}
