package com.andys_portfolio.auth_demo.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andys_portfolio.auth_demo.auth.service.RefreshTokenService;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureMockMvc
class MobileAuthenticationControllerTest {

  private static final String EMAIL = "mobile-controller@example.com";
  private static final String PASSWORD = "password123";
  private static final String BASE_PATH = "/api/v1/auth/mobile";

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
  void registerReturnsAccessAndRefreshTokens() throws Exception {
    mockMvc.perform(post(BASE_PATH + "/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.userId").isNumber());
  }

  @Test
  void loginReturnsAccessAndRefreshTokens() throws Exception {
    registerUser();

    mockMvc.perform(post(BASE_PATH + "/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.userId").isNumber());
  }

  @Test
  void refreshTokenRotatesTokens() throws Exception {
    JsonNode registered = readJson(registerUser());
    String oldRefreshToken = registered.get("refreshToken").asText();

    MvcResult refreshed = mockMvc.perform(post(BASE_PATH + "/refresh-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", oldRefreshToken))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andReturn();

    String newRefreshToken = readJson(refreshed).get("refreshToken").asText();
    assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
  }

  @Test
  void webRefreshTokenRejectedOnMobileEndpoint() throws Exception {
    MvcResult webRegistered = mockMvc.perform(post("/api/v1/auth/web/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson()))
        .andExpect(status().isOk())
        .andReturn();

    String webRefreshToken = extractRefreshToken(webRegistered);

    mockMvc.perform(post(BASE_PATH + "/refresh-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", webRefreshToken))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid or expired refresh token"));
  }

  @Test
  void logoutInvalidatesRefreshToken() throws Exception {
    JsonNode registered = readJson(registerUser());
    String refreshToken = registered.get("refreshToken").asText();

    mockMvc.perform(post(BASE_PATH + "/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
        .andExpect(status().isOk());

    mockMvc.perform(post(BASE_PATH + "/refresh-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid or expired refresh token"));
  }

  private MvcResult registerUser() throws Exception {
    return mockMvc.perform(post(BASE_PATH + "/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson()))
        .andExpect(status().isOk())
        .andReturn();
  }

  private JsonNode readJson(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String registerJson() throws Exception {
    return objectMapper.writeValueAsString(Map.of(
        "firstName", "Mobile",
        "lastName", "User",
        "email", EMAIL,
        "password", PASSWORD));
  }

  private String loginJson() throws Exception {
    return objectMapper.writeValueAsString(Map.of(
        "email", EMAIL,
        "password", PASSWORD));
  }

  private static String extractRefreshToken(MvcResult result) {
    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    int valueStart = setCookie.indexOf('=') + 1;
    int valueEnd = setCookie.indexOf(';');
    return setCookie.substring(valueStart, valueEnd);
  }
}
