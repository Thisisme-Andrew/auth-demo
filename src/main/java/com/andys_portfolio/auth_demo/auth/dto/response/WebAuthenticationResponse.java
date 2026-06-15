package com.andys_portfolio.auth_demo.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebAuthenticationResponse {

  private String accessToken;
  private Long userId;
}
