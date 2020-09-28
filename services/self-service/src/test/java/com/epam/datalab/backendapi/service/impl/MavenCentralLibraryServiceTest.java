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
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MavenCentralLibraryServiceTest {

    @Mock
    private RESTService client;
    @InjectMocks
    private MavenCentralLibraryService mavenCentralLibraryService;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void getLibraryId() {
        when(client.get(any(URI.class), any())).thenReturn(getMavenResponse());

        final LibraryDTO libDTO = mavenCentralLibraryService.getLibrary("groupId", "artifactId", "version");

        assertNotNull(libDTO);
        assertEquals("groupId:artifactId", libDTO.getName());
        assertEquals("version", libDTO.getVersion());

        verify(client).get(refEq(URI.create("/solrsearch/select?q=a:%22artifactId%22+AND+g" +
                        ":%22groupId%22+AND+v:%22version%22+AND+(p:%22jar%22%20OR%20p:%22bundle%22)" +
                        "&rows=20&wt=json&core=gav&p=jar")),
                eq(MavenSearchArtifactResponse.class));
    }

    @Test
    public void getLibraryIdWithException() {
        when(client.get(any(URI.class), any())).thenThrow(new DatalabException("Can not get artifact info from maven " +
                "central due to: Exception"));

        expectedException.expect(DatalabException.class);
        expectedException.expectMessage("Can not get artifact info from maven central due to: Exception");

        mavenCentralLibraryService.getLibrary("groupId", "artifactId", "version");
    }

    private MavenSearchArtifactResponse getMavenResponse() {
        final MavenSearchArtifactResponse response = new MavenSearchArtifactResponse();
        MavenSearchArtifactResponse.Response.Artifact artifact = new MavenSearchArtifactResponse.Response.Artifact();
        artifact.setId("test.group:artifact:1.0");
        artifact.setVersion("1.0");
        final MavenSearchArtifactResponse.Response rsp = new MavenSearchArtifactResponse.Response();
        rsp.setArtifacts(Collections.singletonList(artifact));
        response.setResponse(rsp);
        return response;
    }
}