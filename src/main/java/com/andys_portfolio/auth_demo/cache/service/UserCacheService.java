package com.andys_portfolio.auth_demo.cache.service;

import com.andys_portfolio.auth_demo.cache.dto.UserAuthCache;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCacheService {

  private final UserRepository repository;

  // This code is in case it does not find the user already in cache
  @Cacheable(value = "userAuth", key = "#email")
  public UserAuthCache getCachedUser(String email) {
    var user = repository
      .findByEmail(email)
      .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    return UserAuthCache.builder()
      .email(user.getEmail())
      .password(user.getPassword())
      .roles(user
        .getAuthorities()
        .stream()
        .map(auth -> auth.getAuthority())
        .toList())
      .build();
  }

  @CacheEvict(value = "userAuth", key = "#email")
  public void evictUser(String email) {
    // Evicts cached credentials after password change or reset.
  }
}