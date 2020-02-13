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

package com.epam.dlab.backendapi.domain;

import com.epam.dlab.cloud.CloudProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import javax.validation.constraints.Pattern;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointDTO {
	/*
	  the URL_REGEXP_VALIDATION constant is a template for URL pattern,
	  i.e for url verification like "https://localhost:8084/a/$-*^.oLeh;/"
	 */
	private static final String URL_REGEXP_VALIDATION = "^(http(s)?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    /*
      the NAME_REGEXP_VALIDATION - regexp accepts the latin literals and arabic numbers, plus @._ symbols,
      i.e the loc!a1 field name will be ignored
    */
    private static final String NAME_REGEXP_VALIDATION = "[a-zA-Z0-9@_.]+";
	private static final String IMPROPER_FIELD_MESSAGE = "endpoint field is in improper format!";
	@Pattern(regexp = NAME_REGEXP_VALIDATION, message = IMPROPER_FIELD_MESSAGE)
	private final String name;
	@URL(regexp = URL_REGEXP_VALIDATION, message = IMPROPER_FIELD_MESSAGE)
	private final String url;
	private final String account;
	@JsonProperty("endpoint_tag")
	private final String tag;
	private final EndpointStatus status;
	private final CloudProvider cloudProvider;

	public enum EndpointStatus {
		ACTIVE,
		INACTIVE
	}
}
