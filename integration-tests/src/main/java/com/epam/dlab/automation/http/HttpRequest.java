package com.epam.dlab.automation.http;

import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

import java.io.File;

import static com.jayway.restassured.RestAssured.given;

public class HttpRequest {

	public void AddHeader(String headerType, String headerValue) {
		given().header(headerType, headerValue);
	}

	public void AddAuthorizationBearer(String token) {
		this.AddHeader("Authorization", "Bearer " + token);
	}

	public Response webApiGet(String url) {
		return given().contentType(ContentType.JSON).when().get(url);
	}

	public Response webApiGet(String url, String token) {
		return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).when().get(url);
	}

	public Response webApiPost(String url, String contentType, Object body) {
		return given().contentType(contentType).body(body).when().post(url);
	}

	public Response webApiPost(String url, String contentType) {
		return given().contentType(contentType).when().post(url);
	}

	public Response webApiPost(String url, String contentType, String token) {
		return given()
				.contentType(contentType)
				.header("Authorization", "Bearer " + token)
				.multiPart(new File(ConfigPropertyValue.getAccessKeyPubFileName()))
				.formParam(ConfigPropertyValue.getAccessKeyPubFileName())
				.contentType(contentType)
				.when()
				.post(url);
	}

	public Response webApiPost(String url, String contentType, Object body, String token) {
		return given().contentType(contentType).header("Authorization", "Bearer " + token).body(body).when().post(url);
	}

	public Response webApiPut(String url, String contentType, Object body, String token) {
		return given().contentType(contentType).header("Authorization", "Bearer " + token).body(body).when().put(url);
	}

	public Response webApiDelete(String url, String contentType, String token) {
		return given().contentType(contentType).header("Authorization", "Bearer " + token).when().delete(url);
	}
}
