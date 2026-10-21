package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;

/** Backend result of /api/user/self using the SAME SSO proof already verified for this actor.
 * No controller accepts this object or a frontend payer id. Tokens are not retained. */
public final class BillingPayer {
    public final String userId;
    public final String actorId;
    public final String tenantId;
    public final String platformSubject;

    private BillingPayer(String userId, ComputePlatformAuthorization.Context actor) {
        this.userId = userId; this.actorId = actor.getVlsUserId();
        this.tenantId = actor.getVlsTenantId(); this.platformSubject = actor.getPlatformUserId();
    }

    public static BillingPayer fromVerifiedSelf(ComputePlatformAuthorization.Context actor, JsonNode response) {
        if (actor == null || !actor.isAuthorized() || blank(actor.getVlsUserId()) || blank(actor.getVlsTenantId()) || blank(actor.getPlatformUserId()))
            throw new IllegalArgumentException("Verified local actor and SSO identity required");
        if (response == null || (response.has("success") && !response.path("success").asBoolean())) throw new IllegalArgumentException("User self lookup failed");
        JsonNode self = response.has("data") ? response.get("data") : response;
        JsonNode id = self == null ? null : self.get("id");
        if (id == null || !(id.isTextual() || id.isIntegralNumber())) throw new IllegalArgumentException("Authoritative user id required");
        String userId = id.asText(); ExactQuota.integer(userId, true);
        return new BillingPayer(userId, actor);
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
