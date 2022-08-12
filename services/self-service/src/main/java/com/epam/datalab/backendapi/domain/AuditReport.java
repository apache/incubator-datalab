package com.epam.datalab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditReport {
    private String name;
    @JsonProperty("report_lines")
    private List<AuditReportLine> reportLines;
    @JsonProperty("from")
    private LocalDate usageDateFrom;
    @JsonProperty("to")
    private LocalDate usageDateTo;
}
