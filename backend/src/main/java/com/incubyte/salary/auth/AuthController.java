package com.incubyte.salary.auth;

import java.security.Principal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  public record CsrfResponse(String token, String headerName) {}

  public record Identity(String username) {}

  @GetMapping("/csrf")
  CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken(), token.getHeaderName());
  }

  @GetMapping("/me")
  Identity me(Principal principal) {
    return new Identity(principal.getName());
  }
}
