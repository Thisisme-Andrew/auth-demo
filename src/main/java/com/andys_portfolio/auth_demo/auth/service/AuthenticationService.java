package com.andys_portfolio.auth_demo.auth.service;

import com.andys_portfolio.auth_demo.auth.ClientType;
import com.andys_portfolio.auth_demo.auth.dto.request.AccessTokenRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.AuthenticationRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.LogoutRequest;
import com.andys_portfolio.auth_demo.auth.dto.request.RegisterRequest;
import com.andys_portfolio.auth_demo.auth.dto.response.AuthenticationResponse;
import com.andys_portfolio.auth_demo.auth.exception.DuplicateEmailException;
import com.andys_portfolio.auth_demo.auth.exception.InvalidRefreshTokenException;
import com.andys_portfolio.auth_demo.database.user.entity.Role;
import com.andys_portfolio.auth_demo.database.user.entity.User;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenService refreshTokenService;

  @Transactional
  public AuthenticationResponse register(RegisterRequest request, ClientType clientType) {
    String email = normalizeEmail(request.getEmail());
    if (repository.existsByEmail(email)) {
      throw new DuplicateEmailException(email);
    }

    User user = User.builder()
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .email(email)
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER)
        .build();

    repository.save(user);
    return completeAuthentication(user, clientType, true);
  }

  @Transactional
  public AuthenticationResponse authenticate(AuthenticationRequest request, ClientType clientType) {
    String email = normalizeEmail(request.getEmail());

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, request.getPassword()));

    User user = repository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    return completeAuthentication(user, clientType, true);
  }

  @Transactional
  public AuthenticationResponse completeAuthentication(
      User user,
      ClientType clientType,
      boolean updateLastLogin) {
    if (updateLastLogin) {
      user.setLastLoginAt(LocalDateTime.now());
    }
    repository.save(user);

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = refreshTokenService.createRefreshToken(user.getEmail(), clientType);

    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .userId(user.getId())
        .build();
  }

  public AuthenticationResponse refreshTokens(AccessTokenRequest request, ClientType clientType) {
    return refreshTokens(request.getRefreshToken(), clientType);
  }

  @Transactional
  public AuthenticationResponse refreshTokens(String refreshToken, ClientType clientType) {
    String email = refreshTokenService.validateAndGetEmail(refreshToken, clientType);
    if (email == null) {
      throw new InvalidRefreshTokenException();
    }

    User user = repository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    refreshTokenService.deleteRefreshToken(refreshToken);
    return completeAuthentication(user, clientType, false);
  }

  public void logout(LogoutRequest request) {
    logout(request.getRefreshToken());
  }

  public void logout(String refreshToken) {
    if (refreshToken != null && !refreshToken.isBlank()) {
      refreshTokenService.deleteRefreshToken(refreshToken);
    }
  }

  static String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }
}
