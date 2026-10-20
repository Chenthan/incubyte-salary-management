package com.incubyte.salary.reporting;

import com.incubyte.salary.employee.PopulationFilter;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {
  private final SalaryReports reports;

  ReportController(SalaryReports reports) {
    this.reports = reports;
  }

  @GetMapping("/api/reports/salary-summary")
  SalaryReports.Summary summary(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String country,
      @RequestParam(defaultValue = "") String department,
      @RequestParam(defaultValue = "ACTIVE") String status,
      @RequestParam(defaultValue = "") String currency,
      @RequestParam(required = false) BigDecimal minSalary,
      @RequestParam(defaultValue = "true") boolean usd) {
    return reports.summary(
        new PopulationFilter(q, country, department, status, currency, minSalary, usd));
  }
}
