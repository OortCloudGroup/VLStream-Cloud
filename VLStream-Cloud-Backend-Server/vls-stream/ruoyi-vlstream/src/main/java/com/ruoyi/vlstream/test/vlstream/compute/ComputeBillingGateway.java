package com.ruoyi.vlstream.test.vlstream.compute;

import java.math.BigDecimal;

/** New API accounting contract, NOT an LLM request and NOT a guessed debit endpoint.
 * Implementations must atomically enforce balance, deduplicate debit keys and query uncertain outcomes.
 * accountRef is the backend-verified stable SSO subject: platform user equals New API personal user.
 * It is never a client-selected payer or a tenant balance. No independent account mapping is required.
 * quotaUnit names a measurement, never a credential token.
 * No implementation is installed until the site's authorized accounting API is supplied. */
public interface ComputeBillingGateway {
    enum Outcome { SUCCEEDED, FAILED, UNKNOWN }
    BigDecimal availableQuota(String accountRef, String quotaUnit);
    Outcome debit(String accountRef, String quotaUnit, BigDecimal amount, String idempotencyKey);
    Outcome lookup(String accountRef, String idempotencyKey);
}
