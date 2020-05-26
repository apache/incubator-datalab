package dlab.login;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import gherkin.deps.com.google.gson.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;

import static com.jayway.restassured.RestAssured.given;
import static dlab.Constants.API_URI;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class LoginSteps {


	private static final String LOGIN_RESOURCE_PATH = API_URI + "user/login";
	private RequestSpecification request;
	private Response response;

	@Given("User try to login to Dlab with {string} and {string}")
	public void userProvidedLoginAndPassword(String username, String password) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("username", username);
		jsonObject.addProperty("password", password);
		request = given().body(jsonObject.toString()).contentType(ContentType.JSON);
	}

	@When("user try to login")
	public void userTryToLogin() throws URISyntaxException {
		response = request.post(new URI(LOGIN_RESOURCE_PATH));
	}

	@Then("response code is {string}")
	public void responseCodeIs(String status) {
		assertThat(response.getStatusCode(), equalTo(Integer.valueOf(status)));

	}
}
