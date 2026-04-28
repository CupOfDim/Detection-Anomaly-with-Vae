package com.natyrov.spring.detection_with_vae.repository;

import com.natyrov.spring.detection_with_vae.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findAllByEmail(String email);
    boolean existsByEmail(String email);
}