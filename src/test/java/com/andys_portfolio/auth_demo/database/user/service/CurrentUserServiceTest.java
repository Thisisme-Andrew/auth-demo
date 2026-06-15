package com.andys_portfolio.auth_demo.database.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andys_portfolio.auth_demo.auth.exception.AuthenticationRequiredException;
import com.andys_portfolio.auth_demo.database.user.entity.Role;
import com.andys_portfolio.auth_demo.database.user.entity.User;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.jpa.hibernate.ddl-auto=update"
})
class CurrentUserServiceTest {

  private static final String EMAIL = "current-user@example.com";

  @Autowired
  private CurrentUserService currentUserService;

  @Autowired
  private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    userRepository.deleteAll();
  }

  @Test
  void getCurrentUserReturnsNullWhenNotAuthenticated() {
    assertThat(currentUserService.getCurrentUser()).isNull();
  }

  @Test
  void getCurrentUserOrThrowThrowsWhenNotAuthenticated() {
    assertThatThrownBy(() -> currentUserService.getCurrentUserOrThrow())
        .isInstanceOf(AuthenticationRequiredException.class);
  }

  @Test
  void getCurrentUserReturnsUserWhenAuthenticated() {
    User saved = saveUser();

    setAuthentication(saved.getEmail());

    User current = currentUserService.getCurrentUser();

    assertThat(current).isNotNull();
    assertThat(current.getId()).isEqualTo(saved.getId());
    assertThat(current.getEmail()).isEqualTo(EMAIL);
  }

  @Test
  void getCurrentUserOrThrowReturnsUserWhenAuthenticated() {
    User saved = saveUser();

    setAuthentication(saved.getEmail());

    User current = currentUserService.getCurrentUserOrThrow();

    assertThat(current.getId()).isEqualTo(saved.getId());
    assertThat(current.getEmail()).isEqualTo(EMAIL);
  }

  @Test
  void getCurrentUserReturnsNullWhenUserNotInDatabase() {
    setAuthentication("missing@example.com");

    assertThat(currentUserService.getCurrentUser()).isNull();
  }

  @Test
  void getCurrentUserOrThrowThrowsWhenUserNotInDatabase() {
    setAuthentication("missing@example.com");

    assertThatThrownBy(() -> currentUserService.getCurrentUserOrThrow())
        .isInstanceOf(AuthenticationRequiredException.class);
  }

  private User saveUser() {
    return userRepository.save(User.builder()
        .email(EMAIL)
        .password("encoded-password")
        .role(Role.USER)
        .build());
  }

  private static void setAuthentication(String email) {
    var authentication = new UsernamePasswordAuthenticationToken(email, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
