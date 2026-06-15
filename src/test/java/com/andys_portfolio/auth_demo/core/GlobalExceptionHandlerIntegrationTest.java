package com.andys_portfolio.auth_demo.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andys_portfolio.auth_demo.auth.service.RefreshTokenService;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

  private static final String EMAIL = "exception-handler@example.com";
  private static final String PASSWORD = "password123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

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
  void duplicateRegisterReturnsConflict() throws Exception {
    register();

    mockMvc.perform(post("/api/v1/auth/mobile/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Email already registered: " + EMAIL));
  }

  @Test
  void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
    register();

    mockMvc.perform(post("/api/v1/auth/mobile/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "email", EMAIL,
                "password", "wrong-password"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid email or password"));
  }

  @Test
  void refreshWithInvalidTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/auth/mobile/refresh-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-token"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid or expired refresh token"));
  }

  private void register() throws Exception {
    mockMvc.perform(post("/api/v1/auth/mobile/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson()))
        .andExpect(status().isOk());
  }

  private String registerJson() throws Exception {
    return objectMapper.writeValueAsString(Map.of(
        "firstName", "Exception",
        "lastName", "Handler",
        "email", EMAIL,
        "password", PASSWORD));
  }
}
