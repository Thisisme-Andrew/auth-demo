package com.andys_portfolio.auth_demo.database.user.controller;

import com.andys_portfolio.auth_demo.database.user.entity.User;
import com.andys_portfolio.auth_demo.database.user.service.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final CurrentUserService currentUserService;

  @GetMapping("/me")
  public Map<String, Object> getCurrentUser() {
    User user = currentUserService.getCurrentUserOrThrow();
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", user.getId());
    response.put("email", user.getEmail());
    response.put("firstName", user.getFirstName());
    response.put("lastName", user.getLastName());
    return response;
  }
}
