package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateProjectBudgetDTO {
	@NotNull
	private final String project;
	@NotNull
	private final Integer budget;
	@JsonProperty("is_monthly_budget")
	private final boolean isMonthlyBudget;
}
