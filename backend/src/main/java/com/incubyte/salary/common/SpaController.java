package com.incubyte.salary.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
  @GetMapping({"/", "/login", "/overview", "/employees", "/employees/new", "/employees/{id}"})
  String app() {
    return "forward:/index.html";
  }
}
