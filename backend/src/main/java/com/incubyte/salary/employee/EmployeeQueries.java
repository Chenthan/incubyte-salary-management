package com.incubyte.salary.employee;

import com.incubyte.salary.common.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeQueries {
  public static final String JOINS =
      " FROM employees e JOIN countries c ON c.code = e.country_code JOIN departments d ON d.code = e.department_code LEFT JOIN fx_rates f ON f.currency_code = e.currency_code";
  private static final String COLUMNS =
      "SELECT e.*, c.name AS country_name, d.name AS department_name, f.usd_per_unit";
  private final NamedParameterJdbcTemplate jdbc;

  public EmployeeQueries(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record EmployeePage(
      List<EmployeeView> content, long totalElements, int page, int size, long totalPages) {}

  public EmployeePage page(
      PopulationFilter filter, int page, int size, String sort, String direction) {
    if (page < 0 || page > 100000 || size < 1 || size > 100) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be nonnegative; size must be 1–100.");
    }
    PopulationFilter.Sql query = filter.sql();
    Map<String, Object> parameters = new LinkedHashMap<>(query.parameters());
    parameters.put("limit", size);
    parameters.put("offset", (long) page * size);
    String order = PopulationFilter.ordering(sort, direction, filter.usd());
    long count =
        jdbc.queryForObject("SELECT count(*)" + JOINS + query.predicate(), parameters, Long.class);
    List<EmployeeView> content =
        jdbc.query(
            COLUMNS + JOINS + query.predicate() + order + " LIMIT :limit OFFSET :offset",
            parameters,
            EmployeeView::fromRow);
    return new EmployeePage(content, count, page, size, (count + size - 1) / size);
  }

  public EmployeeView detail(UUID id) {
    return jdbc
        .query(COLUMNS + JOINS + " WHERE e.id = :id", Map.of("id", id), EmployeeView::fromRow)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "Employee not found."));
  }
}
