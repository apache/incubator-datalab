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
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class LdapUserDAO {

    // the request from security.yml for user look up by one of the parameters (mail or phone).
    //  configured in the same request configuration under "filter" key: "(&(objectClass=inetOrgPerson)(mail=%mail%))"
    public static final String USER_LOOK_UP = "userLookUp";
    private final LdapConnectionConfig connConfig;
    private final List<Request> requests;
    private final String bindTemplate;
    private final String ldapBindAttribute;
    private final String ldapSearchAttribute;
    private final LdapConnectionPool usersPool;
    private final LdapConnectionPool searchPool;
    private final ScriptHolder script = new ScriptHolder();
    protected final static Logger LOG = LoggerFactory.getLogger(LdapUserDAO.class);

    public LdapUserDAO(	SecurityServiceConfiguration config ) {
        this.connConfig = config.getLdapConnectionConfig();
        this.requests = config.getLdapSearch();
        this.bindTemplate = config.getLdapBindTemplate();
        this.ldapBindAttribute = config.getLdapBindAttribute();
        this.ldapSearchAttribute = "%" + config.getLdapSearchAttribute() + "%";
        PoolableObjectFactory<LdapConnection> userPoolFactory = new ValidatingPoolableLdapConnectionFactory(connConfig);
        this.usersPool = new LdapConnectionPool(userPoolFactory);
        PoolableObjectFactory<LdapConnection> searchPoolFactory = new ValidatingPoolableLdapConnectionFactory(connConfig);
        this.searchPool = new LdapConnectionPool(searchPoolFactory);
    }

    public UserInfo getUserInfo(String username, String password) throws Exception {
        Map<String, Object> userAttributes = null;
        try (ReturnableConnection userRCon = new ReturnableConnection(usersPool)) {
            LdapConnection userCon = userRCon.getConnection();

            userAttributes = searchUsersAttributes(username, userCon);
            String bindAttribute = userAttributes.get(ldapBindAttribute).toString();
            LOG.info("getUserInfo bindAttribute : {}", bindAttribute);
            bindUser(username, password, bindAttribute, userCon);

            UserInfo userInfo = new UserInfo(username, "******");
            LOG.info("getUserInfo context with attributes : {}", userAttributes);
            for(Map.Entry<String, Object> entry: userAttributes.entrySet()) {
                userInfo.addKey(entry.getKey().toLowerCase(), entry.getValue().toString());
                LOG.debug("Adding attribute {} : {}", entry.getKey().toLowerCase(), entry.getValue().toString());
            }

            return userInfo;
        } catch(Exception e){
            LOG.error("LDAP getUserInfo authentication error for username '{}': {}", username ,e.getMessage());
            throw e;
        }
    }

    private void bindUser(String username, String password, String cn, LdapConnection userCon) throws LdapException {
        LOG.info("Biding with template : "  + bindTemplate + " and username/cn: " + cn);
        String bind = String.format(bindTemplate, cn);
        userCon.bind(bind, password);
        userCon.unBind();
        LOG.debug("User '{}' identified.", username);
    }

    private Map<String, Object> searchUsersAttributes(final String username, LdapConnection userCon) throws IOException, LdapException {
        Map<String, Object> contextMap = new HashMap();
        Map<String, Object> userAttributes = new HashMap();
        for(Request request: requests) {
            if (request.getName().equalsIgnoreCase(USER_LOOK_UP)) {
                LOG.info("Request: {}", request.getName());
                SearchRequest sr = request.buildSearchRequest(new HashMap<String, Object>() {
                    private static final long serialVersionUID = 1L;

                    {
                        LOG.info("Putting user param {} : {}", ldapSearchAttribute, username);
                        put(Pattern.quote(ldapSearchAttribute), username);
                    }
                });
                String filter = sr.getFilter().toString();
                contextMap = LdapFilterCache.getInstance().getLdapFilterInfo(filter);
                LOG.info("searchUsersAttributes context before mapping is: {}", contextMap);
                SearchResultToDictionaryMapper mapper = new SearchResultToDictionaryMapper(request.getName(),
                        new HashMap<>());
                LOG.debug("Retrieving new branch {} for {}", request.getName(), filter);
                try (SearchCursor cursor = userCon.search(sr)) {
                    contextMap = mapper.transformSearchResult(cursor);
                    Iterator<Object> iterator = contextMap.values().iterator();
                    if(iterator.hasNext()) {
                        userAttributes = (Map)iterator.next();
                    }
                }
            }
        }
        LOG.info("searchUsersAttributes context is: {}", contextMap);
        return userAttributes;
    }

    public UserInfo enrichUserInfo(final UserInfo userInfo) throws Exception {
        LOG.debug("Enriching user info for user: {}", userInfo);

        String username = userInfo.getName();
        UserInfo ui = userInfo.withToken("******");
        try (ReturnableConnection searchRCon = new ReturnableConnection(searchPool)) {
            LdapConnection searchCon = searchRCon.getConnection();
            Map<String, Object> conextTree = new HashMap<>();
            for (Request req : requests) {
                if (req == null ) {
                    continue;
                } else if (req.getName().equalsIgnoreCase(USER_LOOK_UP)) {
                    Map<String, Object> usersAttributes = searchUsersAttributes(username, searchCon);
                    LOG.info("enrichUserInfo context is: {}", usersAttributes);
                    for (Map.Entry<String, Object> attribute : usersAttributes.entrySet()) {
                        if (null != attribute.getValue()) {
                            ui.addKey(attribute.getKey().toLowerCase(), attribute.getValue().toString());
                            LOG.debug("Adding attribute {} : {}", attribute.getKey().toLowerCase(), attribute.getValue().toString());
                        }
                    }
                }
                LOG.info("Request: {}", req.getName());
                SearchResultProcessor proc = req.getSearchResultProcessor();
                SearchRequest sr = req.buildSearchRequest(new HashMap<String, Object>() {
                    private static final long serialVersionUID = 1L;
                    {
                        LOG.info("Putting user param {} : {} for user enriching", ldapSearchAttribute, username);
                        put(Pattern.quote(ldapSearchAttribute), username);
                    }
                });
                String filter = sr.getFilter().toString();
                Map<String, Object> contextMap = LdapFilterCache.getInstance().getLdapFilterInfo(filter);
                SearchResultToDictionaryMapper mapper = new SearchResultToDictionaryMapper(req.getName(),
                        conextTree);
                if (contextMap == null) {
                    LOG.debug("Retrieving new branch {} for {}", req.getName(), filter);
                    try (SearchCursor cursor = searchCon.search(sr)) {
                        contextMap = mapper.transformSearchResult(cursor);
                    }
                    if (req.isCache()) {
                        LdapFilterCache.getInstance().save(filter, contextMap, req.getExpirationTimeMsec());
                    }
                } else {
                    LOG.debug("Restoring old branch {} for {}: {}", req.getName(), filter, contextMap);
                    mapper.getBranch().putAll(contextMap);
                }
                if (proc != null) {
                    LOG.debug("Executing: {}", proc.getLanguage());
                    ui = script.evalOnce(req.getName(), proc.getLanguage(), proc.getCode()).apply(ui, conextTree);
                }
            }
        } catch (Exception e) {
            LOG.error("LDAP enrichUserInfo authentication error for username '{}': {}",username ,e.getMessage());
            throw e;
        }
        return ui;
    }

}
