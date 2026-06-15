package com.andys_portfolio.auth_demo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andys_portfolio.auth_demo.auth.ClientType;
import com.andys_portfolio.auth_demo.auth.dto.request.AuthenticationRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.RegisterRequest;
import com.andys_portfolio.auth_demo.auth.exception.DuplicateEmailException;
import com.andys_portfolio.auth_demo.auth.exception.InvalidRefreshTokenException;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.jpa.hibernate.ddl-auto=update"
})
class AuthenticationServiceTest {

  private static final String EMAIL = "auth-service@example.com";
  private static final String PASSWORD = "password123";

  @Autowired
  private AuthenticationService authenticationService;

  @Autowired
  private RefreshTokenService refreshTokenService;

  @Autowired
  private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    refreshTokenService.invalidateAllForEmail(EMAIL);
    userRepository.deleteAll();
  }

  @Test
  void registerIssuesTokensAndPersistsUser() {
    var response = authenticationService.register(registerRequest(), ClientType.MOBILE);

    assertThat(response.getAccessToken()).isNotBlank();
    assertThat(response.getRefreshToken()).isNotBlank();
    assertThat(response.getUserId()).isNotNull();
    assertThat(userRepository.existsByEmail(EMAIL)).isTrue();
    assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getPassword()).startsWith("$2");
  }

  @Test
  void registerRejectsDuplicateEmail() {
    authenticationService.register(registerRequest(), ClientType.MOBILE);

    assertThatThrownBy(() -> authenticationService.register(registerRequest(), ClientType.MOBILE))
        .isInstanceOf(DuplicateEmailException.class);
  }

  @Test
  void authenticateIssuesTokensForExistingUser() {
    authenticationService.register(registerRequest(), ClientType.MOBILE);

    var loginRequest = new AuthenticationRequest();
    loginRequest.setEmail(EMAIL);
    loginRequest.setPassword(PASSWORD);

    var response = authenticationService.authenticate(loginRequest, ClientType.MOBILE);

    assertThat(response.getAccessToken()).isNotBlank();
    assertThat(response.getRefreshToken()).isNotBlank();
    assertThat(response.getUserId()).isNotNull();
  }

  @Test
  void refreshRotatesToken() {
    var registered = authenticationService.register(registerRequest(), ClientType.MOBILE);
    String oldRefreshToken = registered.getRefreshToken();

    var refreshed = authenticationService.refreshTokens(oldRefreshToken, ClientType.MOBILE);

    assertThat(refreshed.getAccessToken()).isNotBlank();
    assertThat(refreshed.getRefreshToken()).isNotBlank();
    assertThat(refreshed.getRefreshToken()).isNotEqualTo(oldRefreshToken);
    assertThat(refreshTokenService.validateAndGetEmail(oldRefreshToken, ClientType.MOBILE)).isNull();
  }

  @Test
  void refreshRejectsInvalidToken() {
    assertThatThrownBy(() -> authenticationService.refreshTokens("invalid-token", ClientType.WEB))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void logoutInvalidatesRefreshToken() {
    var registered = authenticationService.register(registerRequest(), ClientType.WEB);
    String refreshToken = registered.getRefreshToken();

    authenticationService.logout(refreshToken);

    assertThat(refreshTokenService.validateAndGetEmail(refreshToken, ClientType.WEB)).isNull();
  }

  private static RegisterRequest registerRequest() {
    var request = new RegisterRequest();
    request.setFirstName("Test");
    request.setLastName("User");
    request.setEmail(EMAIL);
    request.setPassword(PASSWORD);
    return request;
  }
}
