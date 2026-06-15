package com.andys_portfolio.auth_demo.core.configs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andys_portfolio.auth_demo.auth.ClientType;
import com.andys_portfolio.auth_demo.auth.dto.request.RegisterRequest;
import com.andys_portfolio.auth_demo.auth.service.AuthenticationService;
import com.andys_portfolio.auth_demo.auth.service.RefreshTokenService;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

  private static final String EMAIL = "security-test@example.com";
  private static final String PASSWORD = "password123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AuthenticationService authenticationService;

  @Autowired
  private RefreshTokenService refreshTokenService;

  @Autowired
  private UserRepository userRepository;

  private String accessToken;

  @BeforeEach
  void setUp() {
    var request = new RegisterRequest();
    request.setFirstName("Security");
    request.setLastName("Test");
    request.setEmail(EMAIL);
    request.setPassword(PASSWORD);
    accessToken = authenticationService.register(request, ClientType.MOBILE).getAccessToken();
  }

  @AfterEach
  void tearDown() {
    refreshTokenService.invalidateAllForEmail(EMAIL);
    userRepository.deleteAll();
  }

  @Test
  void protectedEndpointWithoutTokenReturnsUnauthorizedJson() throws Exception {
    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Authentication required"));
  }

  @Test
  void protectedEndpointWithValidBearerTokenReturnsOk() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.firstName").value("Security"))
        .andExpect(jsonPath("$.lastName").value("Test"));
  }

  @Test
  void protectedEndpointWithInvalidBearerTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Authentication required"));
  }

  @Test
  void actuatorHealthIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Nested
  @SpringBootTest(properties = {
      "spring.docker.compose.skip.in-tests=false",
      "spring.jpa.hibernate.ddl-auto=update",
      "jwt.access-token-expiration=-1000"
  })
  @AutoConfigureMockMvc
  class ExpiredAccessTokenTests {

    private static final String EXPIRED_EMAIL = "expired-security-test@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
      refreshTokenService.invalidateAllForEmail(EXPIRED_EMAIL);
      userRepository.deleteAll();
    }

    @Test
    void protectedEndpointWithExpiredBearerTokenReturnsUnauthorized() throws Exception {
      var request = new RegisterRequest();
      request.setFirstName("Security");
      request.setLastName("Test");
      request.setEmail(EXPIRED_EMAIL);
      request.setPassword(PASSWORD);
      String expiredToken = authenticationService.register(request, ClientType.MOBILE).getAccessToken();

      mockMvc.perform(get("/api/v1/users/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.error").value("Authentication required"));
    }
  }
}
