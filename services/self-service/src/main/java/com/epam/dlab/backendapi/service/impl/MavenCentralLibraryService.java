/*
 * **************************************************************************
 *
 * Copyright (c) 2018, EPAM SYSTEMS INC
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
 *
 * ***************************************************************************
 */

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.domain.MavenSearchArtifactResponse;
import com.epam.dlab.backendapi.service.ExternalLibraryService;
import com.epam.dlab.backendapi.resources.dto.LibraryDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URI;

@Singleton
@Slf4j
public class MavenCentralLibraryService implements ExternalLibraryService {

	private static final String MAVEN_SEARCH_API = "http://search.maven.org/solrsearch/select";
	private static final String QUOTE_ENCODED = "%22";
	private static final String MAVEN_SEARCH_API_QUERY_FORMAT = "%s?q=%s&rows=20&wt=json&core=gav&p=jar";
	private static final String LIB_NOT_FOUND_MSG = "Artifact with id=%s, groupId=%s and version %s not found";
	private final Client restClient;

	@Inject
	public MavenCentralLibraryService(Client restClient) {
		this.restClient = restClient;
	}

	@Override
	public LibraryDTO getLibrary(String groupId, String artifactId, String version) {
		return getMavenLibrary(groupId, artifactId, version);

	}

	private LibraryDTO getMavenLibrary(String groupId, String artifactId, String version) {
		final String query = and(artifactQuery(artifactId), groupQuery(groupId), versionQuery(version), jarPackage());
		final String mavenApiSearchUri = String.format(MAVEN_SEARCH_API_QUERY_FORMAT, MAVEN_SEARCH_API, query);
		log.trace("Calling maven api using the following uri: {}", mavenApiSearchUri);
		final Response apiResponse = restClient.target(URI.create(mavenApiSearchUri))
				.request()
				.get();
		if (apiResponse.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
			log.error("Can not get artifact info from maven central due to: " + apiResponse.getStatusInfo().getReasonPhrase());
			throw new DlabException("Can not get artifact info from maven central due to: " + apiResponse.getStatusInfo().getReasonPhrase());
		}
		final MavenSearchArtifactResponse response = apiResponse.readEntity(MavenSearchArtifactResponse.class);
		return response.getArtifacts()
				.stream()
				.findFirst()
				.map(artifact -> new LibraryDTO(String.join(":", groupId, artifactId), version))
				.orElseThrow(() -> new ResourceNotFoundException(String.format(LIB_NOT_FOUND_MSG, artifactId, groupId,
						version)));
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

	private String jarPackage() {
		return "p:" + QUOTE_ENCODED + "jar" + QUOTE_ENCODED;
	}

	private String and(String... strings) {
		return String.join("+AND+", strings);
	}

}
