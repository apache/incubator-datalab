package com.epam.dlab.backendapi.auth;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.google.inject.Singleton;
import io.dropwizard.auth.Authorizer;

@Singleton
public class SelfServiceSecurityAuthorizer implements Authorizer<UserInfo> {
	@Override
	public boolean authorize(UserInfo principal, String role) {
		return UserRoles.checkAccess(principal, RoleType.PAGE, role);
	}
}
