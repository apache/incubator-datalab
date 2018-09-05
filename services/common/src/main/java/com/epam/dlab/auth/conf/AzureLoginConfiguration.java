/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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

package com.epam.dlab.auth.conf;

import lombok.Getter;

@Getter
public class AzureLoginConfiguration {
	private boolean useLdap;
	private boolean silent;
	private String tenant;
	private String authority;
	private String clientId;
	private String redirectUrl;
	private String responseMode;
	private String prompt;
	private String loginPage;
	private boolean validatePermissionScope;
	private String permissionScope;
	private String managementApiAuthFile;
	private long maxSessionDurabilityMilliseconds = 8L * 60L * 60L * 1000L;// 8 hours
}
