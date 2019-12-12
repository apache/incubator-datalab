package dlab.project;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import dlab.util.KeycloakUtil;
import org.apache.dlab.dto.CreateProjectDTO;
import org.apache.dlab.dto.EndpointStatusDTO;
import org.apache.dlab.dto.ProjectKeyDTO;
import org.apache.dlab.dto.ProjectStatusDTO;
import org.apache.dlab.util.JacksonMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static dlab.Constants.API_URI;
import static dlab.Constants.LOCAL_ENDPOINT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ProjectSteps {

	private RequestSpecification createProjectRequest;
	private Response response;
	private String projectName;
	private String publicKey;
	private Set<String> endpoints;
	private Set<String> groups;


	@Given("There is no project with name {string} in DLab")
	public void thereIsNoProjectWithNameInDLab(String projectName) throws URISyntaxException {
		assertThat(authenticatedRequest()
				.get(new URI(API_URI + "project/" + projectName))
				.getStatusCode(), equalTo(404));
	}

	@And("There are the following endpoints")
	public void thereAreTheFollowingEndpoints(List<String> endpoints) {
		this.endpoints = new HashSet<>(endpoints);
	}

	@And("There are the following groups")
	public void thereAreTheFollowingGroups(List<String> groups) {
		this.groups = new HashSet<>(groups);
	}

	@And("User generate new publicKey")
	public void userTryToGenerateNewPublicKey() {
		this.publicKey = authenticatedRequest().contentType(ContentType.JSON)
				.post(API_URI + "project/keys")
				.getBody().as(ProjectKeyDTO.class)
				.getPublicKey();
	}

	@When("User send create new project request")
	public void userSendCreateNewProjectRequest() {
		response = createProjectRequest.post(API_URI + "project");
	}

	@And("User try to create new project with name {string}, endpoints, groups and publicKey")
	public void userTryToCreateNewProjectWithNameEndpointsGroupsAndKey(String projectName) {
		this.projectName = projectName;
		createProjectRequest = given()
				.body(JacksonMapper.marshall(new CreateProjectDTO(projectName, groups, endpoints, publicKey, projectName)))
				.auth()
				.oauth2(KeycloakUtil.getToken())
				.contentType(ContentType.JSON);

	}

	@Then("Status code is {int}")
	public void statusCodeIs(int code) {
		assertThat(response.getStatusCode(), equalTo(code));
	}

	@Then("User wait maximum {int} minutes while project is creating")
	public void userWaitMaximumMinutesWhileProjectIsCreating(int timeout) throws URISyntaxException, InterruptedException {
		boolean isRunning = false;
		LocalDateTime withTimeout = LocalDateTime.now().plusMinutes(timeout);
		while (!isRunning && LocalDateTime.now().isBefore(withTimeout)) {
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

			isRunning = EndpointStatusDTO.Status.RUNNING == localEndpoint.get().getStatus();
			TimeUnit.MINUTES.sleep(1);
		}

		assertTrue("Timeout for project status check reached!", isRunning);
	}

	private HashSet<String> getSetFromString(String string) {
		return new HashSet<>(Arrays.asList(string.split(",")));
	}

	private RequestSpecification authenticatedRequest() {
		return given()
				.auth()
				.oauth2(KeycloakUtil.getToken());
	}
}
