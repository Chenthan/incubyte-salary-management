package com.incubyte.salary.employee;

import com.incubyte.salary.common.ApiException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/** The sole predicate builder for employee pages and reports. Identifiers never come from input. */
public record PopulationFilter(
    String q,
    String country,
    String department,
    String status,
    String currency,
    BigDecimal minSalary,
    boolean usd) {

  public record Sql(String predicate, Map<String, Object> parameters) {}

  public PopulationFilter {
    q = q == null ? "" : q.strip();
    country = country == null ? "" : country;
    department = department == null ? "" : department;
    currency = currency == null ? "" : currency;
    status = status == null ? "ACTIVE" : status;
    if (q.length() > 100
        || !Set.of("ACTIVE", "INACTIVE", "ALL").contains(status)
        || !country.matches("[A-Z]{2}|^$")
        || !department.matches("[A-Z_]{1,30}|^$")
        || !currency.matches("[A-Z]{3}|^$")) {
      throw invalid("Invalid population filter.");
    }
    if (minSalary != null
        && (minSalary.signum() < 0
            || minSalary.scale() > 2
            || minSalary.compareTo(new BigDecimal("9999999999.99")) > 0)) {
      throw invalid("Salary threshold must be a nonnegative amount with up to two decimal places.");
    }
    if (minSalary != null && !usd && currency.isEmpty()) {
      throw invalid("Select a native currency before applying a native salary threshold.");
    }
  }

  public Sql sql() {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    Map<String, Object> values = new LinkedHashMap<>();
    if (!q.isEmpty()) {
      where.append(
          " AND (concat_ws(' ', e.first_name, e.last_name) ILIKE :q ESCAPE '\\' OR e.employee_code ILIKE :q ESCAPE '\\')");
      values.put("q", "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
    }
    if (!status.equals("ALL")) {
      where.append(" AND e.active = :active");
      values.put("active", status.equals("ACTIVE"));
    }
    if (!country.isEmpty()) {
      where.append(" AND e.country_code = :country");
      values.put("country", country);
    }
    if (!department.isEmpty()) {
      where.append(" AND e.department_code = :department");
      values.put("department", department);
    }
    if (!currency.isEmpty()) {
      where.append(" AND e.currency_code = :currency");
      values.put("currency", currency);
    }
    if (minSalary != null) {
      where.append(
          usd
              ? " AND e.annual_salary * f.usd_per_unit > :threshold"
              : " AND e.annual_salary > :threshold");
      values.put("threshold", minSalary);
    }
    return new Sql(where.toString(), values);
  }

  public static String ordering(String sort, String direction, boolean usd) {
    Map<String, String> columns =
        Map.of(
            "name",
            "lower(e.last_name), lower(e.first_name)",
            "code",
            "e.employee_code",
            "salary",
            usd ? "e.annual_salary * f.usd_per_unit" : "e.currency_code, e.annual_salary",
            "country",
            "c.name",
            "department",
            "d.name");
    String column = columns.get(sort);
    String order = direction.toUpperCase(Locale.ROOT);
    if (column == null || !Set.of("ASC", "DESC").contains(order))
      throw invalid("Invalid sort order.");
    return " ORDER BY " + column.replace(", ", " " + order + ", ") + " " + order + ", e.id ASC";
  }

  private static ApiException invalid(String detail) {
    return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", detail);
  }
}
