package com.andys_portfolio.auth_demo.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenCookieService {

  public static final String COOKIE_NAME = "refresh_token";
  public static final String COOKIE_PATH = "/api/v1/auth/web";
  private static final long REFRESH_TOKEN_MAX_AGE_SECONDS = 7L * 24 * 60 * 60;

  @Value("${app.auth.cookie.secure:false}")
  private boolean secureCookie;

  public ResponseCookie createCookie(String refreshToken) {
    return ResponseCookie.from(COOKIE_NAME, refreshToken)
        .httpOnly(true)
        .secure(secureCookie)
        .path(COOKIE_PATH)
        .sameSite("Strict")
        .maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS)
        .build();
  }

  public ResponseCookie clearCookie() {
    return ResponseCookie.from(COOKIE_NAME, "")
        .httpOnly(true)
        .secure(secureCookie)
        .path(COOKIE_PATH)
        .sameSite("Strict")
        .maxAge(0)
        .build();
  }
}

