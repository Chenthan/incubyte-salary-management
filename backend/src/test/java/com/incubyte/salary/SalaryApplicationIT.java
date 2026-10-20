package com.incubyte.salary;

import static org.assertj.core.api.Assertions.*;

import com.incubyte.salary.demo.DemoSeeder;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.demo.enabled=false",
      "app.hr.username=test-hr",
      "app.hr.password=integration-test-password",
      "server.servlet.session.cookie.secure=false"
    })
class SalaryApplicationIT {
  @Autowired JdbcTemplate jdbc;

  @Value("${local.server.port}")
  int port;

  private final ObjectMapper json = new ObjectMapper();
  private HttpClient client;
  private String csrf;
  private int sequence;

  @BeforeEach
  void reset() {
    // Refuse to truncate a developer/demo database by accident.
    assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).endsWith("_test");
    jdbc.execute("TRUNCATE employees");
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    csrf = null;
    sequence = 0;
  }

  @Test
  void sessionLoginCsrfAndLogoutAreEnforcedOnTheServer() throws Exception {
    assertThat(send("GET", "/api/employees", null, false).statusCode()).isEqualTo(401);
    assertThat(send("POST", "/api/auth/login", Map.of(), false).statusCode()).isEqualTo(403);
    login();
    assertThat(send("GET", "/api/employees", null, false).statusCode()).isEqualTo(200);
    assertThat(send("POST", "/api/employees", input("HR-001", "100.00", null), false).statusCode())
        .isEqualTo(403);
    assertThat(send("POST", "/api/auth/logout", Map.of(), true).statusCode()).isEqualTo(204);
    assertThat(send("GET", "/api/auth/me", null, false).statusCode()).isEqualTo(401);
  }

  @Test
  void salaryUpdateRefreshesReportsAndAStaleEditCannotOverwriteIt() throws Exception {
    login();
    var created = send("POST", "/api/employees", input("HR-001", "100.00", null), true);
    assertThat(created.statusCode()).isEqualTo(201);
    String id = tree(created).path("id").asText();
    long version = tree(created).path("version").asLong();
    assertThat(summary("").path("usdEstimate").path("total").asText()).isEqualTo("100.00");
    assertThat(
            send("PUT", "/api/employees/" + id, input("HR-001", "150.25", version), true)
                .statusCode())
        .isEqualTo(200);
    assertThat(summary("").path("usdEstimate").path("total").asText()).isEqualTo("150.25");
    assertThat(
            send("PUT", "/api/employees/" + id, input("HR-001", "999.00", version), true)
                .statusCode())
        .isEqualTo(409);
    assertThat(tree(send("GET", "/api/employees/" + id, null, false)).path("annualSalary").asText())
        .isEqualTo("150.25");
    assertThat(
            tree(send("PUT", "/api/employees/" + id, input("HR-001", "999.00", version), true))
                .path("code")
                .asText())
        .isEqualTo("STALE_EMPLOYEE");
  }

  @Test
  void concurrentUpdatesHaveOneWinner() throws Exception {
    login();
    var created = tree(send("POST", "/api/employees", input("HR-RACE", "100.00", null), true));
    String path = "/api/employees/" + created.path("id").asText();
    long version = created.path("version").asLong();
    var one =
        client.sendAsync(
            request("PUT", path, input("HR-RACE", "200.00", version), true),
            HttpResponse.BodyHandlers.ofString());
    var two =
        client.sendAsync(
            request("PUT", path, input("HR-RACE", "300.00", version), true),
            HttpResponse.BodyHandlers.ofString());
    CompletableFuture.allOf(one, two).join();
    assertThat(List.of(one.join().statusCode(), two.join().statusCode()))
        .containsExactlyInAnyOrder(200, 409);
    String winner = one.join().statusCode() == 200 ? "200.00" : "300.00";
    assertThat(tree(send("GET", path, null, false)).path("annualSalary").asText())
        .isEqualTo(winner);
  }

  @Test
  void decimalMedianHandlesOddEvenAndWeightedMean() throws Exception {
    login();
    fixture("10.00", "USD", "US", "ENGINEERING", true, "Avery");
    fixture("20.00", "USD", "US", "ENGINEERING", true, "Avery");
    fixture("30.00", "USD", "US", "ENGINEERING", true, "Avery");
    assertThat(summary("").path("usdEstimate").path("median").asText()).isEqualTo("20.00");
    fixture("100.00", "USD", "US", "PRODUCT", true, "Avery");
    var report = summary("").path("usdEstimate");
    assertThat(report.path("median").asText()).isEqualTo("25.00");
    assertThat(report.path("average").asText()).isEqualTo("40.00");
    assertThat(report.path("total").asText()).isEqualTo("160.00");
  }

  @Test
  void mixedCurrenciesAreSeparatedAndUsdDirectionIsCorrect() throws Exception {
    login();
    fixture("100.00", "USD", "US", "ENGINEERING", true, "Avery");
    fixture("100.00", "INR", "IN", "ENGINEERING", true, "Avery");
    var report = summary("");
    assertThat(report.path("nativeGroups").size()).isEqualTo(2);
    assertThat(report.path("usdEstimate").path("total").asText()).isEqualTo("101.20");
    assertThat(report.path("usdEstimate").path("median").asText()).isEqualTo("50.60");
    assertThat(report.path("rates").size()).isEqualTo(6);
    assertThat(report.path("estimateLabel").asText()).contains("Illustrative");
  }

  @Test
  void reportingRoundsTheAggregateRatherThanEachConvertedEmployee() throws Exception {
    login();
    fixture("0.42", "INR", "IN", "ENGINEERING", true, "Avery");
    fixture("0.42", "INR", "IN", "ENGINEERING", true, "Avery");
    assertThat(summary("").path("usdEstimate").path("total").asText()).isEqualTo("0.01");
  }

  @Test
  void emptyPopulationsDoNotInventAverageOrMedian() throws Exception {
    login();
    var report = summary("currency=USD");
    assertThat(report.path("headcount").asLong()).isZero();
    assertThat(report.path("usdEstimate").path("total").asText()).isEqualTo("0.00");
    assertThat(report.path("usdEstimate").path("median").isNull()).isTrue();
    assertThat(report.path("nativeGroups").get(0).path("average").isNull()).isTrue();
  }

  @Test
  void listAndReportShareThresholdCountryDepartmentAndStatusFilters() throws Exception {
    login();
    fixture("10000.00", "INR", "IN", "ENGINEERING", true, "Avery");
    fixture("10000.00", "INR", "IN", "PRODUCT", true, "Avery");
    fixture("10000.00", "INR", "IN", "ENGINEERING", false, "Avery");
    fixture("10.00", "USD", "US", "ENGINEERING", true, "Avery");
    String filters = "country=IN&department=ENGINEERING&minSalary=100&usd=true";
    assertThat(
            tree(send("GET", "/api/employees?" + filters, null, false))
                .path("totalElements")
                .asLong())
        .isEqualTo(1);
    assertThat(summary(filters).path("headcount").asLong()).isEqualTo(1);
    assertThat(summary(filters).path("usdEstimate").path("total").asText()).isEqualTo("120.00");
    assertThat(summary(filters + "&status=ALL").path("headcount").asLong()).isEqualTo(2);
  }

  @Test
  void literalSearchStablePaginationAndInvalidParametersAreHandled() throws Exception {
    login();
    fixture("100.00", "USD", "US", "ENGINEERING", true, "Wild%_Card");
    for (int i = 0; i < 5; i++) fixture("100.00", "USD", "US", "ENGINEERING", true, "Other");
    String q = URLEncoder.encode("%_", StandardCharsets.UTF_8);
    assertThat(
            tree(send("GET", "/api/employees?q=" + q, null, false)).path("totalElements").asLong())
        .isEqualTo(1);
    List<String> ids = new ArrayList<>();
    for (int page = 0; page < 3; page++) {
      var rows =
          tree(send("GET", "/api/employees?sort=salary&size=2&page=" + page, null, false))
              .path("content");
      rows.forEach(row -> ids.add(row.path("id").asText()));
    }
    assertThat(ids).hasSize(6).doesNotHaveDuplicates();
    assertThat(send("GET", "/api/employees?size=101", null, false).statusCode()).isEqualTo(400);
    assertThat(send("GET", "/api/employees?sort=salary%3Bdelete", null, false).statusCode())
        .isEqualTo(400);
    assertThat(
            send("GET", "/api/reports/salary-summary?usd=false&minSalary=100", null, false)
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void inputValidationAndUniquenessAreEnforced() throws Exception {
    login();
    assertThat(send("POST", "/api/employees", input("HR-001", "-1.00", null), true).statusCode())
        .isEqualTo(400);
    assertThat(send("POST", "/api/employees", input("HR-001", "1.001", null), true).statusCode())
        .isEqualTo(400);
    assertThat(send("POST", "/api/employees", input("HR-001", "0.00", null), true).statusCode())
        .isEqualTo(400);
    assertThat(send("POST", "/api/employees", input("HR-001", "10.00", null), true).statusCode())
        .isEqualTo(201);
    assertThat(send("POST", "/api/employees", input("HR-001", "20.00", null), true).statusCode())
        .isEqualTo(409);
  }

  @Test
  void deactivationRetainsEmployeeAndRemovesThemFromActiveReports() throws Exception {
    login();
    var created = tree(send("POST", "/api/employees", input("HR-001", "10.00", null), true));
    String path = "/api/employees/" + created.path("id").asText();
    assertThat(
            send(
                    "PATCH",
                    path + "/status",
                    Map.of("active", false, "version", created.path("version").asLong()),
                    true)
                .statusCode())
        .isEqualTo(200);
    assertThat(summary("").path("headcount").asLong()).isZero();
    assertThat(summary("status=ALL").path("headcount").asLong()).isEqualTo(1);
    assertThat(tree(send("GET", path, null, false)).path("active").asBoolean()).isFalse();
  }

  @Test
  void seedHasTenThousandDeterministicEmployeesAndPreservesEdits() {
    DemoSeeder seed = new DemoSeeder(jdbc, false);
    assertThat(seed.seed()).isEqualTo(10000);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM employees", Long.class)).isEqualTo(10000);
    assertThat(
            jdbc.queryForObject("SELECT count(DISTINCT country_code) FROM employees", Long.class))
        .isEqualTo(6);
    String firstId =
        jdbc.queryForObject("SELECT id::text FROM employees WHERE demo_seed_key = 1", String.class);
    jdbc.update("UPDATE employees SET annual_salary=123.45 WHERE demo_seed_key=1");
    assertThat(seed.seed()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT annual_salary FROM employees WHERE demo_seed_key=1",
                java.math.BigDecimal.class))
        .isEqualByComparingTo("123.45");
    jdbc.execute("TRUNCATE employees");
    seed.seed();
    assertThat(
            jdbc.queryForObject(
                "SELECT id::text FROM employees WHERE demo_seed_key=1", String.class))
        .isEqualTo(firstId);
  }

  @Test
  void postgresRejectsNegativeSalaryAndMissingCurrencyRate() {
    UUID id = fixture("10.00", "USD", "US", "ENGINEERING", true, "Avery");
    assertThatThrownBy(() -> jdbc.update("UPDATE employees SET annual_salary=-1 WHERE id=?", id))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("UPDATE employees SET currency_code='XYZ' WHERE id=?", id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void nativeDepartmentCostsStaySeparatedAndUnsupportedReferencesAreRejected() throws Exception {
    login();
    fixture("100.00", "USD", "US", "ENGINEERING", true, "Avery");
    fixture("100.00", "INR", "IN", "ENGINEERING", true, "Avery");
    var departments = summary("usd=false").path("departments");
    assertThat(departments.size()).isEqualTo(2);
    assertThat(departments.get(0).path("currency").asText()).isEqualTo("INR");
    assertThat(departments.get(1).path("currency").asText()).isEqualTo("USD");
    assertThat(departments.get(0).path("total").asText()).isEqualTo("100.00");
    assertThat(departments.get(1).path("total").asText()).isEqualTo("100.00");
    assertThat(send("GET", "/api/employees?country=XX", null, false).statusCode()).isEqualTo(400);
    assertThat(
            send("GET", "/api/reports/salary-summary?department=UNKNOWN", null, false).statusCode())
        .isEqualTo(400);
  }

  private UUID fixture(
      String salary,
      String currency,
      String country,
      String department,
      boolean active,
      String name) {
    UUID id = UUID.randomUUID();
    int n = ++sequence;
    jdbc.update(
        "INSERT INTO employees(id, employee_code, first_name, last_name, email, country_code, department_code, annual_salary, currency_code, active) VALUES (?, ?, ?, 'Fixture', ?, ?, ?, ?::numeric, ?, ?)",
        id,
        "FIX-" + n,
        name,
        "fixture." + n + "@example.invalid",
        country,
        department,
        salary,
        currency,
        active);
    return id;
  }

  private Map<String, Object> input(String code, String salary, Long version) {
    var data =
        new java.util.LinkedHashMap<String, Object>(
            Map.of(
                "employeeCode",
                code,
                "firstName",
                "Avery",
                "lastName",
                "Fixture",
                "email",
                code.toLowerCase() + "@example.invalid",
                "countryCode",
                "US",
                "departmentCode",
                "ENGINEERING",
                "annualSalary",
                salary,
                "currencyCode",
                "USD"));
    if (version != null) data.put("version", version);
    return data;
  }

  private void login() throws Exception {
    csrf = tree(send("GET", "/api/auth/csrf", null, false)).path("token").asText();
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("X-CSRF-TOKEN", csrf)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "username=test-hr&password=integration-test-password"))
            .build();
    assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode())
        .isEqualTo(204);
    csrf = tree(send("GET", "/api/auth/csrf", null, false)).path("token").asText();
  }

  private JsonNode summary(String query) throws Exception {
    var response = send("GET", "/api/reports/salary-summary?" + query, null, false);
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return tree(response);
  }

  private HttpRequest request(String method, String path, Object body, boolean token) {
    var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (token) builder.header("X-CSRF-TOKEN", csrf);
    if (body != null) builder.header("Content-Type", "application/json");
    return builder
        .method(
            method,
            body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
        .build();
  }

  private HttpResponse<String> send(String method, String path, Object body, boolean token)
      throws Exception {
    return client.send(request(method, path, body, token), HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode tree(HttpResponse<String> response) {
    return json.readTree(response.body());
  }
}
