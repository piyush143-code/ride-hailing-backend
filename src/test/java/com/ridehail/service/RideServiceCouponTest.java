package com.ridehail.service;

import com.ridehail.domain.Coupon;
import com.ridehail.domain.Location;
import com.ridehail.domain.Ride;
import com.ridehail.domain.enums.CarType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RideServiceCouponTest {

    private final TestFactory.Rig rig = new TestFactory.Rig();
    private final Location pickup = new Location(12.9716, 77.5946);
    private final Location dropoff10km = new Location(12.9716 + 10.0 / 111.0, 77.5946);

    @Test
    void appliesFlatCoupon() {
        rig.driverService.register("S1", "1", CarType.SEDAN, new Location(12.9720, 77.5950));
        rig.couponService.add("FLAT20", Coupon.DiscountType.FLAT, 20.0, null);

        Ride r = rig.rideService.bookRide("U1", pickup, CarType.SEDAN, "FLAT20");
        Ride done = rig.rideService.endRide(r.getId(), dropoff10km);

        // Sedan 10 km = 69, less 20 = 49
        assertEquals(69.0, done.getBaseFare(), 0.5);
        assertEquals(20.0, done.getDiscount(), 1e-9);
        assertEquals(49.0, done.getFinalFare(), 0.5);
    }

    @Test
    void appliesPercentCoupon() {
        rig.driverService.register("S1", "1", CarType.SEDAN, new Location(12.9720, 77.5950));
        rig.couponService.add("PCT10", Coupon.DiscountType.PERCENT, 10.0, null);

        Ride r = rig.rideService.bookRide("U1", pickup, CarType.SEDAN, "PCT10");
        Ride done = rig.rideService.endRide(r.getId(), dropoff10km);

        // 69 - 10% = 62.1
        assertEquals(6.9, done.getDiscount(), 0.2);
        assertEquals(62.1, done.getFinalFare(), 0.5);
    }

    @Test
    void silentlyIgnoresUnknownCoupon() {
        rig.driverService.register("S1", "1", CarType.SEDAN, new Location(12.9720, 77.5950));

        Ride r = rig.rideService.bookRide("U1", pickup, CarType.SEDAN, "DOES_NOT_EXIST");
        Ride done = rig.rideService.endRide(r.getId(), dropoff10km);

        // Full fare, no discount.
        assertEquals(0.0, done.getDiscount(), 1e-9);
        assertEquals(69.0, done.getFinalFare(), 0.5);
    }

    @Test
    void expiredCouponIsNotApplied() {
        // Fixed clock is 2026-10-08; set expiry before that.
        rig.driverService.register("S1", "1", CarType.SEDAN, new Location(12.9720, 77.5950));
        rig.couponService.add("OLD", Coupon.DiscountType.FLAT, 50.0, LocalDate.parse("2026-01-01"));

        Ride r = rig.rideService.bookRide("U1", pickup, CarType.SEDAN, "OLD");
        Ride done = rig.rideService.endRide(r.getId(), dropoff10km);

        assertEquals(0.0, done.getDiscount(), 1e-9);
        assertEquals(69.0, done.getFinalFare(), 0.5);
    }

    @Test
    void flatCouponCappedAtFare() {
        rig.driverService.register("S1", "1", CarType.SEDAN, new Location(12.9720, 77.5950));
        rig.couponService.add("HUGE", Coupon.DiscountType.FLAT, 10_000, null);

        Ride r = rig.rideService.bookRide("U1", pickup, CarType.SEDAN, "HUGE");
        Ride done = rig.rideService.endRide(r.getId(), dropoff10km);

        // Final fare cannot go negative; max discount is the fare itself.
        assertEquals(0.0, done.getFinalFare(), 1e-9);
    }
}
