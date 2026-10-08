package com.ridehail;

import com.ridehail.cancellation.CancellationPolicy;
import com.ridehail.cancellation.FlatFeeCancellationPolicy;
import com.ridehail.cli.CommandRouter;
import com.ridehail.matching.DriverMatchingStrategy;
import com.ridehail.matching.NearestDriverStrategy;
import com.ridehail.pricing.PricingConfig;
import com.ridehail.pricing.PricingStrategy;
import com.ridehail.pricing.TieredPricingStrategy;
import com.ridehail.repository.CouponRepository;
import com.ridehail.repository.DriverRepository;
import com.ridehail.repository.RideRepository;
import com.ridehail.repository.UserRepository;
import com.ridehail.service.CouponService;
import com.ridehail.service.DriverService;
import com.ridehail.service.RideService;
import com.ridehail.service.UserService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Composition root + REPL. All wiring is explicit — no DI framework. Change the
 * strategy class names below to swap behaviour without touching services.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // --- repositories (in-memory) ---
        UserRepository userRepo = new UserRepository();
        DriverRepository driverRepo = new DriverRepository();
        RideRepository rideRepo = new RideRepository();
        CouponRepository couponRepo = new CouponRepository();

        // --- strategies (swap here) ---
        PricingStrategy pricing = new TieredPricingStrategy(PricingConfig.defaults());
        DriverMatchingStrategy matching = new NearestDriverStrategy();
        CancellationPolicy cancellation = new FlatFeeCancellationPolicy(30.0);
        double searchRadiusKm = 5.0;

        // --- services ---
        UserService userService = new UserService(userRepo);
        DriverService driverService = new DriverService(driverRepo);
        CouponService couponService = new CouponService(couponRepo);
        RideService rideService = new RideService(
                driverRepo, rideRepo, pricing, matching, couponService,
                cancellation, searchRadiusKm, Clock.systemDefaultZone());

        CommandRouter router = new CommandRouter(userService, driverService, rideService, couponService);

        System.out.println("ride-hailing REPL — type 'help' or 'exit'");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = readLine(in)) != null) {
                if ("exit".equalsIgnoreCase(line.trim())) break;
                String out = router.dispatch(line);
                if (!out.isEmpty()) System.out.println(out);
            }
        }
    }

    private static String readLine(BufferedReader in) throws Exception {
        System.out.print("> ");
        return in.readLine();
    }
}
