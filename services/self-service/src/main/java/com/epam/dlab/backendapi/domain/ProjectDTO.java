package com.epam.dlab.backendapi.domain;

import com.epam.dlab.dto.UserInstanceStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
	@Pattern(regexp = "^ssh-.*", message = "Wrong key format. Key should be in openSSH format")
	private final String key;
	@NotNull
	private final String tag;
	private final Integer budget;
	private final Status status = Status.CREATING;


	public enum Status {
		CREATING,
		ACTIVE,
		FAILED, TERMINATED, TERMINATING;

		public static Status from(UserInstanceStatus userInstanceStatus) {
			if (userInstanceStatus == UserInstanceStatus.RUNNING) {
				return ACTIVE;
			}
			return Status.valueOf(userInstanceStatus.name());
		}
	}
}
