package com.andys_portfolio.auth_demo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenCookieServiceTest {

  private static final String REFRESH_TOKEN = "test-refresh-token-uuid";
  private static final long SEVEN_DAYS_SECONDS = 7L * 24 * 60 * 60;

  private RefreshTokenCookieService cookieService;

  @BeforeEach
  void setUp() {
    cookieService = new RefreshTokenCookieService();
    ReflectionTestUtils.setField(cookieService, "secureCookie", false);
  }

  @Test
  void createCookieSetsExpectedAttributes() {
    ResponseCookie cookie = cookieService.createCookie(REFRESH_TOKEN);

    assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieService.COOKIE_NAME);
    assertThat(cookie.getValue()).isEqualTo(REFRESH_TOKEN);
    assertThat(cookie.getPath()).isEqualTo(RefreshTokenCookieService.COOKIE_PATH);
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isFalse();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(SEVEN_DAYS_SECONDS));
  }

  @Test
  void createCookieRespectsSecureFlagWhenEnabled() {
    ReflectionTestUtils.setField(cookieService, "secureCookie", true);

    ResponseCookie cookie = cookieService.createCookie(REFRESH_TOKEN);

    assertThat(cookie.isSecure()).isTrue();
  }

  @Test
  void clearCookieExpiresImmediately() {
    ResponseCookie cookie = cookieService.clearCookie();

    assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieService.COOKIE_NAME);
    assertThat(cookie.getValue()).isEmpty();
    assertThat(cookie.getPath()).isEqualTo(RefreshTokenCookieService.COOKIE_PATH);
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isFalse();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
  }

  @Test
  void createCookieProducesSetCookieHeader() {
    ResponseCookie cookie = cookieService.createCookie(REFRESH_TOKEN);

    String header = cookie.toString();

    assertThat(header).contains("refresh_token=" + REFRESH_TOKEN);
    assertThat(header).contains("Path=/api/v1/auth/web");
    assertThat(header).contains("HttpOnly");
    assertThat(header).contains("SameSite=Strict");
  }
}
