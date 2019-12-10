package dlab.project;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static org.junit.Assert.fail;

public class ProjectSteps {

	@Then("User wait maximum {string} minutes while project is creating")
	public void userWaitMaximumMinutesWhileProjectIsCreating(String arg0) {
		fail("Not implemented");
	}

	@When("User send create new project request")
	public void userSendCreateNewProjectRequest() {
	}

	@And("User try to create new project with name {string}, endpoints {string}, groups {string} " +
			"and key {string}")
	public void userTryToCreateNewProjectWithNameEndpointsGroupsAndKey(String arg0, String arg1, String arg2,
																	   String arg3) {
	}

	@Given("There is no project with name {string} in DLab")
	public void thereIsNoProjectWithNameInDLab(String arg0) {
	}

	@Then("Status code is {int}")
	public void statusCodeIs(int arg0) {

	}
}
