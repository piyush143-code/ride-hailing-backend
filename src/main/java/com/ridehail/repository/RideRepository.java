package com.ridehail.repository;

import com.ridehail.domain.Ride;
import com.ridehail.domain.enums.RideStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RideRepository {

    private final ConcurrentHashMap<String, Ride> rides = new ConcurrentHashMap<>();

    public Ride save(Ride ride) {
        rides.put(ride.getId(), ride);
        return ride;
    }

    public Optional<Ride> findById(String id) {
        return Optional.ofNullable(rides.get(id));
    }

    /** @param status  null returns rides in any status. */
    public List<Ride> findByUser(String userId, RideStatus status) {
        List<Ride> out = new ArrayList<>();
        for (Ride r : rides.values()) {
            if (!r.getUserId().equals(userId)) continue;
            if (status != null && r.getStatus() != status) continue;
            out.add(r);
        }
        return out;
    }

    public List<Ride> findByDriver(String driverId, RideStatus status) {
        List<Ride> out = new ArrayList<>();
        for (Ride r : rides.values()) {
            if (!r.getDriverId().equals(driverId)) continue;
            if (status != null && r.getStatus() != status) continue;
            out.add(r);
        }
        return out;
    }
}
