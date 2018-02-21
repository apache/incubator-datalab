package com.epam.dlab.auth.modules;

import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.resources.SynchronousLdapAuthenticationService;
import com.epam.dlab.cloud.CloudModule;
import com.google.inject.Injector;
import io.dropwizard.setup.Environment;

public class GcpSecurityServiceModule extends CloudModule {

	GcpSecurityServiceModule() {
	}

	@Override
	protected void configure() {
		bind(UserVerificationService.class).toInstance(SecurityServiceModule.defaultUserVerificationService());
	}

	@Override
	public void init(Environment environment, Injector injector) {
		environment.jersey().register(injector.getInstance(SynchronousLdapAuthenticationService.class));
	}

}
