package com.ridehail.pricing;

import com.ridehail.domain.enums.CarType;

public interface PricingStrategy {
    /** Base fare before any coupon or surge adjustments. */
    double price(double distanceKm, CarType carType);
}
