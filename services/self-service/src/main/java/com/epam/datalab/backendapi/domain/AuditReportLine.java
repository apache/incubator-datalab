package com.epam.datalab.backendapi.domain;

import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.billing.BillingResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class AuditReportLine {
    private String datalabId;
    private String action;
    private String resourceType;
    private String resourceName;
    private String project;
    private String user;
    private LocalDate timestamp;
}
