package com.ridehail.pricing;

import com.ridehail.domain.enums.CarType;

/**
 * Walks the per-car tiers accumulating cost from 0 to {@code distanceKm}, then
 * applies the car's minimum fare as a floor.
 *
 * <p>Example (default Sedan config, PDF spec):
 * <ul>
 *   <li>0.5 km → max(0.5 × 10, 50) = ₹50 (min fare)</li>
 *   <li>4 km   → 2×10 + 2×8 = ₹36 → floored to ₹50</li>
 *   <li>6 km   → 2×10 + 3×8 + 1×5 = ₹49 → floored to ₹50</li>
 *   <li>10 km  → 2×10 + 3×8 + 5×5 = ₹69</li>
 * </ul>
 */
public class TieredPricingStrategy implements PricingStrategy {

    private final PricingConfig config;

    public TieredPricingStrategy(PricingConfig config) {
        this.config = config;
    }

    @Override
    public double price(double distanceKm, CarType carType) {
        if (distanceKm < 0) throw new IllegalArgumentException("distanceKm must be >= 0");
        PricingConfig.CarPricing cp = config.forCar(carType);

        double remaining = distanceKm;
        double cost = 0.0;
        double tierStart = 0.0;

        for (PricingConfig.Tier t : cp.tiers()) {
            if (remaining <= 0) break;
            double tierSpan = t.upToKm() - tierStart;
            double chunk = Math.min(remaining, tierSpan);
            cost += chunk * t.ratePerKm();
            remaining -= chunk;
            tierStart = t.upToKm();
        }

        return Math.max(cost, cp.minFare());
    }
}
