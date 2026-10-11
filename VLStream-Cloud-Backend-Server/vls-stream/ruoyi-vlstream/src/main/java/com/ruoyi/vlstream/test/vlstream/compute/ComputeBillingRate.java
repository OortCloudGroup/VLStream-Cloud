package com.ruoyi.vlstream.test.vlstream.compute;

import java.math.*;
import java.time.Instant;
import lombok.Data;

/** Immutable JSON snapshot per usage, including currency/unit/scope/time and quota conversion version. */
@Data
public class ComputeBillingRate {
    private String version;
    private String costSource; // VERIFIED_MANUAL or ACTUAL_API; reference quote/missing can't activate paid use.
    private String currency;
    private String costScope; // INSTANCE_COMPUTE; never multiply by the registered card count again.
    private String costUnit; // PER_INSTANCE_HOUR
    private BigDecimal costHourly;
    private BigDecimal saleMultiplier = new BigDecimal("2");
    private String validFrom;
    private String validUntil;
    private String quotaUnit;
    private String conversionVersion;
    private BigDecimal quotaPerCurrencyUnit;
    private Integer quotaScale;
    private RoundingMode quotaRounding;
    // Provider-confirmed behavior, never inferred from the vendor name (ordinary AutoDL FAQ is not Pro/ECS).
    private String pricePolicy = "UNVERIFIED"; // LOCKED_FOR_RUN or SEGMENTED_NOTICE
    private boolean providerPriceLockConfirmed;
    private String priceEvidence;

    public void validate(Instant at) {
        if (!("LOCKED_FOR_RUN".equals(pricePolicy) || "SEGMENTED_NOTICE".equals(pricePolicy))
            || priceEvidence == null || priceEvidence.trim().isEmpty()
            || ("LOCKED_FOR_RUN".equals(pricePolicy) && !providerPriceLockConfirmed))
            throw new IllegalArgumentException("供应商锁价/调价生效规则尚未核验，禁止推断锁价或自动加价");
        if (version==null || !("VERIFIED_MANUAL".equals(costSource)||"ACTUAL_API".equals(costSource))
            || currency==null || !currency.matches("[A-Z]{3}") || !"INSTANCE_COMPUTE".equals(costScope) || !"PER_INSTANCE_HOUR".equals(costUnit)
            || costHourly==null || costHourly.signum()<=0 || saleMultiplier==null || saleMultiplier.compareTo(new BigDecimal("2"))!=0
            || quotaUnit==null || quotaUnit.trim().isEmpty() || conversionVersion==null || quotaPerCurrencyUnit==null || quotaPerCurrencyUnit.signum()<=0
            || quotaScale==null || quotaScale<0 || quotaScale>12 || quotaRounding==null || quotaRounding==RoundingMode.UNNECESSARY)
            throw new IllegalArgumentException("可信实例小时成本、成本口径、额度单位和换算/舍入策略尚未确认");
        try { if (at.isBefore(Instant.parse(validFrom)) || !at.isBefore(Instant.parse(validUntil))) throw new IllegalArgumentException(); }
        catch (RuntimeException e) { throw new IllegalArgumentException("成本费率未生效或已过期"); }
    }
    public BigDecimal unroundedQuota(long elapsedMillis) {
        if (elapsedMillis<0) throw new IllegalArgumentException("Invalid duration");
        // Seconds = milliseconds / 1000. No per-tick rounding, no ceiling to minutes/hours.
        return costHourly.multiply(saleMultiplier).multiply(quotaPerCurrencyUnit).multiply(BigDecimal.valueOf(elapsedMillis))
            .divide(BigDecimal.valueOf(3600000),MathContext.DECIMAL128);
    }
}
