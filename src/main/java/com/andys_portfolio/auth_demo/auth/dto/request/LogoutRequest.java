package com.andys_portfolio.auth_demo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

  @NotBlank
  private String refreshToken;
}
