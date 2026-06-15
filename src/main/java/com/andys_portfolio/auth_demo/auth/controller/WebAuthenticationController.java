package com.andys_portfolio.auth_demo.auth.controller;

import com.andys_portfolio.auth_demo.auth.ClientType;
import com.andys_portfolio.auth_demo.auth.dto.request.AuthenticationRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.RegisterRequest;
import com.andys_portfolio.auth_demo.auth.dto.response.AuthenticationResponse;
import com.andys_portfolio.auth_demo.auth.dto.response.WebAuthenticationResponse;
import com.andys_portfolio.auth_demo.auth.service.AuthenticationService;
import com.andys_portfolio.auth_demo.auth.service.RefreshTokenCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/web")
@RequiredArgsConstructor
public class WebAuthenticationController {

  private static final ClientType CLIENT_TYPE = ClientType.WEB;
  private static final String X_REQUESTED_WITH = "X-Requested-With";
  private static final String XMLHttpRequest = "XMLHttpRequest";

  private final AuthenticationService authenticationService;
  private final RefreshTokenCookieService refreshTokenCookieService;

  @PostMapping("/register")
  public ResponseEntity<WebAuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Web registration request for email: {}", request.getEmail());
    AuthenticationResponse tokens = authenticationService.register(request, CLIENT_TYPE);
    return webResponseWithRefreshCookie(tokens);
  }

  @PostMapping("/login")
  public ResponseEntity<WebAuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
    log.info("Web login attempt for email: {}", request.getEmail());
    AuthenticationResponse tokens = authenticationService.authenticate(request, CLIENT_TYPE);
    return webResponseWithRefreshCookie(tokens);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<WebAuthenticationResponse> refresh(
      HttpServletRequest request,
      @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
    requireAjaxHeader(request);
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found");
    }

    log.debug("Web token refresh requested");
    AuthenticationResponse tokens = authenticationService.refreshTokens(refreshToken, CLIENT_TYPE);
    return webResponseWithRefreshCookie(tokens);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      HttpServletRequest request,
      @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
    requireAjaxHeader(request);
    log.info("Web logout requested");
    authenticationService.logout(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearCookie().toString())
        .build();
  }

  private ResponseEntity<WebAuthenticationResponse> webResponseWithRefreshCookie(AuthenticationResponse tokens) {
    ResponseCookie cookie = refreshTokenCookieService.createCookie(tokens.getRefreshToken());
    WebAuthenticationResponse body = WebAuthenticationResponse.builder()
        .accessToken(tokens.getAccessToken())
        .userId(tokens.getUserId())
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(body);
  }

  private static void requireAjaxHeader(HttpServletRequest request) {
    if (!XMLHttpRequest.equals(request.getHeader(X_REQUESTED_WITH))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid request");
    }
  }
}
