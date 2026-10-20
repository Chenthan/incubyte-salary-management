package com.incubyte.salary.reporting;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReferenceDataController {
  private final ReferenceDataService referenceData;

  ReferenceDataController(ReferenceDataService referenceData) {
    this.referenceData = referenceData;
  }

  @GetMapping("/api/reference-data")
  ReferenceDataService.ReferenceData referenceData() {
    return referenceData.referenceData();
  }
}
