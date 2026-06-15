package com.andys_portfolio.auth_demo.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andys_portfolio.auth_demo.auth.service.RefreshTokenCookieService;
import com.andys_portfolio.auth_demo.auth.service.RefreshTokenService;
import com.andys_portfolio.auth_demo.database.user.repository.UserRepository;
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
class WebAuthenticationControllerTest {

  private static final String EMAIL = "web-controller@example.com";
  private static final String PASSWORD = "password123";
  private static final String BASE_PATH = "/api/v1/auth/web";

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
  void registerReturnsAccessTokenAndSetsRefreshCookie() throws Exception {
    MvcResult result = mockMvc.perform(post(BASE_PATH + "/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.userId").isNumber())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(cookie().exists(RefreshTokenCookieService.COOKIE_NAME))
        .andReturn();

    assertThat(extractRefreshToken(result)).isNotBlank();
  }

  @Test
  void loginReturnsAccessTokenAndSetsRefreshCookie() throws Exception {
    registerUser();

    mockMvc.perform(post(BASE_PATH + "/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.userId").isNumber())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(cookie().exists(RefreshTokenCookieService.COOKIE_NAME));
  }

  @Test
  void refreshTokenRotatesCookieWhenAjaxHeaderPresent() throws Exception {
    String refreshToken = extractRefreshToken(registerUser());

    MvcResult result = mockMvc.perform(post(BASE_PATH + "/refresh-token")
            .header("X-Requested-With", "XMLHttpRequest")
            .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(cookie().exists(RefreshTokenCookieService.COOKIE_NAME))
        .andReturn();

    String newRefreshToken = extractRefreshToken(result);
    assertThat(newRefreshToken).isNotEqualTo(refreshToken);
  }

  @Test
  void refreshTokenWithoutAjaxHeaderReturnsForbidden() throws Exception {
    String refreshToken = extractRefreshToken(registerUser());

    mockMvc.perform(post(BASE_PATH + "/refresh-token")
            .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
        .andExpect(status().isForbidden());
  }

  @Test
  void logoutClearsRefreshCookie() throws Exception {
    String refreshToken = extractRefreshToken(registerUser());

    mockMvc.perform(post(BASE_PATH + "/logout")
            .header("X-Requested-With", "XMLHttpRequest")
            .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(RefreshTokenCookieService.COOKIE_NAME, 0));
  }

  @Test
  void refreshAfterLogoutReturnsUnauthorized() throws Exception {
    String refreshToken = extractRefreshToken(registerUser());

    mockMvc.perform(post(BASE_PATH + "/logout")
            .header("X-Requested-With", "XMLHttpRequest")
            .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
        .andExpect(status().isOk());

    mockMvc.perform(post(BASE_PATH + "/refresh-token")
            .header("X-Requested-With", "XMLHttpRequest")
            .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken)))
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

  private String registerJson() throws Exception {
    return objectMapper.writeValueAsString(Map.of(
        "firstName", "Web",
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
