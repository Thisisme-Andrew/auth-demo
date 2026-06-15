package com.andys_portfolio.auth_demo.auth.exception;

public class AuthenticationRequiredException extends RuntimeException {

  public AuthenticationRequiredException() {
    super("Authentication required");
  }
}