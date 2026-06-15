package com.andys_portfolio.auth_demo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.andys_portfolio.auth_demo.auth.ClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
class RefreshTokenServiceTest {

  private static final String EMAIL = "user@example.com";

  @Autowired
  private RefreshTokenService refreshTokenService;

  @AfterEach
  void tearDown() {
    refreshTokenService.invalidateAllForEmail(EMAIL);
  }

  @Test
  void createAndValidateReturnsEmail() {
    String token = refreshTokenService.createRefreshToken(EMAIL, ClientType.WEB);

    String email = refreshTokenService.validateAndGetEmail(token, ClientType.WEB);

    assertThat(email).isEqualTo(EMAIL);
  }

  @Test
  void wrongClientTypeReturnsNull() {
    String token = refreshTokenService.createRefreshToken(EMAIL, ClientType.WEB);

    assertThat(refreshTokenService.validateAndGetEmail(token, ClientType.MOBILE)).isNull();
  }

  @Test
  void deleteRefreshTokenPreventsReuse() {
    String token = refreshTokenService.createRefreshToken(EMAIL, ClientType.WEB);

    refreshTokenService.deleteRefreshToken(token);

    assertThat(refreshTokenService.validateAndGetEmail(token, ClientType.WEB)).isNull();
  }

  @Test
  void invalidateAllForEmailRemovesAllTokens() {
    String webToken = refreshTokenService.createRefreshToken(EMAIL, ClientType.WEB);
    String mobileToken = refreshTokenService.createRefreshToken(EMAIL, ClientType.MOBILE);

    refreshTokenService.invalidateAllForEmail(EMAIL);

    assertThat(refreshTokenService.validateAndGetEmail(webToken, ClientType.WEB)).isNull();
    assertThat(refreshTokenService.validateAndGetEmail(mobileToken, ClientType.MOBILE)).isNull();
  }
}
