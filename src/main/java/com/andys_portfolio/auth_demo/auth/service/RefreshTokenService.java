package com.andys_portfolio.auth_demo.auth.service;

import com.andys_portfolio.auth_demo.auth.ClientType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final StringRedisTemplate redisTemplate;
  private static final String REFRESHTOKEN_PREFIX = "refresh:";
  private static final String USER_TOKENS_PREFIX = "refresh-user:";
  private static final String CLIENT_EMAIL_SEPARATOR = "|";

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  public String createRefreshToken(String email, ClientType clientType) {
    String token = UUID.randomUUID().toString();
    String value = encodeValue(clientType, email);
    String tokenKey = REFRESHTOKEN_PREFIX + token;
    String userTokensKey = USER_TOKENS_PREFIX + email;

    redisTemplate.opsForValue().set(tokenKey, value, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    redisTemplate.opsForSet().add(userTokensKey, token);
    redisTemplate.expire(userTokensKey, refreshTokenExpiration, TimeUnit.MILLISECONDS);

    return token;
  }

  public String validateAndGetEmail(String token, ClientType clientType) {
    if (token == null || token.isEmpty()) {
      return null;
    }

    String stored = redisTemplate.opsForValue().get(REFRESHTOKEN_PREFIX + token);
    if (stored == null) {
      return null;
    }

    return decodeEmail(stored, clientType);
  }

  public void deleteRefreshToken(String token) {
    if (token == null || token.isBlank()) {
      return;
    }
    String tokenKey = REFRESHTOKEN_PREFIX + token;
    String stored = redisTemplate.opsForValue().get(tokenKey);
    if (stored != null) {
      String email = extractEmail(stored);
      if (email != null) {
        redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + email, token);
      }
    }
    redisTemplate.delete(tokenKey);
  }

  public void invalidateAllForEmail(String email) {
    if (email == null || email.isBlank()) {
      return;
    }
    String userTokensKey = USER_TOKENS_PREFIX + email;
    Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
    if (tokens != null) {
      for (String token : tokens) {
        redisTemplate.delete(REFRESHTOKEN_PREFIX + token);
      }
    }
    redisTemplate.delete(userTokensKey);
  }

  private static String encodeValue(ClientType clientType, String email) {
    return clientType.name() + CLIENT_EMAIL_SEPARATOR + email;
  }

  private static String decodeEmail(String stored, ClientType clientType) {
    // This is in the case that the cache was stored incorrectly, users has to log in again
    int separatorIndex = stored.indexOf(CLIENT_EMAIL_SEPARATOR);
    if (separatorIndex < 0) {
      return null;
    }

    // If user is trying to use token from a differenet client type than what is stored.
    String storedClientType = stored.substring(0, separatorIndex);
    if (!clientType.name().equals(storedClientType)) {
      return null;
    }

    return stored.substring(separatorIndex + 1);
  }

  private static String extractEmail(String stored) {
    int separatorIndex = stored.indexOf(CLIENT_EMAIL_SEPARATOR);
    if (separatorIndex < 0) {
      return null;
    }
    return stored.substring(separatorIndex + 1);
  }
}
