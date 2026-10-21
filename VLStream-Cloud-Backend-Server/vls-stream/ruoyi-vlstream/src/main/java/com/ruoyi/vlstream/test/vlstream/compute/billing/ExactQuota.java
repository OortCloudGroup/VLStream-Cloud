package com.ruoyi.vlstream.test.vlstream.compute.billing;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Exact nonnegative quota fraction; no per-tick decimal division or floating point. */
public final class ExactQuota {
    public static final ExactQuota ZERO = new ExactQuota(BigInteger.ZERO, BigInteger.ONE);
    public final BigInteger numerator;
    public final BigInteger denominator;

    public ExactQuota(BigInteger numerator, BigInteger denominator) {
        if (numerator.signum() < 0 || denominator.signum() <= 0) throw new IllegalArgumentException("Invalid quota fraction");
        BigInteger gcd = numerator.gcd(denominator);
        this.numerator = numerator.divide(gcd);
        this.denominator = denominator.divide(gcd);
    }

    public static BigInteger integer(String text, boolean positive) {
        if (text == null || text.length() > 19 || !text.matches("0|[1-9][0-9]*")) throw new IllegalArgumentException("Invalid quota integer");
        BigInteger value = new BigInteger(text);
        if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 || (positive && value.signum() == 0))
            throw new IllegalArgumentException("Quota integer outside int64 range");
        return value;
    }

    public static ExactQuota compute(BigDecimal approvedHourlyPrice, long millis, String conversionNumerator, String conversionDenominator) {
        if (approvedHourlyPrice == null || approvedHourlyPrice.signum() <= 0 || millis < 0 || Math.abs((long) approvedHourlyPrice.scale()) > 32)
            throw new IllegalArgumentException("Invalid approved hourly price or duration");
        int scale = approvedHourlyPrice.scale();
        BigInteger n = approvedHourlyPrice.unscaledValue().multiply(BigInteger.valueOf(millis)).multiply(integer(conversionNumerator, true));
        BigInteger d = BigInteger.valueOf(3600000).multiply(integer(conversionDenominator, true));
        if (scale >= 0) d = d.multiply(BigInteger.TEN.pow(scale));
        else n = n.multiply(BigInteger.TEN.pow(-scale));
        return new ExactQuota(n, d);
    }

    public ExactQuota add(ExactQuota other) {
        return new ExactQuota(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)), denominator.multiply(other.denominator));
    }

    public String floor() {
        String result = numerator.divide(denominator).toString();
        integer(result, false);
        return result;
    }

    public ExactQuota remainder() { return new ExactQuota(numerator.remainder(denominator), denominator); }
    @Override public String toString() { return numerator + "/" + denominator; }
}
