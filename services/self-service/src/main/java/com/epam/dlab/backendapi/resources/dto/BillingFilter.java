package com.epam.dlab.backendapi.resources.dto;

import com.epam.dlab.dto.UserInstanceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public abstract class BillingFilter {
	@JsonProperty
	protected List<String> user;
	@JsonProperty("dlab_id")
	protected String dlabId;
	@JsonProperty("resource_type")
	protected List<String> resourceType;
	@JsonProperty("date_start")
	protected String dateStart;
	@JsonProperty("date_end")
	protected String dateEnd;
	@JsonProperty("status")
	protected List<UserInstanceStatus> statuses = Collections.emptyList();

	public abstract List<String> getShapes();
}
