package com.ridehail.cancellation;

import com.ridehail.domain.Ride;

/**
 * Charges a flat fee if the ride had a driver assigned at cancellation time.
 * In this in-memory model, having a {@code driverId} on the ride means a driver
 * was reserved — so every cancellation of an ongoing ride incurs the fee.
 * Zero if the ride was never booked.
 */
public class FlatFeeCancellationPolicy implements CancellationPolicy {

    private final double fee;

    public FlatFeeCancellationPolicy(double fee) {
        this.fee = fee;
    }

    @Override
    public double feeFor(Ride ride) {
        return ride.getDriverId() != null ? fee : 0.0;
    }
}
