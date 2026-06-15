package com.andys_portfolio.auth_demo.auth.exception;

public class DuplicateEmailException extends RuntimeException {

  public DuplicateEmailException(String email) {
    super("Email already registered: " + email);
  }
}
