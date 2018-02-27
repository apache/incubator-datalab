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

package com.epam.dlab.auth.dao;

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.core.LdapFilterCache;
import com.epam.dlab.auth.dao.filter.SearchResultProcessor;
import com.epam.dlab.auth.dao.script.ScriptHolder;
import com.epam.dlab.auth.dao.script.SearchResultToDictionaryMapper;
import com.epam.dlab.exceptions.DlabException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.ldap.client.api.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class LdapUserDAO {

	// the request from security.yml for user look up by one of the parameters (mail or phone).
	//  configured in the same request configuration under "filter" key: "(&(objectClass=inetOrgPerson)(mail=%mail%))"
	public static final String USER_LOOK_UP = "userLookUp";
	public static final String DISTINGUISH_NAME = "dn";
	private final LdapConnectionConfig connConfig;
	private final List<Request> requests;
	private final String bindTemplate;
	private final String ldapBindAttribute;
	private final String ldapSearchAttribute;
	private final LdapConnectionPool usersPool;
	private final LdapConnectionPool searchPool;
	private final ScriptHolder script = new ScriptHolder();
	private final boolean useBindTemplate;
	private boolean useCache;
	private boolean ldapUseConnectionPool;

	public LdapUserDAO(SecurityServiceConfiguration config, boolean useCache) {
		this.connConfig = config.getLdapConnectionConfig();
		this.requests = config.getLdapSearch();
		this.useBindTemplate = config.isUseLdapBindTemplate();
		this.bindTemplate = config.getLdapBindTemplate();
		this.ldapBindAttribute = config.getLdapBindAttribute();
		this.ldapSearchAttribute = "%" + config.getLdapSearchAttribute() + "%";
		PoolableObjectFactory<LdapConnection> userPoolFactory = new ValidatingPoolableLdapConnectionFactory
				(connConfig);
		this.usersPool = new LdapConnectionPool(userPoolFactory);
		PoolableObjectFactory<LdapConnection> searchPoolFactory = new ValidatingPoolableLdapConnectionFactory
				(connConfig);
		this.searchPool = new LdapConnectionPool(searchPoolFactory);
		this.useCache = useCache;
		this.ldapUseConnectionPool = config.isLdapUseConnectionPool();
	}

	public UserInfo getUserInfo(String username, String password) throws Exception {
		Map<String, Object> userAttributes = null;

		ReturnableConnection returnableConnection = null;
		LdapConnection ldapConnection = null;
		try {
			if (ldapUseConnectionPool) {
				returnableConnection = new ReturnableConnection(usersPool);
				ldapConnection = returnableConnection.getConnection();
			} else {
				ldapConnection = new LdapNetworkConnection(connConfig);
				if (!ldapConnection.connect()) {
					log.error("Cannot establish a connection to LDAP server");
					throw new DlabException("Login user failed. LDAP server is not available");
				}
			}
			userAttributes = searchUsersAttributes(username, ldapConnection);
			String bindAttribute = userAttributes.get(ldapBindAttribute).toString();
			bindUser(username, password, bindAttribute, ldapConnection, (String) userAttributes.get(DISTINGUISH_NAME));

			UserInfo userInfo = new UserInfo(username, "******");
			for (Map.Entry<String, Object> entry : userAttributes.entrySet()) {
				userInfo.addKey(entry.getKey().toLowerCase(), entry.getValue().toString());
				log.debug("Adding attribute {} : {}", entry.getKey().toLowerCase(), entry.getValue().toString());
			}

			return userInfo;
		} catch (Exception e) {
			log.error("LDAP getUserInfo authentication error for username '{}': {}", username, e.getMessage(), e);
			throw e;
		} finally {
			closeQuietly(returnableConnection, ldapConnection);
		}
	}

	private void bindUser(String username, String password, String cn, LdapConnection userCon, String dn) throws
			LdapException {
		userCon.bind(getBind(cn, dn), password);
		userCon.unBind();
		log.debug("User '{}' identified.", username);
	}

	private String getBind(String cn, String dn) {
		String bind;
		if (useBindTemplate) {
			log.info("Biding with template : {} and username/cn: {}", bindTemplate, cn);
			bind = String.format(bindTemplate, cn);
		} else {
			log.info("Biding using dn : {}", dn);
			bind = dn;
		}
		return bind;
	}

	private Map<String, Object> searchUsersAttributes(final String username, LdapConnection userCon) throws
			IOException, LdapException {
		Map<String, Object> contextMap;
		Map<String, Object> userAttributes = new HashMap<>();
		for (Request request : requests) {
			if (request.getName().equalsIgnoreCase(USER_LOOK_UP)) {
				log.info("Request: {}", request.getName());
				SearchRequest sr = request.buildSearchRequest(new HashMap<String, Object>() {
					private static final long serialVersionUID = 1L;

					{
						log.info("Putting user param {} : {}", ldapSearchAttribute, username);
						put(Pattern.quote(ldapSearchAttribute), username);
					}
				});
				String filter = sr.getFilter().toString();
				contextMap = (useCache) ? LdapFilterCache.getInstance().getLdapFilterInfo(filter) : null;
				SearchResultToDictionaryMapper mapper = new SearchResultToDictionaryMapper(request.getName(),
						new HashMap<>());
				log.debug("Retrieving new branch {} for {}", request.getName(), filter);
				try (SearchCursor cursor = userCon.search(sr)) {
					contextMap = mapper.transformSearchResult(cursor);
					Iterator<Object> iterator = contextMap.values().iterator();
					if (iterator.hasNext()) {
						@SuppressWarnings("unchecked")
						Map<String, Object> ua = (Map<String, Object>) iterator.next();
						log.info("User atttr {} ", ua);
						userAttributes = ua;
					}
				}
			}
		}
		log.info("User context is: {}", userAttributes);
		return userAttributes;
	}

	public UserInfo enrichUserInfo(final UserInfo userInfo) throws Exception {
		log.debug("Enriching user info for user: {}", userInfo);

		String username = userInfo.getName();
		UserInfo ui = userInfo.withToken("******");
		ReturnableConnection returnableConnection = null;
		LdapConnection ldapConnection = null;
		try {
			if (ldapUseConnectionPool) {
				returnableConnection = new ReturnableConnection(searchPool);
				ldapConnection = returnableConnection.getConnection();
			} else {
				ldapConnection = new LdapNetworkConnection(connConfig);
				if (!ldapConnection.connect()) {
					log.error("Connect to LDAP server is failed");
					throw new DlabException("User enrichment failed. LDAP server is not available.");
				}
			}
			Map<String, Object> conextTree = new HashMap<>();
			for (Request req : requests) {
				if (req == null) {
					continue;
				} else if (req.getName().equalsIgnoreCase(USER_LOOK_UP)) {
					Map<String, Object> usersAttributes = searchUsersAttributes(username, ldapConnection);
					for (Map.Entry<String, Object> attribute : usersAttributes.entrySet()) {
						if (null != attribute.getValue()) {
							ui.addKey(attribute.getKey().toLowerCase(), attribute.getValue().toString());
							log.debug("Adding attribute {} : {}", attribute.getKey().toLowerCase(), attribute.getValue
									().toString());
						}
					}
				}
				log.info("Request: {}", req.getName());
				SearchResultProcessor proc = req.getSearchResultProcessor();
				SearchRequest sr = req.buildSearchRequest(new HashMap<String, Object>() {
					private static final long serialVersionUID = 1L;

					{
						log.info("Putting user param {} : {} for user enriching", ldapSearchAttribute, username);
						put(Pattern.quote(ldapSearchAttribute), username);
					}
				});
				String filter = sr.getFilter().toString();
				Map<String, Object> contextMap = (useCache) ? LdapFilterCache.getInstance().getLdapFilterInfo(filter)
						: null;
				SearchResultToDictionaryMapper mapper = new SearchResultToDictionaryMapper(req.getName(),
						conextTree);
				if (contextMap == null) {
					log.debug("Retrieving new branch {} for {}", req.getName(), filter);
					try (SearchCursor cursor = ldapConnection.search(sr)) {
						contextMap = mapper.transformSearchResult(cursor);
					}
					if (req.isCache() && useCache) {
						LdapFilterCache.getInstance().save(filter, contextMap, req.getExpirationTimeMsec());
					}
				} else {
					log.debug("Restoring old branch {} for {}: {}", req.getName(), filter, contextMap);
					mapper.getBranch().putAll(contextMap);
				}
				if (proc != null) {
					log.debug("Executing: {}", proc.getLanguage());
					conextTree.put("key", ui.getKeys().get("dn"));
					ui = script.evalOnce(req.getName(), proc.getLanguage(), proc.getCode()).apply(ui, conextTree);
				}
			}
		} catch (Exception e) {
			log.error("LDAP enrichUserInfo authentication error for username '{}': {}", username, e.getMessage(), e);
			throw e;
		} finally {
			closeQuietly(returnableConnection, ldapConnection);
		}
		return ui;
	}

	private void closeQuietly(ReturnableConnection returnableConnection, LdapConnection ldapConnection) {
		try {
			if (ldapUseConnectionPool) {
				if (returnableConnection != null) {
					returnableConnection.close();
				}
			} else {
				if (ldapConnection != null) {
					ldapConnection.close();
				}
			}

		} catch (IOException e) {
			log.error("Connection closing failed", e);
		}
	}
}
