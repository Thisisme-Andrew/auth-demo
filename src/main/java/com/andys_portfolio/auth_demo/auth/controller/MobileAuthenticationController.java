package com.andys_portfolio.auth_demo.auth.controller;

import com.andys_portfolio.auth_demo.auth.ClientType;
import com.andys_portfolio.auth_demo.auth.dto.request.AccessTokenRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.AuthenticationRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.LogoutRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.RegisterRequest;
import com.andys_portfolio.auth_demo.auth.dto.response.AuthenticationResponse;
import com.andys_portfolio.auth_demo.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/mobile")
@RequiredArgsConstructor
public class MobileAuthenticationController {

  private static final ClientType CLIENT_TYPE = ClientType.MOBILE;

  private final AuthenticationService authenticationService;

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Mobile registration request for email: {}", request.getEmail());
    return ResponseEntity.ok(authenticationService.register(request, CLIENT_TYPE));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
    log.info("Mobile login attempt for email: {}", request.getEmail());
    return ResponseEntity.ok(authenticationService.authenticate(request, CLIENT_TYPE));
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<AuthenticationResponse> refresh(@Valid @RequestBody AccessTokenRequest request) {
    log.debug("Mobile token refresh requested");
    return ResponseEntity.ok(authenticationService.refreshTokens(request, CLIENT_TYPE));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    log.info("Mobile logout requested");
    authenticationService.logout(request);
    return ResponseEntity.ok().build();
  }
}
