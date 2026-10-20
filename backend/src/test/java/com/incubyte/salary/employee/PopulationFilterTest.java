package com.incubyte.salary.employee;

import static org.assertj.core.api.Assertions.*;

import com.incubyte.salary.common.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PopulationFilterTest {
  @Test
  void searchTreatsWildcardsAsLiteralCharacters() {
    var query = new PopulationFilter("  50%_\\  ", "", "", "ACTIVE", "", null, true).sql();
    assertThat(query.parameters().get("q")).isEqualTo("%50\\%\\_\\\\%");
    assertThat(query.predicate()).doesNotContain("50%");
  }

  @Test
  void nativeThresholdNeedsCurrencyAndValidPrecision() {
    assertThatThrownBy(() -> new PopulationFilter("", "", "", "ACTIVE", "", BigDecimal.TEN, false))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () -> new PopulationFilter("", "", "", "ACTIVE", "USD", new BigDecimal("1.001"), false))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void sortIsAllowlistedAndAlwaysHasUniqueTieBreaker() {
    assertThat(PopulationFilter.ordering("salary", "desc", true)).endsWith("DESC, e.id ASC");
    assertThatThrownBy(() -> PopulationFilter.ordering("salary; DROP TABLE employees", "asc", true))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> PopulationFilter.ordering("salary", "DESC NULLS FIRST", true))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void unsupportedStatusAndMalformedReferenceCodesAreRejected() {
    assertThatThrownBy(() -> new PopulationFilter("", "US'", "", "ALL", "", null, true))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> new PopulationFilter("", "", "", "DELETED", "", null, true))
        .isInstanceOf(ApiException.class);
  }
}
