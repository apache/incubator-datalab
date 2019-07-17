package com.epam.dlab.backendapi.domain;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateProjectBudgetDTO {
	@NotNull
	private final String project;
	@NotNull
	private final Integer budget;
}
