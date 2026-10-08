package com.ridehail.domain;

import com.ridehail.domain.enums.CarType;
import com.ridehail.domain.enums.DriverStatus;

import java.util.concurrent.locks.ReentrantLock;

public class Driver {
    private final String id;
    private final String name;
    private final String phone;
    private final CarType carType;

    private volatile Location location;
    private volatile DriverStatus status;
    private volatile double rating;

    /**
     * Guards all transitions of {@link #status}. See
     * {@code DriverRepository#tryReserve} for the atomic reserve path.
     */
    private final ReentrantLock lock = new ReentrantLock();

    public Driver(String id, String name, String phone, CarType carType, Location location) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.carType = carType;
        this.location = location;
        this.status = DriverStatus.AVAILABLE;
        this.rating = 5.0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public CarType getCarType() { return carType; }
    public Location getLocation() { return location; }
    public DriverStatus getStatus() { return status; }
    public double getRating() { return rating; }
    public ReentrantLock getLock() { return lock; }

    public void setLocation(Location location) { this.location = location; }
    public void setStatus(DriverStatus status) { this.status = status; }
    public void setRating(double rating) { this.rating = rating; }

    @Override
    public String toString() {
        return "Driver{" + id + ", " + name + ", " + carType + ", " + status + ", rating=" + rating + "}";
    }
}
