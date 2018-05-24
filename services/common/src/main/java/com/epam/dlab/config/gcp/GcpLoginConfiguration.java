/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.config.gcp;

import lombok.Data;

@Data
public class GcpLoginConfiguration {
	private final boolean oauth2authenticationEnabled;
	private final String redirectedUri;
	private final String clientId;
	private final String clientSecret;
	private final String applicationName;
	private final int userStateCacheSize;
	private final long userStateCacheExpirationTime;
}
