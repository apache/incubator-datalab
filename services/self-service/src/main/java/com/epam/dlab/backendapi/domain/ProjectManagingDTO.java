package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class ProjectManagingDTO {
    private String name;
    private final Integer budget;
    private boolean canBeStopped;
    private boolean canBeTerminated;
}