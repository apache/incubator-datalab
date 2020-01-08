package dlab;

import org.apache.dlab.util.PropertyHelper;

public interface Constants {
	String CONNECTION_TIMEOUT_LABEL = "http.connection.timeout";
	int CONNECTION_TIMEOUT = Integer.parseInt(PropertyHelper.read(CONNECTION_TIMEOUT_LABEL));
	String SOCKET_TIMEOUT_LABEL = "http.socket.timeout";
	int SOCKET_TIMEOUT = Integer.parseInt(PropertyHelper.read(SOCKET_TIMEOUT_LABEL));

	String API_URI = PropertyHelper.read("dlab.api.base.uri");
	String LOCAL_ENDPOINT = "local";
	String CLIENT_ID = PropertyHelper.read("keycloak.clientId");
}
