package com.epam.dlab.config.gcp;

import lombok.Data;

@Data
public class GcpLoginConfiguration {
	private final boolean oauth2authenticationEnabled;
	private final String redirectedUri;
	private final String clientId;
	private final String clientSecret;
	private final String applicationName;
	private final int userStateCacheSize;
	private final long userStateCacheExpirationTime;
}
