/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.auth.dao;

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.core.DlabLdapConnection;
import com.epam.dlab.auth.core.DlabLdapConnectionFactory;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class LdapUserDAOImpl implements LdapUserDAO {
	private static final String LDAP_SEARCH_ATTRIBUTE = "$LDAP_SEARCH_ATTRIBUTE";
	private static final String COMMON_NAME_ATTRIBUTE = "cn";
	private final DlabLdapConnectionFactory connectionFactory;
	private final SecurityServiceConfiguration configuration;

	@Inject
	public LdapUserDAOImpl(DlabLdapConnectionFactory connectionFactory, SecurityServiceConfiguration configuration) {
		this.connectionFactory = connectionFactory;
		this.configuration = configuration;
	}

	@Override
	public UserInfo getUserInfo(String username, String password) {

		try (DlabLdapConnection connection = connectionFactory.newConnection()) {
			return getUserInfo(username, password, connection.getBoundConnection());
		} catch (Exception e) {
			log.error("Can not get user info for user {} due to: {}", username, e.getMessage());
			throw new DlabException("Username or password is invalid");
		}
	}

	@Override
	public Set<String> getUserGroups(UserInfo userInfo) {
		final String groupUserAttribute = userInfo.getKeys().get(configuration.getLdapGroupUserAttribute());
		try (DlabLdapConnection connection = connectionFactory.newConnection()) {
			final LdapConnection ldapConnection = connection.getBoundConnection();
			try (SearchCursor result = ldapConnection.search(getGroupSearchRequest())) {
				return StreamSupport.stream(result.spliterator(), false)
						.filter(r -> r instanceof SearchResultEntry)
						.map(r -> ((SearchResultEntry) r).getEntry())
						.flatMap(e -> groupStream(groupUserAttribute, e)).collect(Collectors.toSet());
			}
		} catch (Exception e) {
			log.error("Can not get user groups for user {} due to: {}", userInfo.getName(), e.getMessage());
			throw new DlabException("Can not get user groups due to: " + e.getMessage());
		}
	}

	private Stream<? extends String> groupStream(String groupUserAttribute, Entry e) {
		final Attribute groupAttribute = e.get(configuration.getLdapGroupAttribute());
		return StreamSupport.stream(groupAttribute.spliterator(), false)
				.anyMatch(v -> v.toString().equals(groupUserAttribute)) ?
				Stream.of(e.get(configuration.getLdapGroupNameAttribute()).get().toString()) :
				Stream.empty();
	}

	private UserInfo getUserInfo(String username, String password, LdapConnection ldapConnection) throws Exception {
		try (SearchCursor result = ldapConnection.search(getUserSearchRequest(username))) {
			return StreamSupport.stream(result.spliterator(), false)
					.filter(r -> r instanceof SearchResultEntry)
					.map(r -> ((SearchResultEntry) r).getEntry())
					.map(e -> toUserInfo(e, username))
					.peek(u -> bind(ldapConnection, u, password))
					.findAny()
					.orElseThrow(() -> new DlabException("User " + username + " not found"));
		}
	}

	private void bind(LdapConnection ldapConnection, UserInfo u, String password) {
		if (configuration.isUseLdapBindTemplate()) {
			final String bindTemplate = configuration.getLdapBindTemplate();
			final String ldapBindAttrName = configuration.getLdapBindAttribute();
			final String bindAttrValue = Optional.ofNullable(u.getKeys().get(ldapBindAttrName))
					.orElseThrow(() -> new DlabException("Bind attribute " + ldapBindAttrName + " is not found"));
			log.info("Biding with template: {} and attribute {} with value: {}", bindTemplate, ldapBindAttrName,
					bindAttrValue);
			try {
				ldapConnection.bind(String.format(bindTemplate, bindAttrValue), password);
				ldapConnection.unBind();
			} catch (LdapException e) {
				log.error("Can not bind user due to: {}", e.getMessage());
				throw new DlabException("Can not bind user due to: " + e.getMessage(), e);
			}
		}
	}

	private UserInfo toUserInfo(Entry e, String username) {
		final Dn dn = e.getDn();
		log.debug("Entry dn: {}", dn);
		final UserInfo userInfo = new UserInfo(username, null);
		e.getAttributes()
				.forEach(a -> userInfo.addKey(a.getId(), a.get().toString()));
		final String cn = userInfo.getKeys().get(COMMON_NAME_ATTRIBUTE);
		final String[] splittedCommonName = cn.split(" ");
		if (splittedCommonName.length == 2) {
			userInfo.setFirstName(splittedCommonName[0]);
			userInfo.setLastName(splittedCommonName[1]);
		}

		return userInfo;
	}

	private SearchRequestImpl getUserSearchRequest(String username) throws LdapException {
		final SearchRequestImpl searchRequest = new SearchRequestImpl();
		final Request searchRequestParams = configuration.getLdapSearchRequest();
		searchRequest.setBase(new Dn(searchRequestParams.getBase()));
		searchRequest.setFilter(searchRequestParams.getFilter().replace(LDAP_SEARCH_ATTRIBUTE, username));
		searchRequest.setScope(SearchScope.valueOf(searchRequestParams.getScope()));
		searchRequest.setTimeLimit(searchRequestParams.getTimeLimit());
		final List<String> attributes = searchRequestParams.getAttributes();
		searchRequest.addAttributes(attributes.toArray(new String[BigDecimal.ZERO.intValue()]));
		return searchRequest;
	}

	private SearchRequestImpl getGroupSearchRequest() throws LdapException {
		final SearchRequestImpl searchRequest = new SearchRequestImpl();
		final Request searchRequestParams = configuration.getLdapGroupSearchRequest();
		searchRequest.setBase(new Dn(searchRequestParams.getBase()));
		searchRequest.setFilter(searchRequestParams.getFilter());
		searchRequest.setScope(SearchScope.valueOf(searchRequestParams.getScope()));
		searchRequest.setTimeLimit(searchRequestParams.getTimeLimit());
		final List<String> attributes = searchRequestParams.getAttributes();
		searchRequest.addAttributes(attributes.toArray(new String[BigDecimal.ZERO.intValue()]));
		return searchRequest;
	}
}
