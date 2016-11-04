/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.SecurityAPI;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.MongoCollections;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.client.restclient.RESTService;
import com.epam.dlab.dto.UserCredentialDTO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.epam.dlab.auth.SecurityRestAuthenticator.SECURITY_SERVICE;

@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource implements MongoCollections, SecurityAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityResource.class);

    @Inject
    private SecurityDAO dao;
    @Inject
    @Named(SECURITY_SERVICE)
    RESTService securityService;

    @POST
    @Path("/login")
    public Response login(UserCredentialDTO credential) {
        LOGGER.debug("Try login user = {}", credential.getUsername());
        dao.writeLoginAttempt(credential);
        return securityService.post(LOGIN, credential, Response.class);
    }

    @POST
    @Path("/logout")
    public Response logout(@Auth UserInfo userInfo) {
        LOGGER.debug("Try logout accessToken {}", userInfo.getAccessToken());
        return securityService.post(LOGOUT, userInfo.getAccessToken(), Response.class);
    }

    @POST
    @Path("/authorize")
    public Response authorize(@Auth UserInfo userInfo, String username) {
        LOGGER.debug("Try authorize accessToken {}", userInfo.getAccessToken());
        return Response
                .status(userInfo.getName().toLowerCase().equals(username.toLowerCase()) ?
                        Response.Status.OK :
                        Response.Status.FORBIDDEN)
                .build();
    }
}