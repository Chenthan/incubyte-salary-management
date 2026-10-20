package com.incubyte.salary.employee;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeRepository extends JpaRepository<Employee, UUID> {}
