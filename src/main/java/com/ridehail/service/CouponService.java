package com.ridehail.service;

import com.ridehail.domain.Coupon;
import com.ridehail.repository.CouponRepository;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CouponService {

    private final CouponRepository repo;

    public CouponService(CouponRepository repo) {
        this.repo = repo;
    }

    public Coupon add(String code, Coupon.DiscountType type, double value, LocalDate expiry) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code required");
        return repo.save(new Coupon(code, type, value, expiry));
    }

    public void delete(String code) {
        if (!repo.delete(code)) {
            throw new NoSuchElementException("coupon not found: " + code);
        }
    }

    /** Fetch a coupon only if it exists and is not expired as of {@code today}. */
    public Optional<Coupon> findApplicable(String code, LocalDate today) {
        if (code == null || code.isBlank()) return Optional.empty();
        return repo.findByCode(code).filter(c -> !c.isExpired(today));
    }
}
