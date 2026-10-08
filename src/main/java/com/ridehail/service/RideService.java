package com.ridehail.service;

import com.ridehail.cancellation.CancellationPolicy;
import com.ridehail.domain.Coupon;
import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.domain.Ride;
import com.ridehail.domain.enums.CarType;
import com.ridehail.domain.enums.DriverStatus;
import com.ridehail.domain.enums.RideStatus;
import com.ridehail.matching.DriverMatchingStrategy;
import com.ridehail.pricing.PricingStrategy;
import com.ridehail.repository.DriverRepository;
import com.ridehail.repository.RideRepository;
import com.ridehail.util.GeoUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates booking, ending, and cancelling rides. All strategies are
 * injected — swapping pricing, matching, or cancellation never touches this
 * class.
 */
public class RideService {

    private final DriverRepository driverRepo;
    private final RideRepository rideRepo;
    private final PricingStrategy pricing;
    private final DriverMatchingStrategy matching;
    private final CouponService coupons;
    private final CancellationPolicy cancellationPolicy;
    private final double searchRadiusKm;
    private final Clock clock;

    public RideService(DriverRepository driverRepo,
                       RideRepository rideRepo,
                       PricingStrategy pricing,
                       DriverMatchingStrategy matching,
                       CouponService coupons,
                       CancellationPolicy cancellationPolicy,
                       double searchRadiusKm,
                       Clock clock) {
        this.driverRepo = driverRepo;
        this.rideRepo = rideRepo;
        this.pricing = pricing;
        this.matching = matching;
        this.coupons = coupons;
        this.cancellationPolicy = cancellationPolicy;
        this.searchRadiusKm = searchRadiusKm;
        this.clock = clock;
    }

    /**
     * Book a ride. Behaviour:
     * <ul>
     *   <li>Finds AVAILABLE drivers of {@code requestedCarType} within radius.</li>
     *   <li>If HATCHBACK requested and none found, retries with SEDAN and marks
     *       the ride as {@code upgraded=true} — billed at hatchback rates.</li>
     *   <li>Races on reservation are decided by {@link DriverRepository#tryReserve};
     *       losers skip to the next candidate.</li>
     * </ul>
     */
    public Ride bookRide(String userId, Location pickup, CarType requestedCarType, String couponCode) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId required");
        if (pickup == null) throw new IllegalArgumentException("pickup required");
        if (requestedCarType == null) throw new IllegalArgumentException("carType required");

        Reservation reservation = reserveDriver(pickup, requestedCarType);
        if (reservation == null) {
            throw new NoDriverAvailableException(
                    "No driver available for " + requestedCarType + " within " + searchRadiusKm + " km");
        }

        Ride ride = new Ride(
                "R-" + UUID.randomUUID().toString().substring(0, 8),
                userId,
                reservation.driver.getId(),
                requestedCarType,
                reservation.driver.getCarType(),
                reservation.upgraded,
                pickup,
                couponCode,
                Instant.now(clock)
        );
        rideRepo.save(ride);
        driverRepo.setStatus(reservation.driver.getId(), DriverStatus.ON_TRIP);
        return ride;
    }

    /**
     * Attempt to reserve a driver, honoring free hatchback→sedan upgrade.
     * Returns null if nothing available in either pass.
     */
    private Reservation reserveDriver(Location pickup, CarType requested) {
        Reservation r = tryReserveOfType(pickup, requested, false);
        if (r != null) return r;
        if (requested == CarType.HATCHBACK) {
            return tryReserveOfType(pickup, CarType.SEDAN, true);
        }
        return null;
    }

    private Reservation tryReserveOfType(Location pickup, CarType carType, boolean upgraded) {
        List<Driver> pool = driverRepo.findAvailableNear(pickup, carType, searchRadiusKm);
        while (!pool.isEmpty()) {
            Optional<Driver> pick = matching.pick(pickup, pool);
            if (pick.isEmpty()) return null;
            Driver d = pick.get();
            if (driverRepo.tryReserve(d.getId())) {
                return new Reservation(d, upgraded);
            }
            pool.remove(d);
        }
        return null;
    }

    public Ride endRide(String rideId, Location dropoff) {
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new NoSuchElementException("ride not found: " + rideId));
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new IllegalStateException("ride is not ongoing: " + ride.getStatus());
        }
        double distanceKm = GeoUtil.distanceKm(ride.getPickup(), dropoff);
        // Bill at the car type the user requested — hatchback upgrades are free.
        double baseFare = pricing.price(distanceKm, ride.getRequestedCarType());

        double discount = 0.0;
        if (ride.getCouponCode() != null) {
            Optional<Coupon> c = coupons.findApplicable(ride.getCouponCode(), clock.instant().atZone(zoneId()).toLocalDate());
            if (c.isPresent()) {
                discount = c.get().discountFor(baseFare);
            }
        }
        double finalFare = Math.max(0.0, baseFare - discount);

        ride.setDropoff(dropoff);
        ride.setDistanceKm(distanceKm);
        ride.setBaseFare(baseFare);
        ride.setDiscount(discount);
        ride.setFinalFare(finalFare);
        ride.setEndedAt(Instant.now(clock));
        ride.setStatus(RideStatus.COMPLETED);
        driverRepo.setStatus(ride.getDriverId(), DriverStatus.AVAILABLE);
        return ride;
    }

    public Ride cancelRide(String rideId) {
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new NoSuchElementException("ride not found: " + rideId));
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new IllegalStateException("ride is not ongoing: " + ride.getStatus());
        }
        double fee = cancellationPolicy.feeFor(ride);
        ride.setCancellationFee(fee);
        ride.setFinalFare(fee);
        ride.setEndedAt(Instant.now(clock));
        ride.setStatus(RideStatus.CANCELLED);
        driverRepo.setStatus(ride.getDriverId(), DriverStatus.AVAILABLE);
        return ride;
    }

    public List<Ride> userHistory(String userId, RideStatus status) {
        return rideRepo.findByUser(userId, status);
    }

    public List<Ride> driverHistory(String driverId, RideStatus status) {
        return rideRepo.findByDriver(driverId, status);
    }

    private ZoneId zoneId() {
        return clock.getZone() != null ? clock.getZone() : ZoneId.systemDefault();
    }

    private record Reservation(Driver driver, boolean upgraded) {}

    public static class NoDriverAvailableException extends RuntimeException {
        public NoDriverAvailableException(String msg) { super(msg); }
    }
}
