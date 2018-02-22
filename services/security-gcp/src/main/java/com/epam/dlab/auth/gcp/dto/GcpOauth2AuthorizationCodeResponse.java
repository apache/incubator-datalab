package com.epam.dlab.auth.gcp.dto;

import lombok.Data;

@Data
public class GcpOauth2AuthorizationCodeResponse {
	private final String code;
	private final String state;
	private final String errorMessage;
}
