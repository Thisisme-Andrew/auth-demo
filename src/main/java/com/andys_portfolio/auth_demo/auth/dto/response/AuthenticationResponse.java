package com.andys_portfolio.auth_demo.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationResponse {

  private String accessToken;
  private String refreshToken;
  private Long userId;
}
