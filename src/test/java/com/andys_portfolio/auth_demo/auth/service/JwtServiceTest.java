package com.andys_portfolio.auth_demo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andys_portfolio.auth_demo.database.user.entity.Role;
import com.andys_portfolio.auth_demo.database.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
class JwtServiceTest {

  private static final String EMAIL = "user@example.com";

  @Autowired
  private JwtService jwtService;

  private static User buildUser(String email) {
    return User.builder()
        .email(email)
        .password("encoded-password")
        .role(Role.USER)
        .build();
  }

  @Test
  void generateAccessTokenReturnsNonBlankToken() {
    String token = jwtService.generateAccessToken(buildUser(EMAIL));

    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  void extractUsernameReturnsEmailFromSubject() {
    String token = jwtService.generateAccessToken(buildUser(EMAIL));

    assertThat(jwtService.extractUsername(token)).isEqualTo(EMAIL);
  }

  @Test
  void isTokenValidReturnsTrueForMatchingUser() {
    User user = buildUser(EMAIL);
    String token = jwtService.generateAccessToken(user);

    assertThat(jwtService.isTokenValid(token, user)).isTrue();
  }

  @Test
  void isTokenValidReturnsFalseForDifferentUser() {
    User user = buildUser(EMAIL);
    User otherUser = buildUser("other@example.com");
    String token = jwtService.generateAccessToken(user);

    assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
  }

  @Test
  void expirationIsWithinFifteenMinutes() {
    String token = jwtService.generateAccessToken(buildUser(EMAIL));

    Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
    long ttlMs = expiration.getTime() - System.currentTimeMillis();

    assertThat(ttlMs).isBetween(890_000L, 900_000L);
  }

  @Test
  void extractUsernameThrowsForTamperedToken() {
    String token = jwtService.generateAccessToken(buildUser(EMAIL));
    String tampered = token.substring(0, token.length() - 1) + "X";

    assertThatThrownBy(() -> jwtService.extractUsername(tampered))
        .isInstanceOf(JwtException.class);
  }

  @Nested
  @SpringBootTest(properties = {
      "spring.docker.compose.skip.in-tests=false",
      "jwt.access-token-expiration=-1000"
  })
  class ExpiredTokenTests {

    @Autowired
    private JwtService jwtService;

    @Test
    void isTokenValidThrowsForExpiredToken() {
      User user = buildUser(EMAIL);
      String token = jwtService.generateAccessToken(user);

      assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
          .isInstanceOf(ExpiredJwtException.class);
    }
  }
}
