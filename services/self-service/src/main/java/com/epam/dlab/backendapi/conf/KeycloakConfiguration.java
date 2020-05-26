package com.epam.dlab.backendapi.conf;

import lombok.Data;

@Data
public class KeycloakConfiguration extends de.ahus1.keycloak.dropwizard.KeycloakConfiguration {
	private String redirectUri;
}
