package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Set;

@Data
public class CreateProjectDTO {
	@NotNull
	private final String name;
	@NotNull
	private final Set<String> groups;
	@NotNull
	final Set<String> endpoints;
	@NotNull
	@Pattern(regexp = "^ssh-.*\\n?", message = "format is incorrect. Please use the openSSH format")
	private final String key;
	@NotNull
	private final String tag;
	@JsonProperty("shared_image_enabled")
	private boolean sharedImageEnabled;
}
