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

package datalab.login;

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
import static datalab.Constants.API_URI;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class LoginSteps {


    private static final String LOGIN_RESOURCE_PATH = API_URI + "user/login";
    private RequestSpecification request;
    private Response response;

    @Given("User try to login to Datalab with {string} and {string}")
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
