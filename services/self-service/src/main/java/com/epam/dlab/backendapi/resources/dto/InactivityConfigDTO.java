package com.epam.dlab.backendapi.resources.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class InactivityConfigDTO {
	@NotNull
	private final boolean inactivityEnabled;
	private final long maxInactivityTimeMinutes;
}
