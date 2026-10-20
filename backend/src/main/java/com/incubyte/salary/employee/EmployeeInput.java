package com.incubyte.salary.employee;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EmployeeInput(
    @NotBlank
        @Pattern(
            regexp = "[A-Z0-9][A-Z0-9-]{1,29}",
            message = "Use 2–30 uppercase letters, digits, or hyphens.")
        String employeeCode,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 2, max = 2) String countryCode,
    @NotBlank @Size(max = 30) String departmentCode,
    @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2)
        BigDecimal annualSalary,
    @NotBlank @Size(min = 3, max = 3) String currencyCode,
    @PositiveOrZero Long version) {}
