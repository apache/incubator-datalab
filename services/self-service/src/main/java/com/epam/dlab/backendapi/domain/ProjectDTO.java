package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectDTO {
	@NotNull
	private final String name;
	@NotNull
	private final Set<String> endpoints;
	@NotNull
	private final Set<String> groups;
	@NotNull
	private final String key;
	@NotNull
	private final String tag;
	private final Integer budget;
	private final Status status = Status.CREATING;


	public enum Status {
		CREATING,
		CREATED,
		FAILED
	}
}
