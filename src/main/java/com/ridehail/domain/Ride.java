package com.ridehail.domain;

import com.ridehail.domain.enums.CarType;
import com.ridehail.domain.enums.RideStatus;

import java.time.Instant;

public class Ride {
    private final String id;
    private final String userId;
    private final String driverId;
    private final CarType requestedCarType;
    private final CarType assignedCarType;
    private final boolean upgraded;
    private final Location pickup;
    private final String couponCode;
    private final Instant startedAt;

    private Location dropoff;
    private double distanceKm;
    private double baseFare;
    private double discount;
    private double finalFare;
    private double cancellationFee;
    private Instant endedAt;
    private RideStatus status;

    public Ride(String id,
                String userId,
                String driverId,
                CarType requestedCarType,
                CarType assignedCarType,
                boolean upgraded,
                Location pickup,
                String couponCode,
                Instant startedAt) {
        this.id = id;
        this.userId = userId;
        this.driverId = driverId;
        this.requestedCarType = requestedCarType;
        this.assignedCarType = assignedCarType;
        this.upgraded = upgraded;
        this.pickup = pickup;
        this.couponCode = couponCode;
        this.startedAt = startedAt;
        this.status = RideStatus.ONGOING;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getDriverId() { return driverId; }
    public CarType getRequestedCarType() { return requestedCarType; }
    public CarType getAssignedCarType() { return assignedCarType; }
    public boolean isUpgraded() { return upgraded; }
    public Location getPickup() { return pickup; }
    public String getCouponCode() { return couponCode; }
    public Instant getStartedAt() { return startedAt; }
    public Location getDropoff() { return dropoff; }
    public double getDistanceKm() { return distanceKm; }
    public double getBaseFare() { return baseFare; }
    public double getDiscount() { return discount; }
    public double getFinalFare() { return finalFare; }
    public double getCancellationFee() { return cancellationFee; }
    public Instant getEndedAt() { return endedAt; }
    public RideStatus getStatus() { return status; }

    public void setDropoff(Location dropoff) { this.dropoff = dropoff; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }
    public void setDiscount(double discount) { this.discount = discount; }
    public void setFinalFare(double finalFare) { this.finalFare = finalFare; }
    public void setCancellationFee(double cancellationFee) { this.cancellationFee = cancellationFee; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public void setStatus(RideStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Ride{" + id + ", user=" + userId + ", driver=" + driverId
                + ", " + assignedCarType + (upgraded ? " (upgraded)" : "")
                + ", " + status
                + ", distance=" + distanceKm + "km"
                + ", base=" + baseFare + ", discount=" + discount
                + ", final=" + finalFare
                + (cancellationFee > 0 ? ", cancelFee=" + cancellationFee : "")
                + "}";
    }
}
