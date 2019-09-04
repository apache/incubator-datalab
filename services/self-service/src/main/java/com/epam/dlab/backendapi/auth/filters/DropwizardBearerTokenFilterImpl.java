package com.epam.dlab.backendapi.auth.filters;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.PreMatching;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class DropwizardBearerTokenFilterImpl extends JaxrsBearerTokenFilterImpl {

	public DropwizardBearerTokenFilterImpl(KeycloakDeployment keycloakDeployment) {
		deploymentContext = new AdapterDeploymentContext(keycloakDeployment);
		nodesRegistrationManagement = new NodesRegistrationManagement();
	}
}