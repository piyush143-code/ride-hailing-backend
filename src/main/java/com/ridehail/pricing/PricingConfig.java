package com.ridehail.pricing;

import com.ridehail.domain.enums.CarType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Per-car-type pricing configuration. A tier is {@code upToKm} inclusive and
 * {@code ratePerKm}; the last tier should use {@link Double#POSITIVE_INFINITY}
 * for its {@code upToKm} to catch all remaining distance.
 */
public class PricingConfig {

    public record Tier(double upToKm, double ratePerKm) {}

    public record CarPricing(double minFare, List<Tier> tiers) {
        public CarPricing {
            if (tiers == null || tiers.isEmpty()) {
                throw new IllegalArgumentException("tiers must be non-empty");
            }
            double prev = 0;
            for (Tier t : tiers) {
                if (t.upToKm() <= prev) {
                    throw new IllegalArgumentException(
                            "tier upToKm must be strictly increasing, got " + t.upToKm() + " after " + prev);
                }
                prev = t.upToKm();
            }
        }
    }

    private final Map<CarType, CarPricing> byCarType;

    public PricingConfig(Map<CarType, CarPricing> byCarType) {
        this.byCarType = new EnumMap<>(byCarType);
    }

    public CarPricing forCar(CarType carType) {
        CarPricing cp = byCarType.get(carType);
        if (cp == null) {
            throw new IllegalStateException("No pricing configured for " + carType);
        }
        return cp;
    }

    /**
     * Defaults from the PDF for Sedan (min ₹50; 0–2km @10, 3–5km @8, 6+ @5).
     * Hatchback is a slightly cheaper tier so the free-upgrade invariant is
     * meaningful (hatchback price ≤ sedan price for any distance).
     */
    public static PricingConfig defaults() {
        Map<CarType, CarPricing> m = new EnumMap<>(CarType.class);
        m.put(CarType.SEDAN, new CarPricing(50.0, List.of(
                new Tier(2.0, 10.0),
                new Tier(5.0, 8.0),
                new Tier(Double.POSITIVE_INFINITY, 5.0)
        )));
        m.put(CarType.HATCHBACK, new CarPricing(40.0, List.of(
                new Tier(2.0, 8.0),
                new Tier(5.0, 6.0),
                new Tier(Double.POSITIVE_INFINITY, 4.0)
        )));
        return new PricingConfig(m);
    }
}
