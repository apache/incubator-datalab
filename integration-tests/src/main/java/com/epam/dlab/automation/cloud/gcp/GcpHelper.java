package com.epam.dlab.automation.cloud.gcp;

import com.epam.dlab.automation.cloud.CloudException;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GcpHelper {

	private static final Logger LOGGER = LogManager.getLogger(GcpHelper.class);
	private static final Duration CHECK_TIMEOUT = Duration.parse("PT10m");
	private static final String LOCALHOST_IP = ConfigPropertyValue.get("LOCALHOST_IP");
	private static final String NOT_EXIST = "doesn't exist for this resource type";

	private static Compute computeService;


	static {
		try {
			computeService = createComputeService();
		} catch (IOException | GeneralSecurityException e) {
			LOGGER.warn("Exception is occured ", e);
		}
	}

	private GcpHelper() {
	}

	private static Compute createComputeService() throws IOException, GeneralSecurityException {
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

		GoogleCredential credential = GoogleCredential.getApplicationDefault();
		if (credential.createScopedRequired()) {
			credential =
					credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
		}

		return new Compute.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName("Google-ComputeSample/0.1")
				.build();
	}


	public static List<Instance> getInstances(String project, String... zones) throws IOException {
		List<Instance> instanceList = new ArrayList<>();
		for (String zone : zones) {
			Compute.Instances.List request = computeService.instances().list(project, zone);
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

	private static List<String> getInstancePrivateIps(Instance instance) {
		return instance.getNetworkInterfaces().stream().map(NetworkInterface::getNetworkIP).collect(Collectors.toList
				());
	}

	private static List<String> getInstancePublicIps(Instance instance) {
		return instance.getNetworkInterfaces().stream()
				.map(e -> e.getAccessConfigs().stream().map(AccessConfig::getNatIP))
				.flatMap(Function.identity()).collect(Collectors.toList());
	}


	public static List<Instance> getInstancesByName(String name, String project, boolean restrictionMode,
													String... zones) throws IOException {
		if (ConfigPropertyValue.isRunModeLocal()) {
			List<Instance> mockedInstanceList = new ArrayList<>();
			Instance mockedInstance = mock(Instance.class);
			when(getInstancePublicIps(mockedInstance)).thenReturn(Collections.singletonList(LOCALHOST_IP));
			when(getInstancePrivateIps(mockedInstance)).thenReturn(Collections.singletonList(LOCALHOST_IP));
			mockedInstanceList.add(mockedInstance);
			return mockedInstanceList;
		}
		List<Instance> instanceList = getInstances(project, zones);
		if (instanceList == null) {
			LOGGER.warn("There is not any virtual machine in GCP");
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
		return instance.getStatus();
	}

	public static void checkGcpStatus(String instanceName, String project, String expGcpStatus, boolean
			restrictionMode, String... zones)
			throws CloudException, InterruptedException, IOException {
		LOGGER.info("Check status of instance with name {} on GCP", instanceName);
		if (ConfigPropertyValue.isRunModeLocal()) {
			LOGGER.info("GCP instance with name {} fake status is {}", instanceName, expGcpStatus);
			return;
		}
		List<Instance> instancesWithName = getInstancesByName(instanceName, project, restrictionMode, zones);
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
			if (instanceStatus.equalsIgnoreCase(expGcpStatus)) {
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
		Assert.assertEquals(instanceStatus, expGcpStatus, "GCP instance with name " + instanceName +
				" status is not correct. Instance id " + instance.getId() + ", private IP " +
				(!getInstancePrivateIps(instance).isEmpty() ? getInstancePrivateIps(instance).get(0) : NOT_EXIST) +
				", public IP " +
				(!getInstancePublicIps(instance).isEmpty() ? getInstancePublicIps(instance).get(0) : NOT_EXIST));
	}

}
