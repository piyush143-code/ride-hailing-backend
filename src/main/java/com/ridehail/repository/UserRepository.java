package com.ridehail.repository;

import com.ridehail.domain.User;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }
}
