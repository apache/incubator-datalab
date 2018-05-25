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
import com.epam.dlab.auth.core.DlabLdapConnection;
import com.epam.dlab.auth.core.LdapFilterCache;
import com.epam.dlab.auth.core.ReturnableConnection;
import com.epam.dlab.auth.core.SimpleConnection;
import com.epam.dlab.auth.dao.filter.SearchResultProcessor;
import com.epam.dlab.auth.dao.script.ScriptHolder;
import com.epam.dlab.auth.dao.script.SearchResultToDictionaryMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.ldap.client.api.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
public class LdapUserDAO {

	// the request from security.yml for user look up by one of the parameters (mail or phone).
	//  configured in the same request configuration under "filter" key: "(&(objectClass=inetOrgPerson)(mail=%mail%))"
	private static final String USER_LOOK_UP = "userLookUp";
	private static final String DISTINGUISH_NAME = "dn";
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

	private DlabLdapConnection getConnection(LdapConnectionPool connectionPool) {
		return ldapUseConnectionPool ? new ReturnableConnection(connectionPool) :
				new SimpleConnection(new LdapNetworkConnection(connConfig));
	}

	public UserInfo getUserInfo(String username, String password) throws Exception {
		Map<String, Object> userAttributes;

		try (DlabLdapConnection connection = getConnection(usersPool)) {
			final LdapConnection ldapConnection = connection.connect();
			userAttributes = searchUsersAttributes(username, ldapConnection);
			String bindAttribute = userAttributes.get(ldapBindAttribute).toString();
			bindUser(username, password, bindAttribute, ldapConnection, (String) userAttributes.get(DISTINGUISH_NAME));

			UserInfo userInfo = new UserInfo(username, "******");
			userAttributes.entrySet().forEach(entry -> addAttribute(userInfo, entry));

			return userInfo;
		} catch (Exception e) {
			log.error("LDAP getUserInfo authentication error for username '{}': {}", username, e.getMessage(), e);
			throw e;
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
				log.info("Putting user param {} : {}", ldapSearchAttribute, username);
				SearchRequest sr = request.buildSearchRequest(Collections.
						singletonMap(Pattern.quote(ldapSearchAttribute), username));
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
		try (DlabLdapConnection connection = getConnection(searchPool)) {
			final LdapConnection ldapConnection = connection.connect();
			Map<String, Object> conextTree = new HashMap<>();
			for (Request req : requests) {
				if (req.getName().equalsIgnoreCase(USER_LOOK_UP)) {
					addUserAttributes(username, ui, ldapConnection);
				}
				log.info("Request: {}", req.getName());
				SearchResultProcessor proc = req.getSearchResultProcessor();
				log.info("Putting user param {} : {} for user enriching", ldapSearchAttribute, username);
				SearchRequest sr = req.buildSearchRequest(Collections
						.singletonMap(Pattern.quote(ldapSearchAttribute), username));
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
		}
		return ui;
	}

	private void addUserAttributes(String username, UserInfo ui, LdapConnection ldapConnection) throws IOException,
			LdapException {
		Map<String, Object> usersAttributes = searchUsersAttributes(username, ldapConnection);
		usersAttributes.entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
				.forEach(attribute -> addAttribute(ui, attribute));
	}

	private void addAttribute(UserInfo ui, Map.Entry<String, Object> attribute) {
		ui.addKey(attribute.getKey().toLowerCase(), attribute.getValue().toString());
		log.debug("Adding attribute {} : {}", attribute.getKey().toLowerCase(), attribute.getValue
				().toString());
	}
}
