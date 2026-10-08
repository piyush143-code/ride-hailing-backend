package com.ridehail.service;

import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.domain.enums.CarType;
import com.ridehail.repository.DriverRepository;

import java.util.NoSuchElementException;
import java.util.UUID;

public class DriverService {

    private final DriverRepository repo;

    public DriverService(DriverRepository repo) {
        this.repo = repo;
    }

    public Driver register(String name, String phone, CarType carType, Location location) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("phone required");
        if (carType == null) throw new IllegalArgumentException("carType required");
        if (location == null) throw new IllegalArgumentException("location required");
        return repo.save(new Driver("D-" + UUID.randomUUID().toString().substring(0, 8),
                name, phone, carType, location));
    }

    public void updateLocation(String driverId, Location location) {
        Driver d = repo.findById(driverId)
                .orElseThrow(() -> new NoSuchElementException("driver not found: " + driverId));
        d.setLocation(location);
    }
}
