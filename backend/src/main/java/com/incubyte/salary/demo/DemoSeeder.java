package com.incubyte.salary.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DemoSeeder implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(DemoSeeder.class);
  private final JdbcTemplate jdbc;
  private final boolean enabled;

  public DemoSeeder(JdbcTemplate jdbc, @Value("${app.demo.enabled}") boolean enabled) {
    this.jdbc = jdbc;
    this.enabled = enabled;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (enabled) {
      int added = seed();
      LOG.info(
          "Demo seed: {} records added; {} seeded records present",
          added,
          jdbc.queryForObject(
              "SELECT count(*) FROM employees WHERE demo_seed_key BETWEEN 1 AND 10000",
              Long.class));
    }
  }

  public int seed() {
    return jdbc.update(
        """
        INSERT INTO employees (id, employee_code, first_name, last_name, email, country_code,
          department_code, annual_salary, currency_code, active, version, created_at, updated_at, demo_seed_key)
        SELECT md5('salary-seed-v1:' || g)::uuid, 'EMP-' || lpad(g::text, 5, '0'),
          (ARRAY['Avery','Rowan','Morgan','Alex','Taylor','Quinn','Sage','Casey'])[(g - 1) % 8 + 1],
          (ARRAY['Cedar','Harbor','Meadow','Pine','River','Stone','Vale','Willow'])[((g - 1) / 8) % 8 + 1],
          'employee.' || lpad(g::text, 5, '0') || '@example.invalid',
          (ARRAY['US','IN','DE','GB','CA','SG'])[(g - 1) % 6 + 1],
          (ARRAY['ENGINEERING','PRODUCT','DESIGN','PEOPLE','FINANCE','OPERATIONS'])[((g - 1) / 6) % 6 + 1],
          (CASE (g - 1) % 6 WHEN 1 THEN 500000 + (g * 7919) % 3000000
            WHEN 3 THEN 30000 + (g * 7919) % 140000 ELSE 40000 + (g * 7919) % 180000 END)
            + ((g * 37) % 100)::numeric / 100,
          (ARRAY['USD','INR','EUR','GBP','CAD','SGD'])[(g - 1) % 6 + 1],
          g % 20 <> 0, 0, TIMESTAMPTZ '2026-01-01 00:00:00+00' + g * INTERVAL '1 second',
          TIMESTAMPTZ '2026-01-01 00:00:00+00' + g * INTERVAL '1 second', g
        FROM generate_series(1, 10000) AS g
        ON CONFLICT (demo_seed_key) DO NOTHING
        """);
  }
}
