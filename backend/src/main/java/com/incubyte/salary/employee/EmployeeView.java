package com.incubyte.salary.employee;

import com.incubyte.salary.reporting.Money;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public record EmployeeView(
    UUID id,
    String employeeCode,
    String firstName,
    String lastName,
    String email,
    String countryCode,
    String countryName,
    String departmentCode,
    String departmentName,
    String annualSalary,
    String currencyCode,
    String usdEstimate,
    boolean active,
    long version,
    Instant updatedAt) {
  static EmployeeView fromRow(ResultSet rs, int row) throws SQLException {
    return new EmployeeView(
        rs.getObject("id", UUID.class),
        rs.getString("employee_code"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getString("email"),
        rs.getString("country_code"),
        rs.getString("country_name"),
        rs.getString("department_code"),
        rs.getString("department_name"),
        Money.display(rs.getBigDecimal("annual_salary")),
        rs.getString("currency_code"),
        Money.display(
            Money.toUsd(rs.getBigDecimal("annual_salary"), rs.getBigDecimal("usd_per_unit"))),
        rs.getBoolean("active"),
        rs.getLong("version"),
        rs.getTimestamp("updated_at").toInstant());
  }
}
