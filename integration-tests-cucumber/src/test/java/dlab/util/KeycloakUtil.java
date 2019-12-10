package dlab.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.dlab.util.PropertyHelper;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;

public class KeycloakUtil {

	@Getter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class KeyckloakResponse {
		@JsonProperty("access_token")
		private String token;
	}

	public static final String TOKEN_URI = String.format("%s/realms/%s/protocol/openid-connect/token",
			PropertyHelper.read("keycloak.serverUrl"), PropertyHelper.read("keycloak.realm"));

	public static String getToken() {
		return given().formParameters(getFormParams()).post(TOKEN_URI).getBody().as(KeyckloakResponse.class).getToken();
	}

	private static Map<String, ?> getFormParams() {
		Map<String, String> params = new HashMap<>();
		params.put("grant_type", "client_credentials");
		params.put("client_id", PropertyHelper.read("keycloak.clientId"));
		params.put("client_secret", PropertyHelper.read("keycloak.clientSecret"));
		return params;
	}
}
