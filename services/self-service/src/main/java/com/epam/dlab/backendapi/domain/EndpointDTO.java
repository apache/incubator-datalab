package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointDTO {

	private final String name;
	private final String url;
	private final String account;
}
