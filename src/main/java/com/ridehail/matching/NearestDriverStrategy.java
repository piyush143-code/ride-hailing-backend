package com.ridehail.matching;

import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.util.GeoUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NearestDriverStrategy implements DriverMatchingStrategy {

    @Override
    public Optional<Driver> pick(Location pickup, List<Driver> candidates) {
        return candidates.stream()
                .min(Comparator.comparingDouble(d -> GeoUtil.distanceKm(pickup, d.getLocation())));
    }
}
