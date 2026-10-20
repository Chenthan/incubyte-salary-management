package com.incubyte.salary.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(JdbcTemplate jdbc) {
    return username ->
        jdbc
            .query(
                "SELECT username, password_hash FROM hr_users WHERE username = ?",
                (rs, row) ->
                    User.withUsername(rs.getString("username"))
                        .password(rs.getString("password_hash"))
                        .roles("HR")
                        .build(),
                username)
            .stream()
            .findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/csrf",
                        "/api/auth/login",
                        "/actuator/health/readiness",
                        "/actuator/health/liveness")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .denyAll()
                    .requestMatchers("/api/**")
                    .hasRole("HR")
                    .anyRequest()
                    .permitAll())
        .csrf(Customizer.withDefaults())
        .formLogin(
            form ->
                form.loginPage("/login")
                    .loginProcessingUrl("/api/auth/login")
                    .successHandler(
                        (req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT))
                    .failureHandler(
                        (req, res, ex) ->
                            problem(res, 401, "INVALID_CREDENTIALS", "Sign-in failed.")))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(
                        (req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT)))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (req, res, ex) -> problem(res, 401, "UNAUTHENTICATED", "Please sign in."))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            problem(
                                res, 403, "FORBIDDEN", "Request denied. Refresh and try again.")))
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'")))
        .build();
  }

  private static void problem(HttpServletResponse response, int status, String code, String detail)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response
        .getWriter()
        .write(
            "{\"status\":" + status + ",\"code\":\"" + code + "\",\"detail\":\"" + detail + "\"}");
  }
}
