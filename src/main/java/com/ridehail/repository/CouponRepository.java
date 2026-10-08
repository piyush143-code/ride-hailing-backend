package com.ridehail.repository;

import com.ridehail.domain.Coupon;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CouponRepository {

    private final ConcurrentHashMap<String, Coupon> coupons = new ConcurrentHashMap<>();

    public Coupon save(Coupon coupon) {
        coupons.put(coupon.getCode(), coupon);
        return coupon;
    }

    public Optional<Coupon> findByCode(String code) {
        return Optional.ofNullable(coupons.get(code));
    }

    public boolean delete(String code) {
        return coupons.remove(code) != null;
    }
}
