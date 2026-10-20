package com.incubyte.salary.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
  private final EmployeeService service;
  private final EmployeeQueries queries;

  EmployeeController(EmployeeService service, EmployeeQueries queries) {
    this.service = service;
    this.queries = queries;
  }

  @GetMapping
  EmployeeQueries.EmployeePage list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String country,
      @RequestParam(defaultValue = "") String department,
      @RequestParam(defaultValue = "ACTIVE") String status,
      @RequestParam(defaultValue = "") String currency,
      @RequestParam(required = false) BigDecimal minSalary,
      @RequestParam(defaultValue = "true") boolean usd,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "asc") String direction) {
    return queries.page(
        new PopulationFilter(q, country, department, status, currency, minSalary, usd),
        page,
        size,
        sort,
        direction);
  }

  @GetMapping("/{id}")
  EmployeeView detail(@PathVariable UUID id) {
    return queries.detail(id);
  }

  @PostMapping
  ResponseEntity<EmployeeView> create(@Valid @RequestBody EmployeeInput input) {
    EmployeeView result = service.create(input);
    return ResponseEntity.created(URI.create("/api/employees/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  EmployeeView update(@PathVariable UUID id, @Valid @RequestBody EmployeeInput input) {
    return service.update(id, input);
  }

  public record StatusInput(@NotNull Boolean active, @NotNull @PositiveOrZero Long version) {}

  @PatchMapping("/{id}/status")
  EmployeeView status(@PathVariable UUID id, @Valid @RequestBody StatusInput input) {
    return service.status(id, input.active(), input.version());
  }
}
