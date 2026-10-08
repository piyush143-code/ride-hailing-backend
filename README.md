# Ride Hailing Backend

An in-memory ride-hailing backend built as a Java 21 CLI for the AI-assisted machine
coding round. Pure domain code, no framework — strategies for pricing, driver matching,
and cancellation are swappable without touching business logic.

## Quick start

```bash
# JDK 21+ required; this project was built on OpenJDK 27 (Homebrew).
mvn test                # 17 tests across pricing, coupons, upgrade, concurrency
mvn package
java -jar target/ride-hailing-backend.jar
```

## REPL smoke test

```
> register-user alice 9999999999
OK User{U-xxxxxxxx, alice, 9999999999}

> register-driver bob 1111111111 SEDAN 12.9720 77.5950
OK Driver{D-aaaaaaaa, bob, SEDAN, AVAILABLE, rating=5.0}

> add-coupon FLAT20 FLAT 20
OK Coupon{FLAT20, FLAT, value=20.0}

> book U-xxxxxxxx 12.9716 77.5946 SEDAN FLAT20
OK Ride{R-bbbbbbbb, user=U-..., driver=D-..., SEDAN, ONGOING, ...}

> end R-bbbbbbbb 13.0616 77.5946
OK Ride{R-bbbbbbbb, ... COMPLETED, distance=10.01km, base=69.07, discount=20.0, final=49.07}

> history-user U-xxxxxxxx
Ride{...}

> help
```

Edge cases to try:
- `book` with no drivers nearby → `ERR: No driver available...` (**no driver in radius**)
- Ride 0.5 km → base fare = ₹50 (**minimum fare**)
- `book U HATCHBACK` while only sedans are available → ride marked `upgraded` and billed at hatchback rate (**free upgrade**)
- `book ... INVALID_COUPON` → ride proceeds, no discount applied (**invalid coupon ignored**)

## Design

```
cli            — parses REPL commands → service calls
service        — orchestration (RideService, UserService, DriverService, CouponService)
pricing        — PricingStrategy + TieredPricingStrategy + PricingConfig
matching       — DriverMatchingStrategy + Nearest/HighestRated impls
cancellation   — CancellationPolicy + FlatFeeCancellationPolicy
repository     — in-memory, ConcurrentHashMap-backed
domain         — User, Driver, Ride, Coupon, Location, enums
util           — GeoUtil (haversine)
```

### Key design decisions

1. **Strategy pattern wherever a requirement might change.** Pricing, matching, and
   cancellation each live behind a single-method interface. `RideService` depends on
   the interface, not the concrete class, so adding a surge strategy or a highest-rated
   matching strategy is one new class and one line in `Main`.
2. **`PricingConfig` keyed by `CarType`.** Adding a new car type (e.g. SUV) is one new
   enum value and one new entry in `PricingConfig.defaults()`. The strategy code itself
   never changes.
3. **Hatchback → sedan free upgrade** happens in `RideService.reserveDriver` after the
   first pass finds nothing. The ride is marked `upgraded=true` and billed at the
   requested (hatchback) car type's rate, not the assigned type's — "no extra cost".
4. **Concurrency safety.** `Driver.status` transitions are gated by a per-driver
   `ReentrantLock` inside `DriverRepository.tryReserve`, which atomically moves a
   driver `AVAILABLE → RESERVED`. Only the thread that wins the lock-and-check gets
   the ride; losers get a clean `NoDriverAvailableException` and the booking loop
   moves to the next candidate. Verified by `ConcurrencyTest` racing 50 threads on
   one driver.
5. **Cancellation.** `CancellationPolicy` is a one-method interface; the default
   `FlatFeeCancellationPolicy` charges ₹30 whenever a driver was assigned, ₹0
   otherwise. A time-based policy drops in without touching `RideService`.
6. **Coupons.** `Coupon.DiscountType` is `FLAT` or `PERCENT`, with optional expiry.
   The discount is applied inside `endRide` *after* base pricing. Unknown or expired
   codes are silently ignored (the ride still succeeds at full fare) — a deliberate
   UX choice; `CouponService.findApplicable` returns an `Optional`.
7. **One repository per aggregate**, each a thin wrapper over a `ConcurrentHashMap`.
   The repository is the only thing that mutates `Driver.status`, so the concurrency
   invariant has a single enforcement point.
8. **No framework.** Spring would have eaten most of the two hours on scaffolding that
   isn't graded. All wiring lives in `Main` and is trivially swappable to a Javalin/
   REST layer later.
9. **Clock injection.** `RideService` takes a `java.time.Clock`, so coupon expiry and
   ride timestamps are deterministic in tests.

### Pricing engine (the tested target)

`TieredPricingStrategy` walks the car-specific tiers from 0 km and applies the car's
minimum fare as a floor. Default Sedan matches the PDF: ₹50 min; 0–2 km @ ₹10, 2–5 km
@ ₹8, 5+ km @ ₹5. Hatchback is strictly cheaper (₹40 min; ₹8 / ₹6 / ₹4) so the
free-upgrade invariant (`price(d, HATCHBACK) ≤ price(d, SEDAN)`) is a meaningful
assertion.

Tests: tier boundaries (2 km and 5 km), under-minimum (0.5 km → ₹50), long ride
spanning all tiers, hatchback cheaper-than-sedan invariant, flat and percent coupons,
unknown/expired coupon ignored, discount capped at fare, free hatchback→sedan upgrade
billed at hatchback rate, concurrency race on single driver.

## Assumptions

- In-memory storage; everything is lost on exit. The repo interfaces would survive a
  swap to a database.
- Default search radius 5 km. Linear scan over all drivers for radius search — fine for
  small `N`, would need a geo-index at scale.
- Hatchback rates are cheaper than sedan rates — invariant guaranteed by
  `PricingConfig.defaults()`; a hatchback→sedan upgrade is billed at hatchback rate
  (so "no extra cost" holds).
- Default cancellation fee ₹30 whenever a driver was assigned. In this flow every
  ongoing ride has one, so the predicate is effectively "ride is ongoing".
- Coupons are not per-user and not single-use. Multi-use is acceptable for this scope;
  per-user redemption tracking would belong in `CouponService` with a `Set<userId>` per
  coupon.
- Driver ratings start at 5.0 and don't change — matching the PDF scope. Rating updates
  would live in `DriverService`.
- IDs are `U-xxxxxxxx` / `D-xxxxxxxx` / `R-xxxxxxxx` (first 8 chars of a UUID) —
  readable for the REPL demo, globally unique in practice.

## What I would do with more time

- **Surge pricing.** Add `SurgeStrategy` interface and wire it into
  `TieredPricingStrategy` or wrap it as a `SurgePricingDecorator` — the pricing
  interface already takes everything needed except the area/time, which would be
  derived in `RideService`.
- **Geo index** (grid or quadtree) in `DriverRepository.findAvailableNear` to replace
  the O(N) scan.
- **Persistence** behind the existing repository interfaces.
- **REST layer** in Javalin — services are already framework-agnostic, so this is a
  thin adapter.
- **Per-user coupon redemption** tracking.
- **Driver ratings** that actually update on ride completion.
- **Idempotency keys** on `book` to avoid duplicate rides on client retry.

## How I used AI

I treated the AI (Claude Code) as a fast scaffolder and reviewer, not a decider.

- **Prompted for:** the Maven `pom.xml`, boilerplate getters/setters for the mutable
  entities (`Driver`, `Ride`), the haversine formula, and the REPL command-dispatch
  skeleton. These are correct-by-shape, not design decisions, so delegating is fine.
- **Designed by hand (not AI):** the strategy boundaries (pricing / matching /
  cancellation each as its own interface), the per-driver `ReentrantLock` approach for
  concurrency safety, the choice to bill upgraded rides at the *requested* car type's
  rate, and the single-point-of-mutation rule for `Driver.status` living inside the
  repository.
- **Rewrote or rejected:** an initial AI suggestion to synchronize at the service
  layer — moved it down into the repository so the invariant has one enforcement site.
  I also tightened the `PricingConfig` to validate strictly-increasing tier boundaries
  so a misconfigured config fails fast instead of silently charging ₹0 for the middle
  tier.
- **Verified by tests, not vibes.** The concurrency race is tested with 50 threads;
  pricing tiers are tested at exact boundaries; coupon edge cases (expired, unknown,
  over-cap) each have their own test. Every passing claim in this README is backed by
  a green test or a REPL command you can run yourself.
