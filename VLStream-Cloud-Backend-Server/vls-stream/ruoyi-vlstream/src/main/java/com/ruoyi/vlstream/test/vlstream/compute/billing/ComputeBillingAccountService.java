package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.springframework.stereotype.Service;

/** Current-user wallet projection. Never accepts a payer id or returns an upstream response. */
@Service
public class ComputeBillingAccountService {
    @Getter
    public static final class Account {
        private final String userId;
        private final String balanceQuota;
        private final String heldQuota;
        private final String availableQuota;
        private final String balanceVersion;
        private final String quotaUnit;
        private final String currency;
        private final String numerator;
        private final String denominator;
        private final String conversionVersion;

        private Account(BillingPayer payer, JsonNode config, JsonNode balance) {
            userId = payer.userId;
            balanceQuota = amount(balance, "balance_quota");
            heldQuota = amount(balance, "held_quota");
            availableQuota = amount(balance, "available_quota");
            if (!ExactQuota.integer(balanceQuota, false).subtract(ExactQuota.integer(heldQuota, false))
                .equals(ExactQuota.integer(availableQuota, false))) throw new IllegalArgumentException("Inconsistent wallet counters");
            JsonNode version = balance.get("balance_version");
            if (version == null || !(version.isIntegralNumber() || version.isTextual()))
                throw new IllegalArgumentException("Exact balance version required");
            balanceVersion = ExactQuota.integer(version.asText(), false).toString();
            quotaUnit = BillingSnapshot.required(config, "quota_unit");
            currency = BillingSnapshot.required(config, "settlement_currency");
            conversionVersion = BillingSnapshot.required(config, "conversion_version");
            numerator = ExactQuota.integer(BillingSnapshot.required(config.path("quota_per_currency_unit"), "numerator"), true).toString();
            denominator = ExactQuota.integer(BillingSnapshot.required(config.path("quota_per_currency_unit"), "denominator"), true).toString();
        }
    }

    /** Called only by an explicit current-user action accepting the upstream trusted-app scopes.
     * A balance request can create an ACTIVE grant; it must never run as an automatic page GET. */
    public Account check(BillingPayer payer, ExternalBillingGateway gateway, JsonNode config, boolean acceptedPermissions) {
        if (!acceptedPermissions) throw new IllegalArgumentException("Billing permission acceptance required before wallet lookup");
        if (payer == null || gateway == null) throw new IllegalArgumentException("Verified current payer required");
        JsonNode actual = config;
        if (actual != null && actual.has("success")) {
            if (!actual.path("success").isBoolean() || !actual.path("success").booleanValue())
                throw new IllegalArgumentException("Billing configuration unavailable");
            actual = actual.get("data");
        }
        if (actual == null || !actual.isObject()) throw new IllegalArgumentException("Billing configuration unavailable");
        // Validate the public conversion before any potentially grant-producing request.
        BillingSnapshot.required(actual, "quota_unit");
        BillingSnapshot.required(actual, "settlement_currency");
        BillingSnapshot.required(actual, "conversion_version");
        ExactQuota.integer(BillingSnapshot.required(actual.path("quota_per_currency_unit"), "numerator"), true);
        ExactQuota.integer(BillingSnapshot.required(actual.path("quota_per_currency_unit"), "denominator"), true);
        ExternalBillingGateway.Result result = gateway.balance(payer.userId);
        if (result == null || result.status != ExternalBillingGateway.Status.SUCCEEDED || result.charge == null)
            throw new IllegalStateException(result != null && result.status == ExternalBillingGateway.Status.NOT_AUTHORIZED
                ? "BILLING_NOT_AUTHORIZED" : "BILLING_BALANCE_UNCONFIRMED");
        if (!payer.userId.equals(result.charge.path("user_id").asText())) throw new IllegalArgumentException("Wallet payer mismatch");
        return new Account(payer, actual, result.charge);
    }

    private static String amount(JsonNode node, String name) {
        return ExactQuota.integer(BillingSnapshot.required(node, name), false).toString();
    }
}
