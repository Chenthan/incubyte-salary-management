package com.incubyte.salary.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employees")
class Employee {
  @Id UUID id;

  @Column(name = "employee_code", nullable = false, length = 30, updatable = false)
  String employeeCode;

  @Column(name = "first_name", nullable = false, length = 80)
  String firstName;

  @Column(name = "last_name", nullable = false, length = 80)
  String lastName;

  @Column(nullable = false, length = 254)
  String email;

  @Column(name = "country_code", nullable = false, length = 2)
  String countryCode;

  @Column(name = "department_code", nullable = false, length = 30)
  String departmentCode;

  @Column(name = "annual_salary", nullable = false, precision = 12, scale = 2)
  BigDecimal annualSalary;

  @Column(name = "currency_code", nullable = false, length = 3)
  String currencyCode;

  @Column(nullable = false)
  boolean active = true;

  @Version Long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  protected Employee() {}

  Employee(String employeeCode) {
    this.id = UUID.randomUUID();
    this.employeeCode = employeeCode;
  }

  @PrePersist
  void created() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void updated() {
    updatedAt = Instant.now();
  }
}
