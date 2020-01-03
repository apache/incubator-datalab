package dlab.project;

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
import org.apache.dlab.dto.CreateProjectDTO;
import org.apache.dlab.dto.EndpointStatusDTO;
import org.apache.dlab.dto.ProjectActionDTO;
import org.apache.dlab.dto.ProjectDTO;
import org.apache.dlab.dto.ProjectKeyDTO;
import org.apache.dlab.dto.ProjectStatusDTO;
import org.apache.dlab.dto.UpdateProjectDTO;
import org.apache.dlab.util.JacksonMapper;
import org.apache.http.HttpStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static dlab.Constants.API_URI;
import static dlab.Constants.CONNECTION_TIMEOUT;
import static dlab.Constants.CONNECTION_TIMEOUT_LABEL;
import static dlab.Constants.LOCAL_ENDPOINT;
import static dlab.Constants.SOCKET_TIMEOUT;
import static dlab.Constants.SOCKET_TIMEOUT_LABEL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ProjectSteps {

	private RequestSpecification request;
	private Response response;
	private String projectName;
	private String publicKey;
	private Set<String> endpoints;
	private Set<String> groups;
	private Set<String> projects;
	private boolean sharedImageEnabled;
	private ProjectDTO project;


	@Given("There is no project with name {string} in DLab")
	public void thereIsNoProjectWithNameInDLab(String projectName) throws URISyntaxException {
		authenticatedRequest()
				.get(new URI(API_URI + "project/" + projectName))
				.then()
				.statusCode(HttpStatus.SC_NOT_FOUND);
	}

	@Given("There are the following projects")
	public void thereAreTheFollowingProjects(List<String> projects) {
		this.projects = new HashSet<>(projects);
	}

	@Given("There is a project with name {string} in DLab")
	public void thereIsAProjectWithNameInDLab(String projectName) {
		this.projectName = projectName;
		this.project = authenticatedRequest()
				.get(API_URI + "project/{project}", projectName)
				.as(ProjectDTO.class);
	}

	@And("There are the following endpoints")
	public void thereAreTheFollowingEndpoints(List<String> endpoints) {
		this.endpoints = new HashSet<>(endpoints);
	}

	@And("There are the following groups")
	public void thereAreTheFollowingGroups(List<String> groups) {
		this.groups = new HashSet<>(groups);
	}

	@And("User generates new publicKey")
	public void userGeneratesNewPublicKey() {
		this.publicKey = authenticatedRequest()
				.contentType(ContentType.JSON)
				.post(API_URI + "project/keys")
				.getBody().as(ProjectKeyDTO.class)
				.getPublicKey();
	}

	@And("User tries to create new project with name {string}, endpoints, groups, publicKey and use shared image enable {string}")
	public void userTriesToCreateNewProjectWithNameEndpointsGroupsAndKey(String projectName, String sharedImageEnabled) {
		this.projectName = projectName;
		boolean sharedImage = Boolean.parseBoolean(sharedImageEnabled);
		request = authenticatedRequest()
				.body(JacksonMapper.marshall(new CreateProjectDTO(projectName, groups, endpoints, publicKey, projectName, sharedImage)))
				.contentType(ContentType.JSON);
	}

	@And("User tries to get information about project with name {string}")
	public void userTriesToGetInformationAboutProjectWithName(String projectName) {
		this.projectName = projectName;
		request = authenticatedRequest();
	}

	@And("Use shared image enable {string}")
	public void useSharedImageEnable(String sharedImageEnabled) {
		this.sharedImageEnabled = Boolean.parseBoolean(sharedImageEnabled);
	}

	@And("User tries to edit project with shared image enable opposite to existing")
	public void userTriesToEditProjectWithSharedImageEnableOppositeToExisting() {
		sharedImageEnabled = !project.isSharedImageEnabled();
		UpdateProjectDTO updateProjectDTO = new UpdateProjectDTO(project.getName(), project.getGroups(),
				project.getEndpoints().stream().map(EndpointStatusDTO::getName).collect(Collectors.toSet()), sharedImageEnabled);

		request = authenticatedRequest()
				.body(JacksonMapper.marshall(updateProjectDTO))
				.contentType(ContentType.JSON);
	}

	@And("User tries to terminate the project with name {string}")
	public void userTriesToTerminateTheProjectWithName(String projectName) {
		this.projectName = projectName;
		request = authenticatedRequest().contentType(ContentType.JSON);
	}

	@And("User tries to get information about projects")
	public void userTriesToGetInformationAboutProjects() {
		request = authenticatedRequest().contentType(ContentType.JSON);
	}

	@And("User tries to stop the project")
	public void userTriesToStopTheProject() {
		request = authenticatedRequest()
				.body(JacksonMapper.marshall(new ProjectActionDTO(projectName, Collections.singletonList(LOCAL_ENDPOINT))))
				.contentType(ContentType.JSON);
	}

	@And("User tries to start the project")
	public void userTriesToStartTheProject() {
		request = authenticatedRequest()
				.body(JacksonMapper.marshall(new ProjectActionDTO(projectName, Collections.singletonList(LOCAL_ENDPOINT))))
				.contentType(ContentType.JSON);
	}

	@When("User sends create new project request")
	public void userSendsCreateNewProjectRequest() {
		response = request.post(API_URI + "project");
	}

	@When("User sends request to get information about project")
	public void userSendsRequestToGetInformationAboutProject() {
		response = request.get(API_URI + "project/{project}", projectName);
	}

	@When("User sends edit request")
	public void userSendsEditRequest() {
		response = request.put(API_URI + "project");
	}

	@When("User sends termination request")
	public void userSendsTerminationRequest() {
		response = request.delete(API_URI + "project/{project}", projectName);
	}

	@When("User sends request to get information about projects")
	public void userSendsRequestToGetInformationAboutProjects() {
		response = request.get(API_URI + "project");
	}

	@When("User sends request to stop the project")
	public void userSendsRequestToStopTheProject() {
		response = request.post(API_URI + "project/stop");
	}

	@When("User sends request to start the project")
	public void userSendsRequestToStartTheProject() {
		response = request.post(API_URI + "project/start");
	}

	@Then("Status code is {int}")
	public void statusCodeIs(int code) {
		assertThat(response.getStatusCode(), equalTo(code));
	}

	@Then("User waits maximum {int} minutes while project is creating")
	public void userWaitMaximumMinutesWhileProjectIsCreating(int timeout) throws URISyntaxException, InterruptedException {
		boolean isRunning = waitForStatus(timeout, EndpointStatusDTO.Status.RUNNING);

		assertTrue("Timeout for project status check reached!", isRunning);
		log.info("Project {} successfully created", projectName);
	}

	@Then("User waits maximum {int} minutes while project is terminating")
	public void userWaitsMaximumTimeoutMinutesWhileProjectIsTerminated(int timeout) throws URISyntaxException, InterruptedException {
		boolean isTerminated = waitForStatus(timeout, EndpointStatusDTO.Status.TERMINATED);

		assertTrue("Timeout for project status check reached!", isTerminated);
		log.info("Project {} successfully terminated", projectName);
	}

	@Then("User waits maximum {int} minutes while project is stopping")
	public void userWaitsMaximumTimeoutMinutesWhileProjectIsStopping(int timeout) throws URISyntaxException, InterruptedException {
		boolean isStopped = waitForStatus(timeout, EndpointStatusDTO.Status.STOPPED);

		assertTrue("Timeout for project status check reached!", isStopped);
		log.info("Project {} successfully stopped", projectName);
	}

	@Then("User waits maximum {int} minutes while project is starting")
	public void userWaitsMaximumTimeoutMinutesWhileProjectIsStarting(int timeout) throws URISyntaxException, InterruptedException {
		boolean isRunning = waitForStatus(timeout, EndpointStatusDTO.Status.RUNNING);

		assertTrue("Timeout for project status check reached!", isRunning);
		log.info("Project {} successfully started", projectName);
	}

	@And("Project information is successfully returned with name {string}, endpoints, groups")
	public void projectInformationIsSuccessfullyReturnedWithNameEndpointsGroups(String expectedProjectName) {
		ProjectDTO project = response.getBody().as(ProjectDTO.class);
		Set<String> endpoints = project.getEndpoints().stream().map(EndpointStatusDTO::getName).collect(Collectors.toSet());
		Set<String> groups = project.getGroups();

		assertEquals(project.getName(), expectedProjectName);
		assertFalse(Collections.disjoint(endpoints, this.endpoints));
		assertFalse(Collections.disjoint(groups, this.groups));
	}

	@And("Project information is successfully updated with shared image enable")
	public void projectInformationIsSuccessfullyUpdatedWithSharedImageEnable() {
		boolean sharedImageEnabled = authenticatedRequest()
				.get(API_URI + "project/{project}", projectName)
				.as(ProjectDTO.class)
				.isSharedImageEnabled();

		assertSame(this.sharedImageEnabled, sharedImageEnabled);
		log.info("Project {} successfully edited", projectName);
	}


	@And("Projects are successfully returned")
	public void projectsAreSuccessfullyReturned() {
		List<ProjectDTO> projects = Arrays.asList(response.getBody().as(ProjectDTO[].class));

		assertTrue(projects.stream().anyMatch(p -> projectName.equals(p.getName())));
	}

	private boolean waitForStatus(int timeout, EndpointStatusDTO.Status status) throws URISyntaxException, InterruptedException {
		boolean correctStatus = false;
		LocalDateTime withTimeout = LocalDateTime.now().plusMinutes(timeout);
		log.info("User wait till {} for project {} to be {}", withTimeout, projectName, status);

		while (!correctStatus && LocalDateTime.now().isBefore(withTimeout)) {
			ProjectStatusDTO projectDTO = authenticatedRequest()
					.get(new URI(API_URI + "project/" + projectName))
					.getBody().as(ProjectStatusDTO.class);

			assertThat(projectDTO.getName(), equalTo(projectName));

			Optional<EndpointStatusDTO> localEndpoint = projectDTO.getEndpoints()
					.stream()
					.filter(e -> LOCAL_ENDPOINT.equals(e.getName()))
					.findAny();

			assertTrue("local endpoint does not exist!", localEndpoint.isPresent());
			assertNotSame("Endpoint with status FAILED", EndpointStatusDTO.Status.FAILED, localEndpoint.get().getStatus());

			correctStatus = status == localEndpoint.get().getStatus();
			TimeUnit.MINUTES.sleep(1);
		}

		return correctStatus;
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
