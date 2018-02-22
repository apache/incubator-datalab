package com.epam.dlab.auth.modules;

import com.epam.dlab.ModuleBase;
import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.dao.LdapUserDAO;
import com.epam.dlab.auth.dao.UserInfoDAODumbImpl;
import com.epam.dlab.auth.dao.UserInfoDAOMongoImpl;
import com.epam.dlab.mongo.MongoService;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityServiceModule extends ModuleBase<SecurityServiceConfiguration> {

	public SecurityServiceModule(SecurityServiceConfiguration configuration, Environment environment) {
		super(configuration, environment);
	}

	@Override
	protected void configure() {
		bind(SecurityServiceConfiguration.class).toInstance(configuration);
		if (configuration.isUserInfoPersistenceEnabled()) {
			bind(UserInfoDAO.class).to(UserInfoDAOMongoImpl.class);
		} else {
			bind(UserInfoDAO.class).to(UserInfoDAODumbImpl.class);
		}
	}

	@Provides
	@Singleton
	private MongoService mongoService() {
		return configuration.getMongoFactory().build(environment);
	}

	@Provides
	@Singleton
	private LdapUserDAO ldapUserDAOWithoutCache() {
		return new LdapUserDAO(configuration, false);
	}

	public static UserVerificationService defaultUserVerificationService() {
		return (username, userInfo) -> log.debug("No additional user verification configured");
	}
}
