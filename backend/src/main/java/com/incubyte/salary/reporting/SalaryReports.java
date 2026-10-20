package com.incubyte.salary.reporting;

import com.incubyte.salary.employee.EmployeeQueries;
import com.incubyte.salary.employee.PopulationFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaryReports {
  private final NamedParameterJdbcTemplate jdbc;
  private final ReferenceDataService referenceData;

  SalaryReports(NamedParameterJdbcTemplate jdbc, ReferenceDataService referenceData) {
    this.jdbc = jdbc;
    this.referenceData = referenceData;
  }

  public record Statistics(
      String currency,
      long count,
      String total,
      String average,
      String median,
      String minimum,
      String maximum) {
    static Statistics empty(String currency) {
      return new Statistics(currency, 0, "0.00", null, null, null, null);
    }
  }

  public record DepartmentCost(
      String code, String name, String currency, long count, String total) {}

  public record Summary(
      long headcount,
      List<Statistics> nativeGroups,
      Statistics usdEstimate,
      List<DepartmentCost> departments,
      List<ReferenceDataService.Rate> rates,
      String estimateLabel,
      Instant asOf) {}

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Summary summary(PopulationFilter filter) {
    referenceData.validate(filter);
    List<Statistics> nativeGroups = statistics(filter, false);
    long count = nativeGroups.stream().mapToLong(Statistics::count).sum();
    if (nativeGroups.isEmpty() && !filter.currency().isEmpty())
      nativeGroups = List.of(Statistics.empty(filter.currency()));
    Statistics usd = statistics(filter, true).stream().findFirst().orElse(Statistics.empty("USD"));
    PopulationFilter.Sql query = filter.sql();
    String currency = filter.usd() ? "'USD'" : "e.currency_code";
    String amount = filter.usd() ? "e.annual_salary * f.usd_per_unit" : "e.annual_salary";
    List<DepartmentCost> departments =
        jdbc.query(
            "SELECT d.code, d.name, "
                + currency
                + " AS currency, count(*) AS count, sum("
                + amount
                + ") AS total"
                + EmployeeQueries.JOINS
                + query.predicate()
                + " GROUP BY d.code, d.name"
                + (filter.usd() ? "" : ", e.currency_code")
                + " ORDER BY currency, total DESC, d.code",
            query.parameters(),
            (rs, row) ->
                new DepartmentCost(
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("currency"),
                    rs.getLong("count"),
                    Money.display(rs.getBigDecimal("total"))));
    return new Summary(
        count,
        nativeGroups,
        usd,
        departments,
        referenceData.referenceData().rates(),
        "Illustrative USD estimate — fixed assessment rates, not live FX",
        Instant.now());
  }

  private List<Statistics> statistics(PopulationFilter filter, boolean usd) {
    PopulationFilter.Sql query = filter.sql();
    String currency = usd ? "'USD'" : "e.currency_code";
    String amount = usd ? "e.annual_salary * f.usd_per_unit" : "e.annual_salary";
    String sql =
        "WITH population AS (SELECT e.id, "
            + currency
            + " AS currency, "
            + amount
            + " AS amount"
            + EmployeeQueries.JOINS
            + query.predicate()
            + "), "
            + "ordered AS (SELECT *, row_number() OVER (PARTITION BY currency ORDER BY amount, id) AS ordinal, "
            + "count(*) OVER (PARTITION BY currency) AS population_size FROM population) "
            + "SELECT currency, count(*) AS count, sum(amount) AS total, avg(amount) AS average, "
            + "min(amount) AS minimum, max(amount) AS maximum, "
            + "avg(amount) FILTER (WHERE ordinal IN ((population_size + 1) / 2, (population_size + 2) / 2)) AS median "
            + "FROM ordered GROUP BY currency ORDER BY currency";
    return jdbc.query(sql, query.parameters(), SalaryReports::statisticsRow);
  }

  private static Statistics statisticsRow(ResultSet rs, int row) throws SQLException {
    return new Statistics(
        rs.getString("currency"),
        rs.getLong("count"),
        Money.display(rs.getBigDecimal("total")),
        Money.display(rs.getBigDecimal("average")),
        Money.display(rs.getBigDecimal("median")),
        Money.display(rs.getBigDecimal("minimum")),
        Money.display(rs.getBigDecimal("maximum")));
  }
}
