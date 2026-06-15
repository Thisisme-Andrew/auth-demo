package com.andys_portfolio.auth_demo.database.user.service;

import com.andys_portfolio.auth_demo.auth.exception.AuthenticationRequiredException;
import com.andys_portfolio.auth_demo.database.user.entity.User;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

  private final UserRepository userRepository;

  public User getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
      return null;
    }
    String email = auth.getName();
    return userRepository.findByEmail(email).orElse(null);
  }

  public User getCurrentUserOrThrow() {
    User user = getCurrentUser();
    if (user == null) {
      throw new AuthenticationRequiredException();
    }
    return user;
  }
}
