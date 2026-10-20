package com.incubyte.salary.reporting;

import com.incubyte.salary.common.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;

public final class Money {
  private Money() {}

  public static BigDecimal toUsd(BigDecimal nativeAmount, BigDecimal usdPerUnit) {
    if (usdPerUnit == null || usdPerUnit.signum() <= 0) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE, "MISSING_RATE", "A valid reporting rate is unavailable.");
    }
    return nativeAmount.multiply(usdPerUnit);
  }

  public static String display(BigDecimal value) {
    return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
