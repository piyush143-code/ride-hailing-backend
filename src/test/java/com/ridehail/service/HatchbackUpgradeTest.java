package com.ridehail.service;

import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.domain.Ride;
import com.ridehail.domain.enums.CarType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HatchbackUpgradeTest {

    private final TestFactory.Rig rig = new TestFactory.Rig();

    @Test
    void upgradesToSedanWhenNoHatchbackAvailableAndChargesHatchbackRate() {
        Location pickup = new Location(12.9716, 77.5946);
        // Only sedan drivers nearby.
        Driver sedan = rig.driverService.register("S1", "999", CarType.SEDAN,
                new Location(12.9720, 77.5950));

        Ride ride = rig.rideService.bookRide("U1", pickup, CarType.HATCHBACK, null);

        assertTrue(ride.isUpgraded(), "ride should be marked upgraded");
        assertEquals(CarType.HATCHBACK, ride.getRequestedCarType());
        assertEquals(CarType.SEDAN, ride.getAssignedCarType());
        assertEquals(sedan.getId(), ride.getDriverId());

        // Dropoff ~10 km away → hatchback rate: 2*8 + 3*6 + 5*4 = 54 (sedan would be 69)
        Location dropoff = new Location(12.9716 + 10.0 / 111.0, 77.5946);
        Ride ended = rig.rideService.endRide(ride.getId(), dropoff);
        assertEquals(54.0, ended.getBaseFare(), 0.5);
    }

    @Test
    void noUpgradeWhenHatchbackAvailable() {
        Location pickup = new Location(12.9716, 77.5946);
        rig.driverService.register("H1", "888", CarType.HATCHBACK,
                new Location(12.9720, 77.5950));
        rig.driverService.register("S1", "999", CarType.SEDAN,
                new Location(12.9720, 77.5950));

        Ride ride = rig.rideService.bookRide("U1", pickup, CarType.HATCHBACK, null);

        assertFalse(ride.isUpgraded(), "no upgrade when hatchback is available");
        assertEquals(CarType.HATCHBACK, ride.getAssignedCarType());
    }

    @Test
    void sedanRequestWithNoDriversThrows() {
        Location pickup = new Location(12.9716, 77.5946);
        assertThrows(RideService.NoDriverAvailableException.class,
                () -> rig.rideService.bookRide("U1", pickup, CarType.SEDAN, null));
    }

    @Test
    void hatchbackRequestWithNoDriversAtAllThrows() {
        Location pickup = new Location(12.9716, 77.5946);
        assertThrows(RideService.NoDriverAvailableException.class,
                () -> rig.rideService.bookRide("U1", pickup, CarType.HATCHBACK, null));
    }
}
