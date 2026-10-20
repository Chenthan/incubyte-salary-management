package com.incubyte.salary.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HrBootstrap implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final String username;
  private final String password;

  public HrBootstrap(
      JdbcTemplate jdbc,
      PasswordEncoder encoder,
      @Value("${app.hr.username}") String username,
      @Value("${app.hr.password}") String password) {
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (!username.matches("[A-Za-z0-9._-]{2,80}")
        || password.length() < 16
        || password.length() > 64
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
      throw new IllegalStateException(
          "HR_USERNAME must be 2–80 safe characters; HR_PASSWORD must be 16–64 characters and at most 72 UTF-8 bytes.");
    }
    if (jdbc.queryForObject(
            "SELECT count(*) FROM hr_users WHERE username = ?", Long.class, username)
        == 0) {
      jdbc.update(
          "INSERT INTO hr_users(username, password_hash) VALUES (?, ?) ON CONFLICT DO NOTHING",
          username,
          encoder.encode(password));
    }
  }
}
