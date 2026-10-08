package com.ridehail.matching;

import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.util.GeoUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Highest rating first; break ties with nearer driver. */
public class HighestRatedDriverStrategy implements DriverMatchingStrategy {

    @Override
    public Optional<Driver> pick(Location pickup, List<Driver> candidates) {
        return candidates.stream()
                .max(Comparator
                        .comparingDouble(Driver::getRating)
                        .thenComparing(Comparator.comparingDouble(
                                (Driver d) -> GeoUtil.distanceKm(pickup, d.getLocation())).reversed()));
    }
}
