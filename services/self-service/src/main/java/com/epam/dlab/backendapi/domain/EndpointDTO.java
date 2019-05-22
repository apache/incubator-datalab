package com.epam.dlab.backendapi.domain;

import lombok.Data;

@Data
public class EndpointDTO {

	private final String name;
	private final String url;
	private final String account;
}
