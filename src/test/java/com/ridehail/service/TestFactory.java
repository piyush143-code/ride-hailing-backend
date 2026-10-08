package com.ridehail.service;

import com.ridehail.cancellation.FlatFeeCancellationPolicy;
import com.ridehail.matching.NearestDriverStrategy;
import com.ridehail.pricing.PricingConfig;
import com.ridehail.pricing.TieredPricingStrategy;
import com.ridehail.repository.CouponRepository;
import com.ridehail.repository.DriverRepository;
import com.ridehail.repository.RideRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Shared harness for service-level tests. */
final class TestFactory {

    static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneOffset.UTC);

    private TestFactory() {}

    static class Rig {
        final DriverRepository driverRepo = new DriverRepository();
        final RideRepository rideRepo = new RideRepository();
        final CouponRepository couponRepo = new CouponRepository();
        final CouponService couponService = new CouponService(couponRepo);
        final RideService rideService = new RideService(
                driverRepo,
                rideRepo,
                new TieredPricingStrategy(PricingConfig.defaults()),
                new NearestDriverStrategy(),
                couponService,
                new FlatFeeCancellationPolicy(30.0),
                5.0,
                FIXED_CLOCK
        );
        final DriverService driverService = new DriverService(driverRepo);
        final UserService userService = new UserService(new com.ridehail.repository.UserRepository());
    }
}
