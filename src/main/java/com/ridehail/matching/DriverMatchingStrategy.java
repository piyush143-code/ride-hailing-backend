package com.ridehail.matching;

import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;

import java.util.List;
import java.util.Optional;

public interface DriverMatchingStrategy {
    /**
     * Pick a driver from the candidate list given the rider's pickup location.
     * Caller is responsible for the candidate pool (status, car-type, radius
     * filtering); this method only ranks.
     */
    Optional<Driver> pick(Location pickup, List<Driver> candidates);
}
