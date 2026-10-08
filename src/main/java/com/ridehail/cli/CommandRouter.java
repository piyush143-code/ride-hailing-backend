package com.ridehail.cli;

import com.ridehail.domain.Coupon;
import com.ridehail.domain.Driver;
import com.ridehail.domain.Location;
import com.ridehail.domain.Ride;
import com.ridehail.domain.User;
import com.ridehail.domain.enums.CarType;
import com.ridehail.domain.enums.RideStatus;
import com.ridehail.service.CouponService;
import com.ridehail.service.DriverService;
import com.ridehail.service.RideService;
import com.ridehail.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Dispatches whitespace-separated input lines to the right service call.
 * Returns a human-readable reply; errors are rendered as "ERR: ...".
 */
public class CommandRouter {

    private final UserService users;
    private final DriverService drivers;
    private final RideService rides;
    private final CouponService coupons;

    public CommandRouter(UserService users, DriverService drivers, RideService rides, CouponService coupons) {
        this.users = users;
        this.drivers = drivers;
        this.rides = rides;
        this.coupons = coupons;
    }

    public String dispatch(String line) {
        if (line == null) return "";
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return "";
        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        try {
            return switch (cmd) {
                case "register-user" -> registerUser(parts);
                case "register-driver" -> registerDriver(parts);
                case "update-location" -> updateLocation(parts);
                case "book" -> book(parts);
                case "end" -> end(parts);
                case "cancel" -> cancel(parts);
                case "history-user" -> historyUser(parts);
                case "history-driver" -> historyDriver(parts);
                case "add-coupon" -> addCoupon(parts);
                case "delete-coupon" -> deleteCoupon(parts);
                case "help" -> help();
                default -> "ERR: unknown command '" + cmd + "'. Try 'help'.";
            };
        } catch (RuntimeException e) {
            return "ERR: " + e.getMessage();
        }
    }

    private String registerUser(String[] p) {
        require(p, 3, "register-user <name> <phone>");
        User u = users.register(p[1], p[2]);
        return "OK " + u;
    }

    private String registerDriver(String[] p) {
        require(p, 6, "register-driver <name> <phone> <SEDAN|HATCHBACK> <lat> <lng>");
        Driver d = drivers.register(p[1], p[2], carType(p[3]),
                new Location(parseD(p[4], "lat"), parseD(p[5], "lng")));
        return "OK " + d;
    }

    private String updateLocation(String[] p) {
        require(p, 4, "update-location <driverId> <lat> <lng>");
        drivers.updateLocation(p[1], new Location(parseD(p[2], "lat"), parseD(p[3], "lng")));
        return "OK location updated";
    }

    private String book(String[] p) {
        if (p.length != 5 && p.length != 6) {
            return "ERR: usage: book <userId> <lat> <lng> <SEDAN|HATCHBACK> [couponCode]";
        }
        String coupon = p.length == 6 ? p[5] : null;
        Ride r = rides.bookRide(p[1],
                new Location(parseD(p[2], "lat"), parseD(p[3], "lng")),
                carType(p[4]), coupon);
        return "OK " + r;
    }

    private String end(String[] p) {
        require(p, 4, "end <rideId> <destLat> <destLng>");
        Ride r = rides.endRide(p[1], new Location(parseD(p[2], "destLat"), parseD(p[3], "destLng")));
        return "OK " + r;
    }

    private String cancel(String[] p) {
        require(p, 2, "cancel <rideId>");
        Ride r = rides.cancelRide(p[1]);
        return "OK " + r;
    }

    private String historyUser(String[] p) {
        if (p.length != 2 && p.length != 3) return "ERR: usage: history-user <userId> [ONGOING|COMPLETED|CANCELLED]";
        RideStatus status = p.length == 3 ? RideStatus.valueOf(p[2].toUpperCase(Locale.ROOT)) : null;
        return formatRides(rides.userHistory(p[1], status));
    }

    private String historyDriver(String[] p) {
        if (p.length != 2 && p.length != 3) return "ERR: usage: history-driver <driverId> [ONGOING|COMPLETED|CANCELLED]";
        RideStatus status = p.length == 3 ? RideStatus.valueOf(p[2].toUpperCase(Locale.ROOT)) : null;
        return formatRides(rides.driverHistory(p[1], status));
    }

    private String addCoupon(String[] p) {
        if (p.length != 4 && p.length != 5) return "ERR: usage: add-coupon <code> <FLAT|PERCENT> <value> [YYYY-MM-DD]";
        Coupon.DiscountType type = Coupon.DiscountType.valueOf(p[2].toUpperCase(Locale.ROOT));
        LocalDate expiry = p.length == 5 ? LocalDate.parse(p[4]) : null;
        Coupon c = coupons.add(p[1], type, parseD(p[3], "value"), expiry);
        return "OK " + c;
    }

    private String deleteCoupon(String[] p) {
        require(p, 2, "delete-coupon <code>");
        coupons.delete(p[1]);
        return "OK deleted";
    }

    private String formatRides(List<Ride> rs) {
        if (rs.isEmpty()) return "(no rides)";
        StringBuilder sb = new StringBuilder();
        for (Ride r : rs) sb.append(r).append('\n');
        return sb.toString().stripTrailing();
    }

    private static void require(String[] p, int n, String usage) {
        if (p.length != n) throw new IllegalArgumentException("usage: " + usage);
    }

    private static CarType carType(String s) {
        try { return CarType.valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("unknown car type: " + s); }
    }

    private static double parseD(String s, String field) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(field + " must be a number, got '" + s + "'"); }
    }

    private String help() {
        return """
                Commands:
                  register-user <name> <phone>
                  register-driver <name> <phone> <SEDAN|HATCHBACK> <lat> <lng>
                  update-location <driverId> <lat> <lng>
                  book <userId> <lat> <lng> <SEDAN|HATCHBACK> [couponCode]
                  end <rideId> <destLat> <destLng>
                  cancel <rideId>
                  history-user <userId> [ONGOING|COMPLETED|CANCELLED]
                  history-driver <driverId> [ONGOING|COMPLETED|CANCELLED]
                  add-coupon <code> <FLAT|PERCENT> <value> [YYYY-MM-DD]
                  delete-coupon <code>
                  help | exit
                """.stripTrailing();
    }
}
