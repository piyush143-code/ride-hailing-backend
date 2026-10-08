package com.ridehail.service;

import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.domain.enums.CarType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that even with many threads racing to book the same single driver,
 * exactly one booking wins. This is the race the PDF calls out as a bonus.
 */
class ConcurrencyTest {

    @Test
    void onlyOneBookerWinsTheSameDriver() throws Exception {
        TestFactory.Rig rig = new TestFactory.Rig();
        Location pickup = new Location(12.9716, 77.5946);
        Driver only = rig.driverService.register("D1", "1", CarType.SEDAN,
                new Location(12.9720, 77.5950));

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger losses = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final String user = "U" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    rig.rideService.bookRide(user, pickup, CarType.SEDAN, null);
                    wins.incrementAndGet();
                } catch (RideService.NoDriverAvailableException e) {
                    losses.incrementAndGet();
                } catch (Exception e) {
                    // treat other failures as losses
                    losses.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "threads did not finish in time");
        pool.shutdownNow();

        assertEquals(1, wins.get(), "exactly one booking should succeed");
        assertEquals(threads - 1, losses.get(), "others must receive NoDriverAvailable");
        assertEquals(com.ridehail.domain.enums.DriverStatus.ON_TRIP,
                rig.driverRepo.findById(only.getId()).orElseThrow().getStatus(),
                "the winning booker should have moved driver to ON_TRIP");
    }
}
