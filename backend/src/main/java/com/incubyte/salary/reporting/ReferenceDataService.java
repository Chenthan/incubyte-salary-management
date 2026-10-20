package com.incubyte.salary.reporting;

import com.incubyte.salary.common.ApiException;
import com.incubyte.salary.employee.PopulationFilter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReferenceDataService {
  private final JdbcTemplate jdbc;

  ReferenceDataService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Option(String code, String label) {}

  public record Rate(String currency, String usdPerUnit, String version, String effectiveDate) {}

  public record ReferenceData(List<Option> countries, List<Option> departments, List<Rate> rates) {}

  public void validate(PopulationFilter filter) {
    boolean known =
        jdbc.queryForObject(
            """
        SELECT (? = '' OR EXISTS(SELECT 1 FROM countries WHERE code = ?))
          AND (? = '' OR EXISTS(SELECT 1 FROM departments WHERE code = ?))
          AND (? = '' OR EXISTS(SELECT 1 FROM fx_rates WHERE currency_code = ?))
        """,
            Boolean.class,
            filter.country(),
            filter.country(),
            filter.department(),
            filter.department(),
            filter.currency(),
            filter.currency());
    if (!known)
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "INVALID_FILTER",
          "Choose supported country, department, and currency filters.");
  }

  public ReferenceData referenceData() {
    return new ReferenceData(
        jdbc.query(
            "SELECT code, name FROM countries ORDER BY name",
            (rs, row) -> new Option(rs.getString(1), rs.getString(2))),
        jdbc.query(
            "SELECT code, name FROM departments ORDER BY name",
            (rs, row) -> new Option(rs.getString(1), rs.getString(2))),
        jdbc.query(
            "SELECT currency_code, usd_per_unit, version, effective_date FROM fx_rates ORDER BY currency_code",
            (rs, row) ->
                new Rate(
                    rs.getString(1),
                    rs.getBigDecimal(2).toPlainString(),
                    rs.getString(3),
                    rs.getDate(4).toString())));
  }
}
