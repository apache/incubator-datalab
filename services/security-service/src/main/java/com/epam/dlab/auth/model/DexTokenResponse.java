package com.epam.dlab.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DexTokenResponse {
	@JsonProperty("access_token")
	private final String accessToken;
	@JsonProperty("token_type")
	private final String tokenType;
	@JsonProperty("expires_in")
	private final long exprires;
	@JsonProperty("id_token")
	private final String idToken;
}
