package com.epam.dlab.backendapi.dropwizard.bundles;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.auth.KeycloakAuthenticator;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthorizer;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.google.inject.Inject;
import de.ahus1.keycloak.dropwizard.KeycloakBundle;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;

import java.security.Principal;

public class DlabKeycloakBundle extends KeycloakBundle<SelfServiceApplicationConfiguration> {

	@Inject
	private KeycloakAuthenticator authenticator;

	@Override
	protected KeycloakConfiguration getKeycloakConfiguration(SelfServiceApplicationConfiguration configuration) {
		return configuration.getKeycloakConfiguration();
	}

	@Override
	protected Class<? extends Principal> getUserClass() {
		return UserInfo.class;
	}

	@Override
	protected Authorizer createAuthorizer() {
		return new SelfServiceSecurityAuthorizer();
	}

	@Override
	protected Authenticator createAuthenticator(KeycloakConfiguration configuration) {
		return new KeycloakAuthenticator(configuration);
	}
}
