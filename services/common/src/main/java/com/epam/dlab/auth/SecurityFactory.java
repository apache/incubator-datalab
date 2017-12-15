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

package com.epam.dlab.auth;

import com.google.inject.Injector;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class SecurityFactory {
    private static final String PREFIX = "Bearer";

    public void configure(Injector injector, Environment environment) {

        configure(injector, environment, SecurityRestAuthenticator.class,
                injector.getInstance(SecurityAuthorizer.class));
    }

    public <T extends SecurityRestAuthenticator> void configure(Injector injector, Environment environment,
                                                                Class<T> authenticator,
                                                                Authorizer<UserInfo> authorizer) {

        environment.jersey().register(new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<UserInfo>()
                        .setAuthenticator(injector.getInstance(authenticator))
                        .setAuthorizer(authorizer)
                        .setPrefix(PREFIX)
                        .setUnauthorizedHandler(injector.getInstance(SecurityUnauthorizedHandler.class))
                        .buildAuthFilter()));

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(UserInfo.class));
    }
}
