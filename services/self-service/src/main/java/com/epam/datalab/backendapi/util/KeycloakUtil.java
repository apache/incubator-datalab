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

package com.epam.datalab.backendapi.util;

import com.epam.datalab.exceptions.DatalabException;
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
            throw new DatalabException("Can not parse token due to: " + e.getMessage(), e);
        }
    }
}
