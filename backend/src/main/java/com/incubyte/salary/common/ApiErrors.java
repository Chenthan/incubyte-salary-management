package com.incubyte.salary.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(ApiException.class)
  ProblemDetail business(ApiException ex) {
    return problem(ex.status(), ex.code(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
    ProblemDetail result =
        problem(HttpStatus.BAD_REQUEST, "VALIDATION", "Check the highlighted fields.");
    result.setProperty("fieldErrors", fields);
    return result;
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail malformed(Exception ex) {
    return problem(
        HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "One or more request values are invalid.");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail stale(OptimisticLockingFailureException ex) {
    return problem(
        HttpStatus.CONFLICT,
        "STALE_EMPLOYEE",
        "This employee changed. Reload before applying your edit.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail constraint(DataIntegrityViolationException ex) {
    return problem(
        HttpStatus.CONFLICT,
        "EMPLOYEE_CONFLICT",
        "Employee code or email already exists, or a data constraint was violated.");
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
    result.setProperty("code", code);
    return result;
  }
}
