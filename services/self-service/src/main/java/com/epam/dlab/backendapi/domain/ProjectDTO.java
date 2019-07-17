package com.epam.dlab.backendapi.domain;

import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
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
	private EdgeInfo edgeInfo;


	public enum Status {
		CREATING,
		ACTIVE,
		FAILED,
		DELETED,
		DELETING,
		DEACTIVATING,
		ACTIVATING,
		NOT_ACTIVE;

		public static Status from(UserInstanceStatus userInstanceStatus) {
			if (userInstanceStatus == UserInstanceStatus.RUNNING) {
				return ACTIVE;
			} else if (userInstanceStatus == UserInstanceStatus.TERMINATED) {
				return DELETED;
			} else if (userInstanceStatus == UserInstanceStatus.TERMINATING) {
				return DELETING;
			} else if (userInstanceStatus == UserInstanceStatus.STOPPING) {
				return DEACTIVATING;
			} else if (userInstanceStatus == UserInstanceStatus.STOPPED) {
				return NOT_ACTIVE;
			} else if (userInstanceStatus == UserInstanceStatus.STARTING) {
				return ACTIVATING;
			}
			return Status.valueOf(userInstanceStatus.name());
		}

		public static UserInstanceStatus from(Status status) {
			if (status == ACTIVE) {
				return UserInstanceStatus.RUNNING;
			} else if (status == ACTIVATING) {
				return UserInstanceStatus.STARTING;
			} else if (status == DEACTIVATING) {
				return UserInstanceStatus.STOPPING;
			} else if (status == NOT_ACTIVE) {
				return UserInstanceStatus.STOPPED;
			} else if (status == DELETING) {
				return UserInstanceStatus.TERMINATING;
			} else if (status == DELETED) {
				return UserInstanceStatus.TERMINATED;
			}
			throw new IllegalArgumentException();
		}
	}
}
