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

package com.epam.datalab.automation.cloud.gcp;

import com.epam.datalab.automation.exceptions.CloudException;
import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Zone;
import com.google.api.services.compute.model.ZoneList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GcpHelper {

	private static final Logger LOGGER = LogManager.getLogger(GcpHelper.class);
	private static final Duration CHECK_TIMEOUT = Duration.parse("PT10m");
	private static final String LOCALHOST_IP = ConfigPropertyValue.get("LOCALHOST_IP");
	private static final String NOT_EXIST = "doesn't exist for this resource type";

	private GcpHelper() {
	}

	private static List<Instance> getInstances(String projectId, List<String> zones) throws IOException {
		List<Instance> instanceList = new ArrayList<>();
		for (String zone : zones) {
			Compute.Instances.List request = ComputeService.getInstance().instances().list(projectId, zone);
			InstanceList response;
			do {
				response = request.execute();
				if (response.getItems() == null) {
					continue;
				}
				instanceList.addAll(response.getItems());
				request.setPageToken(response.getNextPageToken());
			} while (response.getNextPageToken() != null);

		}
		return !instanceList.isEmpty() ? instanceList : null;
	}

	public static List<String> getInstancePrivateIps(Instance instance) {
		return instance.getNetworkInterfaces().stream().filter(Objects::nonNull)
				.map(NetworkInterface::getNetworkIP).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public static List<String> getInstancePublicIps(Instance instance) {
		return instance.getNetworkInterfaces()
				.stream().filter(Objects::nonNull)
				.map(NetworkInterface::getAccessConfigs)
				.filter(Objects::nonNull).map(Collection::stream)
				.flatMap(Function.identity()).filter(Objects::nonNull)
				.map(AccessConfig::getNatIP).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}


	public static List<Instance> getInstancesByName(String name, String projectId, boolean restrictionMode,
													List<String> zones) throws IOException {
		if (ConfigPropertyValue.isRunModeLocal()) {
			List<Instance> mockedInstanceList = new ArrayList<>();
			Instance mockedInstance = mock(Instance.class);
			NetworkInterface mockedNetworkInterface = mock(NetworkInterface.class);
			when(mockedInstance.getNetworkInterfaces()).thenReturn(Collections.singletonList(mockedNetworkInterface));
			when(mockedInstance.getNetworkInterfaces().get(0).getNetworkIP()).thenReturn(LOCALHOST_IP);
			AccessConfig mockedAccessConfig = mock(AccessConfig.class);
			when(mockedInstance.getNetworkInterfaces().get(0).getAccessConfigs())
					.thenReturn(Collections.singletonList(mockedAccessConfig));
			when(mockedInstance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP())
					.thenReturn(LOCALHOST_IP);
			mockedInstanceList.add(mockedInstance);
			return mockedInstanceList;
		}
		List<Instance> instanceList = getInstances(projectId, zones);
		if (instanceList == null) {
			LOGGER.warn("There is not any virtual machine in GCP for project with id {}", projectId);
			return instanceList;
		}
		if (restrictionMode) {
			instanceList.removeIf(instance -> !hasName(instance, name));
		} else {
			instanceList.removeIf(instance -> !containsName(instance, name));
		}
		return !instanceList.isEmpty() ? instanceList : null;
	}

	private static boolean hasName(Instance instance, String name) {
		return instance.getName().equals(name);
	}

	private static boolean containsName(Instance instance, String name) {
		return instance.getName().contains(name);
	}

	private static String getStatus(Instance instance) {
		return instance.getStatus().toLowerCase();
	}

	public static void checkGcpStatus(String instanceName, String projectId, GcpInstanceState expGcpStatus, boolean
			restrictionMode, List<String> zones) throws InterruptedException, IOException {

		LOGGER.info("Check status of instance with name {} on GCP", instanceName);
		if (ConfigPropertyValue.isRunModeLocal()) {
			LOGGER.info("GCP instance with name {} fake status is {}", instanceName, expGcpStatus);
			return;
		}
		List<Instance> instancesWithName = getInstancesByName(instanceName, projectId, restrictionMode, zones);
		if (instancesWithName == null) {
			LOGGER.warn("There is not any instance in GCP with name {}", instanceName);
			return;
		}

		String instanceStatus;
		long requestTimeout = ConfigPropertyValue.getGcpRequestTimeout().toMillis();
		long timeout = CHECK_TIMEOUT.toMillis();
		long expiredTime = System.currentTimeMillis() + timeout;
		Instance instance = instancesWithName.get(0);
		while (true) {
			instanceStatus = getStatus(instance);
			if (instanceStatus.equalsIgnoreCase(expGcpStatus.toString())) {
				break;
			}
			if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
				LOGGER.info("GCP instance with name {} state is {}", instanceName, getStatus(instance));
				throw new CloudException("Timeout has been expired for check status of GCP instance with " +
						"name " + instanceName);
			}
			Thread.sleep(requestTimeout);
		}

		for (Instance inst : instancesWithName) {
			LOGGER.info("GCP instance with name {} status is {}. Instance id {}, private IP {}, public " +
							"IP {}",
					instanceName, getStatus(inst), inst.getId(), (!getInstancePrivateIps(inst).isEmpty() ?
							getInstancePrivateIps(inst).get(0) : NOT_EXIST),
					(!getInstancePublicIps(inst).isEmpty() ? getInstancePublicIps(inst).get(0) : NOT_EXIST));
		}
		Assert.assertEquals(instanceStatus, expGcpStatus.toString(), "GCP instance with name " + instanceName +
				" status is not correct. Instance id " + instance.getId() + ", private IP " +
				(!getInstancePrivateIps(instance).isEmpty() ? getInstancePrivateIps(instance).get(0) : NOT_EXIST) +
				", public IP " +
				(!getInstancePublicIps(instance).isEmpty() ? getInstancePublicIps(instance).get(0) : NOT_EXIST));
	}

	public static List<String> getAvailableZonesForProject(String projectId) throws IOException {
		if (ConfigPropertyValue.isRunModeLocal()) {
			return Collections.emptyList();
		}
		List<Zone> zoneList = new ArrayList<>();
		Compute.Zones.List request = ComputeService.getInstance().zones().list(projectId);
		ZoneList response;
		do {
			response = request.execute();
			if (response.getItems() == null) {
				continue;
			}
			zoneList.addAll(response.getItems());
			request.setPageToken(response.getNextPageToken());
		} while (response.getNextPageToken() != null);
		return zoneList.stream().map(Zone::getDescription).collect(Collectors.toList());
	}

	private static class ComputeService {

		private static Compute instance;

		private ComputeService() {
		}

		static synchronized Compute getInstance() throws IOException {
			if (!ConfigPropertyValue.isRunModeLocal() && instance == null) {
				try {
					instance = createComputeService();
				} catch (GeneralSecurityException e) {
					LOGGER.info("An exception occured: {}", e);
				}
			}
			return instance;
		}

		private static Compute createComputeService() throws IOException, GeneralSecurityException {
			HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

			GoogleCredential credential =
					GoogleCredential.fromStream(new FileInputStream(ConfigPropertyValue.getGcpAuthFileName()));
			if (credential.createScopedRequired()) {
				credential = credential.createScoped(
						Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
			}

			return new Compute.Builder(httpTransport, jsonFactory, credential)
					.setApplicationName("Google-ComputeSample/0.1")
					.build();
		}

	}

}




