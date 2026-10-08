package com.ridehail.domain;

import java.time.LocalDate;

public class Coupon {

    public enum DiscountType { FLAT, PERCENT }

    private final String code;
    private final DiscountType type;
    private final double value;
    private final LocalDate expiry;

    public Coupon(String code, DiscountType type, double value, LocalDate expiry) {
        if (type == DiscountType.PERCENT && (value < 0 || value > 100)) {
            throw new IllegalArgumentException("Percent discount must be in [0, 100], was " + value);
        }
        if (type == DiscountType.FLAT && value < 0) {
            throw new IllegalArgumentException("Flat discount must be >= 0, was " + value);
        }
        this.code = code;
        this.type = type;
        this.value = value;
        this.expiry = expiry;
    }

    public String getCode() { return code; }
    public DiscountType getType() { return type; }
    public double getValue() { return value; }
    public LocalDate getExpiry() { return expiry; }

    public boolean isExpired(LocalDate today) {
        return expiry != null && today.isAfter(expiry);
    }

    public double discountFor(double fare) {
        if (fare <= 0) return 0;
        return switch (type) {
            case FLAT -> Math.min(value, fare);
            case PERCENT -> fare * value / 100.0;
        };
    }

    @Override
    public String toString() {
        return "Coupon{" + code + ", " + type + ", value=" + value
                + (expiry != null ? ", expires=" + expiry : "") + "}";
    }
}
