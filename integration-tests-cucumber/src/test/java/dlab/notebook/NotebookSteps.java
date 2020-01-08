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

package dlab.notebook;

import com.jayway.restassured.config.HttpClientConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import dlab.util.KeycloakUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dlab.dto.EndpointStatusDTO;
import org.apache.dlab.dto.NotebookCreateDTO;
import org.apache.dlab.dto.NotebookDTO;
import org.apache.dlab.dto.ProjectDTO;
import org.apache.dlab.dto.ProjectNotebookDTO;
import org.apache.dlab.util.JacksonMapper;


import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static dlab.Constants.API_URI;
import static dlab.Constants.CLIENT_ID;
import static dlab.Constants.CONNECTION_TIMEOUT;
import static dlab.Constants.CONNECTION_TIMEOUT_LABEL;
import static dlab.Constants.LOCAL_ENDPOINT;
import static dlab.Constants.SOCKET_TIMEOUT;
import static dlab.Constants.SOCKET_TIMEOUT_LABEL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;


@Slf4j
public class NotebookSteps {
    private static final String SERVICE_ACCOUNT_FORMAT = "service-account-%s";

    private RequestSpecification request;
    private Response response;
    private String name;
    private String projectName;


    @Given("There is active project {string} in DLab")
    public void thereIsActiveProjectInDLab(String project) {
        this.projectName = project;
        Response response = authenticatedRequest().get(API_URI + "project/{project}", project);

        assertThat("Status code doesn't match!", response.getStatusCode(), equalTo(200));

        boolean runningProjectExists = response.as(ProjectDTO.class).getEndpoints().stream()
                .anyMatch(e -> LOCAL_ENDPOINT.equals(e.getName()) && e.getStatus() == EndpointStatusDTO.Status.RUNNING);

        assertTrue(String.format("There is no active project %s.", project), runningProjectExists);
        log.info("There is an active project {}", project);
    }

    @And("There is no notebook with name {string}")
    public void thereIsNoNotebookWithName(String name) throws URISyntaxException {
        this.name = name;
        Optional<NotebookDTO> notebook = getNotebookDTO();

        assertFalse("Notebook exists!", notebook.isPresent());
    }

    @And("User tries to create new notebook with name {string}, endpoint {string}, image {string}, template {string}, project {string}, exploratory tag {string}, shape {string}, version {string}, image name {string}")
    public void userTriesToCreateNewNotebookWithNameEndpointImageTemplateProjectExploratoryTagShapeVersionImageName(String name, String endpoint, String image, String template, String project, String exploratoryTag, String shape, String version, String imageName) {
        this.name = name;
        NotebookCreateDTO notebookCreateDTO = new NotebookCreateDTO(image, template, name, project, exploratoryTag, endpoint, shape, version, imageName, null);
        request = authenticatedRequest()
                .body(JacksonMapper.marshall(notebookCreateDTO))
                .contentType(ContentType.JSON);
    }

    @When("User sends create new notebook request")
    public void userSendsCreateNewNotebookRequest() {
        response = request.put(API_URI + "infrastructure_provision/exploratory_environment");
        log.info("Sending request to create notebook {} in project {}", name, projectName);
    }

    @Then("Status code is {int} for notebook")
    public void statusCodeIs(int code) {
        assertThat("Status codes don't match!", response.getStatusCode(), equalTo(code));
    }

    @And("User waits maximum {int} minutes while notebook is creating")
    public void userWaitsMaximumTimeoutMinutesWhileNotebookIsCreating(int timeout) throws URISyntaxException, InterruptedException {
        boolean isRunning = waitForStatus(timeout, NotebookDTO.Status.RUNNING);

        assertTrue("Timeout for notebook status check reached!", isRunning);
        log.info("Notebook {} successfully started", projectName);
    }

    private boolean waitForStatus(int timeout, NotebookDTO.Status status) throws URISyntaxException, InterruptedException {
        boolean correctStatus = false;
        LocalDateTime withTimeout = LocalDateTime.now().plusMinutes(timeout);
        log.info("User wait till {} for notebook {} to be {}", withTimeout, projectName, status);

        while (!correctStatus && LocalDateTime.now().isBefore(withTimeout)) {
            Optional<NotebookDTO> notebook = getNotebookDTO();

            assertTrue("Notebook does not exist!", notebook.isPresent());
            assertNotSame("Notebook with status FAILED", NotebookDTO.Status.FAILED.toString(), notebook.get().getStatus().toString());

            correctStatus = status == notebook.get().getStatus();
            TimeUnit.MINUTES.sleep(1);
        }

        return correctStatus;
    }

    private Optional<NotebookDTO> getNotebookDTO() throws URISyntaxException {
        Response response = authenticatedRequest().get(new URI(API_URI + "infrastructure/info"));

        assertThat("Status code doesn't match!", response.getStatusCode(), equalTo(200));

        List<NotebookDTO> notebooks = Arrays.stream(response.getBody().as(ProjectNotebookDTO[].class))
                .filter(projectNotebook -> projectName.equals(projectNotebook.getProject()))
                .map(ProjectNotebookDTO::getNotebooks)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        return notebooks.stream()
                .filter(n -> name.equals(n.getName()) && String.format(SERVICE_ACCOUNT_FORMAT, CLIENT_ID).equals(n.getUser()) &&
                        projectName.equals(n.getProject()))
                .findAny();
    }

    private RequestSpecification authenticatedRequest() {
        return given()
                .auth()
                .oauth2(KeycloakUtil.getToken())
                .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CONNECTION_TIMEOUT_LABEL, CONNECTION_TIMEOUT)
                        .setParam(SOCKET_TIMEOUT_LABEL, SOCKET_TIMEOUT)));
    }
}
