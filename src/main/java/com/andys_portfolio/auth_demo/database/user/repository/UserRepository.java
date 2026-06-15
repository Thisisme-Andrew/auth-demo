package com.andys_portfolio.auth_demo.database.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.andys_portfolio.auth_demo.database.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
