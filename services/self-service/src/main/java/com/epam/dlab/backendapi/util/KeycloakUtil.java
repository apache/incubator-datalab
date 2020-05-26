package com.epam.dlab.backendapi.util;

import com.epam.dlab.exceptions.DlabException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

public class KeycloakUtil {

    public static IDToken parseToken(String encoded) {
        try {
            String[] parts = encoded.split("\\.");
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalArgumentException("Parsing error");
            }
            byte[] bytes = Base64Url.decode(parts[1]);
            return JsonSerialization.readValue(bytes, IDToken.class);
        } catch (Exception e) {
            throw new DlabException("Can not parse token due to: " + e.getMessage());
        }
    }
}
