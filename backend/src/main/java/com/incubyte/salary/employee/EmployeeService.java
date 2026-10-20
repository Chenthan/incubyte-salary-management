package com.incubyte.salary.employee;

import com.incubyte.salary.common.ApiException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {
  private final EmployeeRepository repository;
  private final EmployeeQueries queries;
  private final JdbcTemplate jdbc;

  EmployeeService(EmployeeRepository repository, EmployeeQueries queries, JdbcTemplate jdbc) {
    this.repository = repository;
    this.queries = queries;
    this.jdbc = jdbc;
  }

  @Transactional
  public EmployeeView create(EmployeeInput input) {
    Employee employee = new Employee(input.employeeCode());
    apply(employee, input);
    repository.saveAndFlush(employee);
    return queries.detail(employee.id);
  }

  @Transactional
  public EmployeeView update(UUID id, EmployeeInput input) {
    Employee employee = existing(id, input.version());
    if (!employee.employeeCode.equals(input.employeeCode())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "IMMUTABLE_CODE", "Employee code cannot be changed.");
    }
    apply(employee, input);
    repository.flush();
    return queries.detail(id);
  }

  @Transactional
  public EmployeeView status(UUID id, boolean active, Long version) {
    Employee employee = existing(id, version);
    employee.active = active;
    repository.flush();
    return queries.detail(id);
  }

  private Employee existing(UUID id, Long expectedVersion) {
    if (expectedVersion == null)
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "VERSION_REQUIRED", "Supply the employee version.");
    Employee employee =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "Employee not found."));
    if (!Objects.equals(employee.version, expectedVersion)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "STALE_EMPLOYEE",
          "This employee changed. Reload before applying your edit.");
    }
    return employee;
  }

  private void apply(Employee employee, EmployeeInput input) {
    boolean known =
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM countries WHERE code = ?) AND EXISTS(SELECT 1 FROM departments WHERE code = ?) AND EXISTS(SELECT 1 FROM fx_rates WHERE currency_code = ?)",
            Boolean.class,
            input.countryCode(),
            input.departmentCode(),
            input.currencyCode());
    if (!known)
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "UNKNOWN_REFERENCE",
          "Choose a supported country, department, and currency.");
    employee.firstName = input.firstName().strip();
    employee.lastName = input.lastName().strip();
    employee.email = input.email().strip().toLowerCase(Locale.ROOT);
    employee.countryCode = input.countryCode();
    employee.departmentCode = input.departmentCode();
    employee.annualSalary = input.annualSalary();
    employee.currencyCode = input.currencyCode();
  }
}
