package com.andys_portfolio.auth_demo.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JsonAuthenticationEntryPointTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonAuthenticationEntryPoint entryPoint =
      new JsonAuthenticationEntryPoint(objectMapper);

  @Test
  void commenceReturnsUnauthorizedJson() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new BadCredentialsException("test"));

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

    @SuppressWarnings("unchecked")
    Map<String, String> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
    assertThat(body).containsEntry("error", "Authentication required");
  }
}
