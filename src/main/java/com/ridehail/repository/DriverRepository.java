package com.ridehail.repository;

import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.domain.enums.CarType;
import com.ridehail.domain.enums.DriverStatus;
import com.ridehail.util.GeoUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DriverRepository {

    private final ConcurrentHashMap<String, Driver> drivers = new ConcurrentHashMap<>();

    public Driver save(Driver driver) {
        drivers.put(driver.getId(), driver);
        return driver;
    }

    public Optional<Driver> findById(String id) {
        return Optional.ofNullable(drivers.get(id));
    }

    /**
     * Snapshot of available drivers of the requested type within {@code radiusKm}
     * of {@code pickup}. Returned list is a snapshot — not a live view.
     */
    public List<Driver> findAvailableNear(Location pickup, CarType carType, double radiusKm) {
        List<Driver> result = new ArrayList<>();
        for (Driver d : drivers.values()) {
            if (d.getStatus() != DriverStatus.AVAILABLE) continue;
            if (d.getCarType() != carType) continue;
            if (GeoUtil.distanceKm(pickup, d.getLocation()) > radiusKm) continue;
            result.add(d);
        }
        return result;
    }

    /**
     * Atomically moves driver AVAILABLE → RESERVED. Returns true iff this call
     * won the reservation. All concurrent callers see at most one true.
     */
    public boolean tryReserve(String driverId) {
        Driver driver = drivers.get(driverId);
        if (driver == null) return false;
        driver.getLock().lock();
        try {
            if (driver.getStatus() != DriverStatus.AVAILABLE) return false;
            driver.setStatus(DriverStatus.RESERVED);
            return true;
        } finally {
            driver.getLock().unlock();
        }
    }

    /** Release a reservation back to AVAILABLE (used e.g. when booking aborts). */
    public void release(String driverId) {
        Driver driver = drivers.get(driverId);
        if (driver == null) return;
        driver.getLock().lock();
        try {
            driver.setStatus(DriverStatus.AVAILABLE);
        } finally {
            driver.getLock().unlock();
        }
    }

    public void setStatus(String driverId, DriverStatus status) {
        Driver driver = drivers.get(driverId);
        if (driver == null) return;
        driver.getLock().lock();
        try {
            driver.setStatus(status);
        } finally {
            driver.getLock().unlock();
        }
    }
}
