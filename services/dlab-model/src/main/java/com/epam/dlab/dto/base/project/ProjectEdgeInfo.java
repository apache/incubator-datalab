package com.epam.dlab.dto.base.project;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectEdgeInfo {
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String id;

	@JsonProperty("instance_id")
	private String instanceId;

	@JsonProperty
	private String hostname;

	@JsonProperty("public_ip")
	private String publicIp;

	@JsonProperty
	private String ip;

	@JsonProperty("key_name")
	private String keyName;
}
