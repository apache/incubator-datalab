package dlab.endpoint;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.dlab.dto.EndpointDTO;
import org.apache.dlab.mongo.MongoDBHelper;
import org.apache.dlab.util.JacksonMapper;

import java.net.URI;
import java.net.URISyntaxException;

import static com.jayway.restassured.RestAssured.given;
import static dlab.Constants.API_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class EndpointSteps {
	private RequestSpecification request;
	private Response response;
	private String name;

	@Given("User try to create new endpoint with name {string} and uri {string} and account {string} and {string}")
	public void userTryToCreateNewEndpoint(String name, String uri, String account, String tag) {
		this.name = name;
		request = given().body(JacksonMapper.marshall(new EndpointDTO(name, uri, account, tag)))
				.auth()
				.oauth2("token123")
				.contentType(ContentType.JSON);

	}

	@When("User send create new endpoint request")
	public void userSendCreateNewEndpoint() {
		response = request.post(API_URI + "endpoint");
	}

	@Given("There is no endpoints in DLab")
	public void thereIsNoEndpointsInDLab() {
		MongoDBHelper.cleanCollection("endpoints");

	}

	@Then("Response status code is {int}")
	public void responseStatusCodeIs(int code) {
		assertThat(response.getStatusCode(), equalTo(code));
	}

	@And("Endpoint URI is present in location header")
	public void endpointURIIsPresentInLocationHeader() {
		assertThat(response.getHeader("Location"), equalTo(API_URI + "endpoint/" + name));
	}

	@When("User try to get information about endpoint with name {string}")
	public void userTryToGetInformationAboutEndpointWithName(String endpoint) throws URISyntaxException {
		response = authenticatedRequest()
				.get(new URI(API_URI + "endpoint/" + endpoint));

	}

	@And("Endpoint information is successfully returned with " +
			"name {string}, uri {string}, account {string}, and tag {string}")
	public void endpointInformationIsSuccessfullyReturnedWithNameUriAccountAndTag(String name, String uri,
																				  String account, String tag) {
		final EndpointDTO dto = response.getBody().as(EndpointDTO.class);
		assertThat(dto.getAccount(), equalTo(account));
		assertThat(dto.getName(), equalTo(name));
		assertThat(dto.getUrl(), equalTo(uri));
		assertThat(dto.getTag(), equalTo(tag));

	}

	@When("User try to get information about endpoints")
	public void userTryToGetInformationAboutEndpoints() throws URISyntaxException {
		response = authenticatedRequest()
				.get(new URI(API_URI + "endpoint"));

	}

	@And("There are endpoints with name test1 and test2")
	public void thereAreEndpointsWithNameTestAndTest() {
		final EndpointDTO[] endpoints = response.getBody().as(EndpointDTO[].class);
		assertThat(2, equalTo(endpoints.length));
		assertThat("test1", equalTo(endpoints[0].getName()));
		assertThat("test2", equalTo(endpoints[1].getName()));
	}

	private RequestSpecification authenticatedRequest() {
		return given()
				.auth()
				.oauth2("token123");
	}
}
