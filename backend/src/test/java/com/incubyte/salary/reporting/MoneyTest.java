package com.incubyte.salary.reporting;

import static org.assertj.core.api.Assertions.*;

import com.incubyte.salary.common.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
  @Test
  void conversionUsesUsdPerNativeUnitAndRetainsPrecision() {
    assertThat(Money.toUsd(new BigDecimal("100.01"), new BigDecimal("0.02")))
        .isEqualByComparingTo("2.0002");
    assertThat(Money.toUsd(new BigDecimal("123.45"), BigDecimal.ONE))
        .isEqualByComparingTo("123.45");
  }

  @Test
  void missingOrInvalidRatesFailInsteadOfUnderstatingTotals() {
    assertThatThrownBy(() -> Money.toUsd(BigDecimal.TEN, null)).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> Money.toUsd(BigDecimal.TEN, BigDecimal.ZERO))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> Money.toUsd(BigDecimal.TEN, new BigDecimal("-1")))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void displayRoundsHalfUpOnlyAtTheBoundaryAndKeepsEmptyStatisticsUnavailable() {
    assertThat(Money.display(new BigDecimal("1.005"))).isEqualTo("1.01");
    assertThat(Money.display(new BigDecimal("1.004"))).isEqualTo("1.00");
    assertThat(Money.display(null)).isNull();
  }
}
