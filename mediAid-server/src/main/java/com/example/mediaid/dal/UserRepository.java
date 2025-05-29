package com.example.mediaid.dal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    public User findByEmail(String email);

}
