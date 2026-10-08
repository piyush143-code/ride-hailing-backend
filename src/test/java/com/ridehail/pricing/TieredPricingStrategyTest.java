package com.ridehail.pricing;

import com.ridehail.domain.enums.CarType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers PDF spec:
 *   Sedan min ₹50; 0–2 km @10, 2–5 km @8, 5+ @5.
 *   Hatchback cheaper than sedan at every distance (free-upgrade invariant).
 */
class TieredPricingStrategyTest {

    private static final double EPS = 1e-9;

    private final TieredPricingStrategy strategy =
            new TieredPricingStrategy(PricingConfig.defaults());

    @Test
    void appliesMinimumFareForShortSedanRide() {
        // 0.5 km → 5, but min fare 50
        assertEquals(50.0, strategy.price(0.5, CarType.SEDAN), EPS);
        // 0 km → still min fare
        assertEquals(50.0, strategy.price(0.0, CarType.SEDAN), EPS);
    }

    @Test
    void firstTierExactBoundarySedan() {
        // 2 km × 10 = 20 → floored to 50
        assertEquals(50.0, strategy.price(2.0, CarType.SEDAN), EPS);
    }

    @Test
    void secondTierExactBoundarySedan() {
        // 2*10 + 3*8 = 44 → floored to 50
        assertEquals(50.0, strategy.price(5.0, CarType.SEDAN), EPS);
    }

    @Test
    void thirdTierAppliesSedan() {
        // 6 km: 2*10 + 3*8 + 1*5 = 49 → floored to 50
        assertEquals(50.0, strategy.price(6.0, CarType.SEDAN), EPS);
        // 10 km: 2*10 + 3*8 + 5*5 = 69
        assertEquals(69.0, strategy.price(10.0, CarType.SEDAN), EPS);
        // 20 km: 2*10 + 3*8 + 15*5 = 119
        assertEquals(119.0, strategy.price(20.0, CarType.SEDAN), EPS);
    }

    @Test
    void hatchbackPriceAlwaysLessThanOrEqualToSedan() {
        for (double km : new double[]{0.1, 1.0, 2.0, 3.5, 5.0, 7.0, 10.0, 25.0}) {
            double h = strategy.price(km, CarType.HATCHBACK);
            double s = strategy.price(km, CarType.SEDAN);
            assertEquals(true, h <= s, "hatchback (" + h + ") must be <= sedan (" + s + ") @ " + km + "km");
        }
    }

    @Test
    void hatchbackUsesItsOwnRates() {
        // Hatchback 10 km: 2*8 + 3*6 + 5*4 = 54
        assertEquals(54.0, strategy.price(10.0, CarType.HATCHBACK), EPS);
    }

    @Test
    void rejectsNegativeDistance() {
        assertThrows(IllegalArgumentException.class, () -> strategy.price(-1.0, CarType.SEDAN));
    }
}
