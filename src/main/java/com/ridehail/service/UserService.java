package com.ridehail.service;

import com.ridehail.domain.User;
import com.ridehail.repository.UserRepository;

import java.util.UUID;

public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User register(String name, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("phone required");
        return repo.save(new User("U-" + UUID.randomUUID().toString().substring(0, 8), name, phone));
    }
}
