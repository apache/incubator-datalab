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

package com.epam.dlab.backendapi;

import com.epam.dlab.ServiceConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.validation.ProvisioningServiceCloudConfigurationSequenceProvider;
import com.epam.dlab.validation.AwsValidation;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import io.dropwizard.util.Duration;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.group.GroupSequenceProvider;

@GroupSequenceProvider(ProvisioningServiceCloudConfigurationSequenceProvider.class)
public class ProvisioningServiceApplicationConfiguration extends ServiceConfiguration implements Directories {

	@NotEmpty
	@JsonProperty
	private String keyDirectory;

	@NotEmpty
	@JsonProperty
	private String responseDirectory;

	@NotEmpty
	@JsonProperty
	private String dockerLogDirectory;

	@NotEmpty
	@JsonProperty
	private String handlerDirectory;

	@JsonProperty
	private Duration warmupPollTimeout = Duration.seconds(3);

	@JsonProperty
	private Duration resourceStatusPollTimeout = Duration.minutes(3);

	@JsonProperty
	private Duration keyLoaderPollTimeout = Duration.minutes(2);

	@JsonProperty
	private Duration requestEnvStatusTimeout = Duration.seconds(30);

	@NotEmpty
	@JsonProperty
	private String adminKey;

	@NotEmpty
	@JsonProperty
	private String edgeImage;

	@JsonProperty
	private Duration fileLengthCheckDelay = Duration.seconds(3);

	@NotEmpty(groups = AwsValidation.class)
	@JsonProperty
	private String emrEC2RoleDefault;

	@NotEmpty(groups = AwsValidation.class)
	@JsonProperty
	private String emrServiceRoleDefault;

	@JsonProperty
	private int processMaxThreadsPerJvm = 50;

	@JsonProperty
	private int processMaxThreadsPerUser = 5;

	@JsonProperty
	private Duration processTimeout = Duration.hours(3);
	@JsonProperty
	private String backupScriptPath;
	@JsonProperty
	private String backupDirectory;
	@JsonProperty
	private boolean handlersPersistenceEnabled;

	private KeycloakConfiguration keycloakConfiguration = new KeycloakConfiguration();

	public boolean isHandlersPersistenceEnabled() {
		return handlersPersistenceEnabled;
	}

	public String getKeyDirectory() {
		return keyDirectory;
	}

	public Duration getWarmupPollTimeout() {
		return warmupPollTimeout;
	}

	public Duration getResourceStatusPollTimeout() {
		return resourceStatusPollTimeout;
	}

	public Duration getKeyLoaderPollTimeout() {
		return keyLoaderPollTimeout;
	}

	/**
	 * Return the timeout for the check the status of environment resources.
	 */
	public Duration getRequestEnvStatusTimeout() {
		return requestEnvStatusTimeout;
	}

	public String getAdminKey() {
		return adminKey;
	}

	public String getEdgeImage() {
		return edgeImage;
	}

	public Duration getFileLengthCheckDelay() {
		return fileLengthCheckDelay;
	}

	public String getEmrEC2RoleDefault() {
		return emrEC2RoleDefault;
	}

	public String getEmrServiceRoleDefault() {
		return emrServiceRoleDefault;
	}

	public String getWarmupDirectory() {
		return responseDirectory + WARMUP_DIRECTORY;
	}

	public String getImagesDirectory() {
		return responseDirectory + IMAGES_DIRECTORY;
	}

	public String getKeyLoaderDirectory() {
		return responseDirectory + KEY_LOADER_DIRECTORY;
	}

	public String getDockerLogDirectory() {
		return dockerLogDirectory;
	}

	public int getProcessMaxThreadsPerJvm() {
		return processMaxThreadsPerJvm;
	}

	public int getProcessMaxThreadsPerUser() {
		return processMaxThreadsPerUser;
	}

	public Duration getProcessTimeout() {
		return processTimeout;
	}

	public String getBackupScriptPath() {
		return backupScriptPath;
	}

	public String getBackupDirectory() {
		return backupDirectory;
	}

	public String getHandlerDirectory() {
		return handlerDirectory;
	}

	public KeycloakConfiguration getKeycloakConfiguration() {
		return keycloakConfiguration;
	}
}
