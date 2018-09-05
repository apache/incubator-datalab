/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.auth.azure;

import com.epam.dlab.auth.conf.AzureLoginConfiguration;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DataBindingException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Builds login url for authentication through Azure Active Directory using OAuth2 protocol
 */
@Slf4j
@Singleton
public class AzureLoginUrlBuilder {
    private AzureLoginConfiguration azureLoginConfiguration;

    public AzureLoginUrlBuilder(AzureLoginConfiguration azureLoginConfiguration) {
        this.azureLoginConfiguration = azureLoginConfiguration;
    }

    String buildLoginUrl() {
        return azureLoginConfiguration.getLoginPage();
    }

    String buildLoginUrl(String state) {
        return buildLoginUrl(state, azureLoginConfiguration.getPrompt());
    }

    String buildSilentLoginUrl(String state) {
        log.info("Silent login is {}", azureLoginConfiguration.isSilent());

        if (azureLoginConfiguration.isSilent()) {
            return buildLoginUrl(state, "none");
        } else {
            return buildLoginUrl(state);
        }
    }

    private String buildLoginUrl(String state, String prompt) {
        try {
            return String.format("%s/%s/oauth2/authorize?client_id=%s&redirect_uri=%s&response_type=code&response_mode=%s&prompt=%s&state=%s",
                    azureLoginConfiguration.getAuthority(),
                    azureLoginConfiguration.getTenant(),
                    azureLoginConfiguration.getClientId(),
                    URLEncoder.encode(azureLoginConfiguration.getRedirectUrl(), "UTF-8"),
                    azureLoginConfiguration.getResponseMode(),
                    prompt,
                    state);
        } catch (UnsupportedEncodingException e) {
            log.error("Cannot create login url", e);
            throw new DataBindingException("Cannot handle authorization info", e);
        }
    }
}
