package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointDTO {

	private final String name;
	private final String url;
	private final String account;
	@JsonProperty("endpoint_tag")
	private final String tag;
}
