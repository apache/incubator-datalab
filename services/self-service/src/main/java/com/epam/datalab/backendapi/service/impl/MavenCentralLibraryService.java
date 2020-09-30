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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.backendapi.domain.MavenSearchArtifactResponse;
import com.epam.datalab.backendapi.resources.dto.LibraryDTO;
import com.epam.datalab.backendapi.service.ExternalLibraryService;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.net.URI;

import static java.lang.String.join;

@Singleton
@Slf4j
public class MavenCentralLibraryService implements ExternalLibraryService {

    private static final String QUOTE_ENCODED = "%22";
    private static final String SEARCH_API_QUERY_FORMAT = "/solrsearch/select?q=%s&rows=20&wt=json&core=gav&p=jar";
    private static final String LIB_NOT_FOUND_MSG = "No matches found";
    private final RESTService restClient;

    @Inject
    public MavenCentralLibraryService(@Named(ServiceConsts.MAVEN_SEARCH_API) RESTService restClient) {
        this.restClient = restClient;
    }

    @Override
    public LibraryDTO getLibrary(String groupId, String artifactId, String version) {
        return getMavenLibrary(groupId, artifactId, version);

    }

    private LibraryDTO getMavenLibrary(String groupId, String artifactId, String version) {
        final String query = and(artifactQuery(artifactId), groupQuery(groupId), versionQuery(version), jarOrBundlePackage());
        return restClient.get(URI.create(String.format(SEARCH_API_QUERY_FORMAT, query)),
                MavenSearchArtifactResponse.class)
                .getArtifacts()
                .stream()
                .findFirst()
                .map(artifact -> new LibraryDTO(join(":", groupId, artifactId), version))
                .orElseThrow(() -> new ResourceNotFoundException(LIB_NOT_FOUND_MSG));
    }

    private String groupQuery(String groupId) {
        return "g:" + QUOTE_ENCODED + groupId + QUOTE_ENCODED;
    }

    private String artifactQuery(String artifactId) {
        return "a:" + QUOTE_ENCODED + artifactId + QUOTE_ENCODED;
    }

    private String versionQuery(String version) {
        return "v:" + QUOTE_ENCODED + version + QUOTE_ENCODED;
    }

    private String jarOrBundlePackage() {
        return "(p:" + QUOTE_ENCODED + "jar" + QUOTE_ENCODED + "%20OR%20p:" + QUOTE_ENCODED + "bundle" + QUOTE_ENCODED + ")";
    }

    private String and(String... strings) {
        return join("+AND+", strings);
    }

}
