/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.auth.resources;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.User;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.core.LoginCache;
import com.epam.dlab.auth.core.LoginConveyor;
import com.epam.dlab.auth.core.LoginStep;
import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.dao.AwsUserDAOImpl;
import com.epam.dlab.auth.dao.LdapUserDAO;
import com.epam.dlab.auth.dao.UserInfoDAODumbImpl;
import com.epam.dlab.auth.dao.UserInfoDAOMongoImpl;
import com.epam.dlab.auth.dao.filter.AwsUserDAO;
import com.epam.dlab.auth.rest.AbstractAuthenticationService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserCredentialDTO;
import io.dropwizard.setup.Environment;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Provides authentication against LDAP server
 * @deprecated  as it causes undefined behaviours during login. Use @{@link SynchronousLdapAuthenticationService}
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LdapAuthenticationService extends AbstractAuthenticationService<SecurityServiceConfiguration> {

	private final LdapUserDAO ldapUserDAO;
	private final AwsUserDAO awsUserDAO;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final ExecutorService threadpool = Executors.newFixedThreadPool(32);
	private UserInfoDAO userInfoDao;

	private final LoginConveyor loginConveyor;

	public LdapAuthenticationService(SecurityServiceConfiguration config, Environment env) {
		super(config);
		if(config.isUserInfoPersistenceEnabled()) {
			this.userInfoDao = new UserInfoDAOMongoImpl(config.getMongoFactory().build(env),config.getInactiveUserTimeoutMillSec());
		} else {
			this.userInfoDao = new UserInfoDAODumbImpl();
		}
		loginConveyor = new LoginConveyor(config.getLoginAuthenticationTimeout());
		loginConveyor.setUserInfoDao(userInfoDao);
		LoginCache.getInstance().setDefaultBuilderTimeout(config.getInactiveUserTimeoutMillSec(),TimeUnit.MILLISECONDS);
		LoginCache.getInstance().setExpirationPostponeTime(config.getInactiveUserTimeoutMillSec(),TimeUnit.MILLISECONDS);
		if(config.isAwsUserIdentificationEnabled()) {
			DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain();
			awsUserDAO = new AwsUserDAOImpl(providerChain.getCredentials());
			scheduler.scheduleAtFixedRate(()->{
				try {
					providerChain.refresh();
					awsUserDAO.updateCredentials(providerChain.getCredentials());
					log.debug("provider credentials refreshed");
				} catch (Exception e) {
					log.error("AWS provider error",e);
					throw e;
				}
			},5,5, TimeUnit.MINUTES);
		} else {
			awsUserDAO = null;
		}
		this.ldapUserDAO = new LdapUserDAO(config, true);
	}

	@Override
	@POST
	@Path("/login")
	public Response login(UserCredentialDTO credential, @Context HttpServletRequest request) {
		String username    = credential.getUsername();
		String password    = credential.getPassword();
		String accessToken = credential.getAccessToken();
		String remoteIp    = request.getRemoteAddr();

		log.debug("validating username:{} password:****** token:{} ip:{}", username, accessToken,remoteIp);
		String token = getRandomToken();
		UserInfo ui = getUserInfo(accessToken, remoteIp);
		if (ui != null) {
			return Response.ok(accessToken).build();
		}

		CompletableFuture<UserInfo> uiFuture = loginConveyor.startUserInfoBuild(token,username);
		loginConveyor.add(token,remoteIp, LoginStep.REMOTE_IP);

		submitLdapLogin(username,password,token);
		submitLdapInfo(username,token);
		submitAwsCheck(username,token);
		submitAwsKeys(username,token);

		try {
			UserInfo userInfo = uiFuture.get(config.getLoginAuthenticationTimeout(), TimeUnit.SECONDS);
			log.debug("user info collected by conveyor '{}' ", userInfo);
		} catch (Exception e) {
			log.error("Authentication error", e);
			Throwable cause = e.getCause();
			if(cause == null) {
				cause = e;
			}
			log.error("Conveyor error {}", cause.getMessage());
			return Response.status(Response.Status.UNAUTHORIZED).entity(cause.getMessage()).build();
		}
		return Response.ok(token).build();
	}

	private void submitLdapLogin(String username, String password, String token) {
		//Try to login
		threadpool.submit(()->{
			try {
				ldapUserDAO.getUserInfo(username,password);
				log.debug("User Authenticated: {}",username);
				loginConveyor.add(token,"USER LOGGED IN",LoginStep.LDAP_LOGIN);
			} catch (Exception e) {
				log.error("Authentication error", e);
				loginConveyor.cancel(token,LoginStep.LDAP_USER_INFO_ERROR,"Username or password are not valid");
			}
		});
	}

	private void submitLdapInfo(String username, String token) {
		//Extract User Info from LDAP
		threadpool.submit(()->{
			try {
				UserInfo rolesUserInfo = ldapUserDAO.enrichUserInfo(new UserInfo(username, token));
				loginConveyor.add(token,rolesUserInfo,LoginStep.LDAP_USER_INFO);
			} catch (Exception e) {
				log.error("Authentication error", e);
				loginConveyor.cancel(token,LoginStep.LDAP_GROUP_INFO_ERROR,"User not authorized. Please access DLAB administrator.");
			}
		});
	}

	private void submitAwsCheck(String username, String token) {
		//Check AWS account
		threadpool.submit(()->{
			if(config.isAwsUserIdentificationEnabled()) {
				try {
					User awsUser = awsUserDAO.getAwsUser(username);
					if (awsUser != null) {
						loginConveyor.add(token, true, LoginStep.AWS_USER);
					} else {
						loginConveyor.cancel(token,LoginStep.AWS_USER_ERROR,"Please contact AWS administrator to create corresponding IAM User");
					}
				} catch (Exception e) {
					loginConveyor.cancel(token,LoginStep.AWS_USER_ERROR,"Please contact AWS administrator to create corresponding IAM User");
				}
			} else {
				loginConveyor.add(token,false, LoginStep.AWS_USER);
			}
		});

	}

	private void submitAwsKeys(String username, String token) {
		//Check AWS keys
		threadpool.submit(()->{
			if(config.isAwsUserIdentificationEnabled()) {
				try {
					List<AccessKeyMetadata> keys = awsUserDAO.getAwsAccessKeys(username);
					loginConveyor.add(token, keys, LoginStep.AWS_KEYS);
				} catch (Exception e) {
					loginConveyor.cancel(token,LoginStep.AWS_KEYS_ERROR,"Please contact AWS administrator to activate your Access Key");
				}
			} else {
				loginConveyor.add(token,new ArrayList<AccessKeyMetadata>(),LoginStep.AWS_KEYS_EMPTY);
			}
		});

	}

	@Override
	@POST
	@Path("/getuserinfo")
	public UserInfo getUserInfo(String accessToken, @Context HttpServletRequest request) {
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		String remoteIp = request.getRemoteAddr();
		UserInfo ui     = getUserInfo(accessToken, remoteIp);
		if(ui == null) {
			ui = userInfoDao.getUserInfoByAccessToken(accessToken);
			if( ui != null ) {
				ui = ui.withToken(accessToken);
				LoginCache.getInstance().save(ui);
				updateTTL(accessToken, ui, userAgent);
				log.debug("restored UserInfo from DB {}",ui);
			}
		} else {
			updateTTL(accessToken, ui, userAgent);
		}
		log.debug("Authorized {} {} {}", accessToken, ui, remoteIp);
		return ui;
	}

	@Override
	@POST
	@Path("/logout")
	public Response logout(String accessToken) {
		removeUserInfo(accessToken);
		log.debug("Logged out {}", accessToken);
		return Response.ok().build();
	}
	
	private UserInfo getUserInfo(String accessToken, String remoteIp) {
		UserInfo ui = LoginCache.getInstance().getUserInfo(accessToken);
		if (ui != null &&
			(remoteIp == null ||
			 !remoteIp.equals(ui.getRemoteIp()))) {
			log.debug("Remove user info for token {}. IP address is changed from {} to {}", accessToken, ui.getRemoteIp(), remoteIp);
			removeUserInfo(accessToken);
			return null;
		}
		return ui;
	}
	
	private void removeUserInfo(String accessToken) {
		UserInfo ui = LoginCache.getInstance().getUserInfo(accessToken);
		if (ui != null) {
			LoginCache.getInstance().removeUserInfo(accessToken);
			userInfoDao.deleteUserInfo(accessToken);
		}
	}

	private void updateTTL(String accessToken, UserInfo ui, String userAgent) {
		log.debug("updating TTL agent {} {}", userAgent, ui);
		if (ServiceConsts.PROVISIONING_USER_AGENT.equals(userAgent)) {
			return;
		}

		userInfoDao.updateUserInfoTTL(accessToken, ui);
	}
}
