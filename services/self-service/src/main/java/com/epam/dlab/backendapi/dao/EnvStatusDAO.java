/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.epam.dlab.backendapi.resources.aws.ComputationalResourceAws;
import com.epam.dlab.backendapi.resources.dto.HealthStatusEnum;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusResource;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.dto.status.EnvResourceList;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.*;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_EDGE;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.epam.dlab.dto.UserInstanceStatus.TERMINATED;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.*;
import static java.util.Objects.nonNull;

/**
 * DAO for updates of the status of environment resources.
 */
@Singleton
public class EnvStatusDAO extends BaseDAO {
	private static final Logger LOGGER = LoggerFactory.getLogger(EnvStatusDAO.class);

	private static final String EDGE_PUBLIC_IP = "public_ip";
	private static final String COMPUTATIONAL_STATUS = COMPUTATIONAL_RESOURCES + "." + STATUS;
	private static final String COMPUTATIONAL_STATUS_FILTER = COMPUTATIONAL_RESOURCES + FIELD_SET_DELIMETER + STATUS;
	private static final String COMPUTATIONAL_SPOT = "slave_node_spot";
	private static final String IMAGE = "image";

	private static final Bson INCLUDE_EDGE_FIELDS = include(INSTANCE_ID, EDGE_STATUS, EDGE_PUBLIC_IP);
	private static final Bson INCLUDE_EXP_FIELDS = include(INSTANCE_ID, STATUS,
			COMPUTATIONAL_RESOURCES + "." + INSTANCE_ID, COMPUTATIONAL_RESOURCES + "." + IMAGE, COMPUTATIONAL_STATUS,
			EXPLORATORY_NAME, COMPUTATIONAL_RESOURCES + "." + ComputationalDAO.COMPUTATIONAL_NAME);
	private static final Bson INCLUDE_EXP_UPDATE_FIELDS = include(EXPLORATORY_NAME, INSTANCE_ID, STATUS,
			COMPUTATIONAL_RESOURCES + "." + ComputationalDAO.COMPUTATIONAL_NAME, COMPUTATIONAL_RESOURCES + "." +
					INSTANCE_ID,
			COMPUTATIONAL_STATUS, COMPUTATIONAL_RESOURCES + "." + IMAGE);
	public static final String COMPUTATIONAL_NAME = "computational_name";

	@Inject
	private SelfServiceApplicationConfiguration configuration;

	/**
	 * Finds and returns the list of user resources.
	 *
	 * @param user name.
	 */
	public EnvResourceList findEnvResources(String user) {
		List<EnvResource> hostList = new ArrayList<>();
		List<EnvResource> clusterList = new ArrayList<>();

		getEdgeNode(user).ifPresent(edge -> addResource(hostList, edge, EDGE_STATUS, ResourceType.EDGE, null));

		stream(find(USER_INSTANCES, eq(USER, user), fields(INCLUDE_EXP_FIELDS, excludeId())))
				.forEach(exp -> {
					final String exploratoryName = exp.getString(EXPLORATORY_NAME);
					addResource(hostList, exp, STATUS, ResourceType.EXPLORATORY, exploratoryName);
					addComputationalResources(hostList, clusterList, exp, exploratoryName);
				});
		return new EnvResourceList()
				.withHostList(!hostList.isEmpty() ? hostList : Collections.emptyList())
				.withClusterList(!clusterList.isEmpty() ? clusterList : Collections.emptyList());
	}

	@SuppressWarnings("unchecked")
	private void addComputationalResources(List<EnvResource> hostList, List<EnvResource> clusterList, Document exp,
										   String
												   exploratoryName) {
		((List<Document>) exp.getOrDefault(COMPUTATIONAL_RESOURCES, Collections.emptyList()))
				.forEach(comp -> addComputational(hostList, clusterList, exploratoryName, comp));
	}

	private void addComputational(List<EnvResource> hostList, List<EnvResource> clusterList, String exploratoryName,
								  Document computational) {
		final List<EnvResource> resourceList = DataEngineType.CLOUD_SERVICE ==
				DataEngineType.fromDockerImageName(computational.getString(IMAGE)) ? clusterList :
				hostList;
		addResource(resourceList, computational, STATUS, ResourceType.COMPUTATIONAL,
				String.join("_", exploratoryName, computational.getString(COMPUTATIONAL_NAME)));
	}

	/**
	 * Finds and returns the of computational resource.
	 *
	 * @param user       the name of user.
	 * @param fullReport return full report if <b>true</b> otherwise common status only.
	 * @throws DlabException in case of any exception
	 */
	public HealthStatusPageDTO getHealthStatusPageDTO(String user, boolean fullReport) {
		List<HealthStatusResource> listResource = new ArrayList<>();
		final HealthStatusPageDTO healthStatusPageDTO = new HealthStatusPageDTO()
				.withStatus(HealthStatusEnum.OK);

		getEdgeNode(user).ifPresent(edge -> {
			final String edgeStatus = edge.getString(EDGE_STATUS);
			if (UserInstanceStatus.RUNNING != UserInstanceStatus.of(edgeStatus)) {
				healthStatusPageDTO.withStatus(HealthStatusEnum.ERROR);
			}
			if (fullReport) {
				listResource.add(new HealthStatusResource()
						.withType("Edge Node")
						.withResourceId(edge.getString(EDGE_PUBLIC_IP))
						.withStatus(edgeStatus));
			}
		});
		return healthStatusPageDTO
				.withListResources(fullReport ? listResource : null);
	}


	/**
	 * Updates the status of exploratory and computational for user.
	 *
	 * @param user the name of user.
	 * @param list the status of node.
	 */
	public void updateEnvStatus(String user, EnvResourceList list) {
		if (list != null && notEmpty(list.getHostList())) {
			updateEdgeStatus(user, list.getHostList());
			if (!list.getHostList().isEmpty()) {
				stream(find(USER_INSTANCES, eq(USER, user),
						fields(INCLUDE_EXP_UPDATE_FIELDS, excludeId())))
						.filter(this::instanceIdPresent)
						.forEach(exp -> updateUserResourceStatuses(user, list, exp));
			}
		}
	}

	public Set<String> fetchActiveEnvUsers() {
		return Stream.concat(
				stream(find(USER_INSTANCES, eq(STATUS, UserInstanceStatus.RUNNING.toString()),
						fields(include(USER), excludeId()))).map(d -> d.getString(USER)),
				stream(find(USER_EDGE, eq(EDGE_STATUS, UserInstanceStatus.RUNNING.toString()),
						fields(include(ID)))).map(d -> d.getString(ID))
		).collect(Collectors.toSet());
	}

	public Set<String> fetchAllUsers() {
		return stream(find(USER_EDGE)).map(d -> d.getString(ID))
				.collect(Collectors.toSet());
	}

	@SuppressWarnings("unchecked")
	private void updateUserResourceStatuses(String user, EnvResourceList list, Document exp) {
		final String exploratoryName = exp.getString(EXPLORATORY_NAME);
		getEnvResourceAndRemove(list.getHostList(), exp.getString(INSTANCE_ID))
				.ifPresent(resource -> updateExploratoryStatus(user, exploratoryName, exp.getString(STATUS),
						resource.getStatus()));

		((List<Document>) exp.getOrDefault(COMPUTATIONAL_RESOURCES, Collections.emptyList()))
				.stream()
				.filter(this::instanceIdPresent)
				.forEach(comp -> updateComputational(user, list, exploratoryName, comp));
	}

	private void updateComputational(String user, EnvResourceList list, String exploratoryName, Document comp) {
		final List<EnvResource> listToCheck = DataEngineType.CLOUD_SERVICE ==
				DataEngineType.fromDockerImageName(comp.getString(IMAGE)) ?
				list.getClusterList() : list.getHostList();
		getEnvResourceAndRemove(listToCheck, comp.getString(INSTANCE_ID))
				.ifPresent(resource -> updateComputationalStatus(user, exploratoryName,
						comp.getString(ComputationalDAO.COMPUTATIONAL_NAME), comp.getString(STATUS), resource
								.getStatus()));
	}

	private boolean instanceIdPresent(Document d) {
		return nonNull(d.getString(INSTANCE_ID));
	}

	private Optional<String> getInstanceId(Document document) {
		return Optional.ofNullable(document.getString(INSTANCE_ID));
	}


	/**
	 * Find and return the id of instance for EDGE node.
	 *
	 * @param user the name of user.
	 */
	private Optional<Document> getEdgeNode(String user) {
		return findOne(USER_EDGE,
				eq(ID, user),
				fields(INCLUDE_EDGE_FIELDS, excludeId()));
	}

	/**
	 * Find and return the resource item for given id (of instance or cluster) or <b>null<b> otherwise.
	 *
	 * @param list the list of resources.
	 * @param id   the id of instance or cluster.
	 */
	private Optional<EnvResource> getEnvResourceAndRemove(List<EnvResource> list, String id) {
		if (list != null) {
			return IntStream.range(0, list.size())
					.filter(i -> list.get(i).getId().equals(id))
					.mapToObj(i -> getAndRemove(list, i)).findAny();
		}
		return Optional.empty();
	}

	private EnvResource getAndRemove(List<EnvResource> list, int i) {
		final EnvResource envResource = list.get(i);
		list.remove(i);
		return envResource;
	}

	/**
	 * Translate the status of instance in Amazon into exploratory's status.
	 *
	 * @param oldStatus the current status of exploratory.
	 * @param newStatus the current status of instance in Amazon.
	 */
	private UserInstanceStatus getInstanceNewStatus(UserInstanceStatus oldStatus, String newStatus) {
		/* AWS statuses: pending, running, shutting-down, terminated, stopping, stopped */
		UserInstanceStatus status;
		if ("pending".equalsIgnoreCase(newStatus) || "stopping".equalsIgnoreCase(newStatus)) {
			return oldStatus;
		} else if ("shutting-down".equalsIgnoreCase(newStatus)) {
			status = UserInstanceStatus.TERMINATING;
		} else {
			status = UserInstanceStatus.of(newStatus);
		}

		switch (oldStatus) {
			case CREATING_IMAGE:
				return !status.in(UserInstanceStatus.TERMINATED, UserInstanceStatus.TERMINATING,
						UserInstanceStatus.RUNNING) ? status : oldStatus;
			case CREATING:
				return (status.in(UserInstanceStatus.TERMINATED, UserInstanceStatus.STOPPED) ? status : oldStatus);
			case RUNNING:
			case STOPPING:
				return (status.in(UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATED,
						UserInstanceStatus.STOPPING, UserInstanceStatus.STOPPED) ? status : oldStatus);
			case STARTING:
				return (status.in(UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATED,
						UserInstanceStatus.STOPPING) ? status : oldStatus);
			case STOPPED:
				return (status.in(UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATED,
						UserInstanceStatus.RUNNING) ? status : oldStatus);
			case TERMINATING:
				return (status.in(UserInstanceStatus.TERMINATED) ? status : oldStatus);
			case FAILED:
			case TERMINATED:
			default:
				return oldStatus;
		}
	}

	/**
	 * Updates the status of EDGE node for user.
	 *
	 * @param user     the name of user.
	 * @param hostList list with instance ids for edge resources
	 * @throws DlabException in case of exception
	 */
	private void updateEdgeStatus(String user, List<EnvResource> hostList) {
		LOGGER.trace("Update EDGE status for user {}", user);
		getEdgeNode(user)
				.ifPresent(edge -> getInstanceId(edge)
						.ifPresent(instanceId -> getEnvResourceAndRemove(hostList, instanceId)
								.ifPresent(r -> updateEdgeStatus(user, edge, instanceId, r))));

	}

	private void updateEdgeStatus(String user, Document edge, String instanceId, EnvResource r) {
		LOGGER.trace("Update EDGE status for user {} with instance_id {} from {} to {}",
				user, instanceId, edge.getString(EDGE_STATUS), r.getStatus());
		String oldStatus = edge.getString(EDGE_STATUS);
		UserInstanceStatus oStatus =
				(oldStatus == null ? UserInstanceStatus.CREATING : UserInstanceStatus.of(oldStatus));
		UserInstanceStatus status = getInstanceNewStatus(oStatus, r.getStatus());
		LOGGER.trace("EDGE status translated for user {} with instanceId {} from {} to {}",
				user, instanceId, r.getStatus(), status);
		if (oStatus != status) {
			LOGGER.debug("EDGE status will be updated from {} to {}", oldStatus, status);
			updateOne(USER_EDGE,
					eq(ID, user),
					Updates.set(EDGE_STATUS, status.toString()));
		}
	}

	/**
	 * Update the status of exploratory if it needed.
	 *
	 * @param user            the user name
	 * @param exploratoryName the name of exploratory
	 * @param oldStatus       old status
	 * @param newStatus       new status
	 */
	private void updateExploratoryStatus(String user, String exploratoryName,
										 String oldStatus, String newStatus) {
		LOGGER.trace("Update exploratory status for user {} with exploratory {} from {} to {}", user, exploratoryName,
				oldStatus, newStatus);
		UserInstanceStatus oStatus = UserInstanceStatus.of(oldStatus);
		UserInstanceStatus status = getInstanceNewStatus(oStatus, newStatus);
		LOGGER.trace("Exploratory status translated for user {} with exploratory {} from {} to {}", user,
				exploratoryName, newStatus, status);

		if (oStatus != status) {
			LOGGER.debug("Exploratory status for user {} with exploratory {} will be updated from {} to {}", user,
					exploratoryName, oldStatus, status);
			updateOne(USER_INSTANCES,
					exploratoryCondition(user, exploratoryName),
					Updates.set(STATUS, status.toString()));
		}
	}

	/**
	 * Translate the status of cluster in Amazon into computational's status.
	 *
	 * @param oldStatus the current status of computational.
	 * @param newStatus the current status of cluster in Amazon.
	 */
	private UserInstanceStatus getComputationalNewStatus(UserInstanceStatus oldStatus, String newStatus) {
		/* AWS statuses: bootstrapping, running, starting, terminated, terminated_with_errors, terminating, waiting */
		UserInstanceStatus status;
		if ("terminated".equalsIgnoreCase(newStatus) || "terminated_with_errors".equalsIgnoreCase(newStatus)) {
			status = UserInstanceStatus.TERMINATED;
		} else {
			status = Optional.ofNullable(UserInstanceStatus.of(newStatus)).orElse(oldStatus);
		}

		switch (oldStatus) {
			case CREATING:
			case CONFIGURING:
			case RUNNING:
				return (status.in(UserInstanceStatus.TERMINATED, UserInstanceStatus.TERMINATING,
						UserInstanceStatus.STOPPING, UserInstanceStatus.STOPPED) ? status : oldStatus);
			case TERMINATING:
				return (status.in(UserInstanceStatus.TERMINATED) ? status : oldStatus);
			case STARTING:
			case STOPPED:
			case STOPPING:
				return status;
			case FAILED:
			case TERMINATED:
			default:
				return oldStatus;
		}
	}

	/**
	 * Update the status of exploratory if it needed.
	 *
	 * @param user              the user name.
	 * @param exploratoryName   the name of exploratory.
	 * @param computationalName the name of computational.
	 * @param oldStatus         old status.
	 * @param newStatus         new status.
	 */
	private void updateComputationalStatus(String user, String exploratoryName, String computationalName,
										   String oldStatus, String newStatus) {
		LOGGER.trace("Update computational status for user {} with exploratory {} and computational {} from {} to {}",
				user, exploratoryName, computationalName, oldStatus, newStatus);
		UserInstanceStatus oStatus = UserInstanceStatus.of(oldStatus);
		UserInstanceStatus status = getComputationalNewStatus(oStatus, newStatus);
		LOGGER.trace("Translate computational status for user {} with exploratory {} and computational {} from {} to" +
						" " +
						"{}",
				user, exploratoryName, computationalName, newStatus, status);

		if (oStatus != status) {
			LOGGER.debug("Computational status for user {} with exploratory {} and computational {} will be updated " +
							"from {} to {}",
					user, exploratoryName, computationalName, oldStatus, status);
			if (configuration.getCloudProvider() == CloudProvider.AWS && status == UserInstanceStatus.TERMINATED &&
					terminateComputationalSpot(user, exploratoryName, computationalName)) {
				return;
			}
			Document values = new Document(COMPUTATIONAL_STATUS_FILTER, status.toString());
			updateOne(USER_INSTANCES,
					and(exploratoryCondition(user, exploratoryName),
							elemMatch(COMPUTATIONAL_RESOURCES,
									and(eq(ComputationalDAO.COMPUTATIONAL_NAME, computationalName))
							)
					),
					new Document(SET, values));
		}
	}

	/**
	 * Terminate EMR if it is spot.
	 *
	 * @param user              the user name.
	 * @param exploratoryName   the name of exploratory.
	 * @param computationalName the name of computational.
	 * @return <b>true</b> if computational is spot and should be terminated by docker, otherwise <b>false</b>.
	 */
	private boolean terminateComputationalSpot(String user, String exploratoryName, String computationalName) {
		LOGGER.trace("Check computatation is spot for user {} with exploratory {} and computational {}", user,
				exploratoryName, computationalName);
		Document doc = findOne(USER_INSTANCES,
				exploratoryCondition(user, exploratoryName),
				and(elemMatch(COMPUTATIONAL_RESOURCES,
						and(eq(ComputationalDAO.COMPUTATIONAL_NAME, computationalName),
								eq(COMPUTATIONAL_SPOT, true),
								not(eq(STATUS, TERMINATED.toString())))),
						include(COMPUTATIONAL_RESOURCES + "." + COMPUTATIONAL_SPOT))
		).orElse(null);
		if (doc == null || doc.get(COMPUTATIONAL_RESOURCES) == null) {
			return false;
		}

		EnvStatusListener envStatusListener =
				SelfServiceApplication.getInjector().getInstance(EnvStatusListener.class);
		UserInfo userInfo = (envStatusListener != null) ? envStatusListener.getSession(user) : null;
		if (userInfo == null) {
			// User logged off. Computational will be terminated when user logged in.
			return true;
		}

		String accessToken = userInfo.getAccessToken();
		LOGGER.debug("Computational will be terminated for user {} with exploratory {} and computational {}",
				user, exploratoryName, computationalName);
		try {
			// Send post request to provisioning service to terminate spot EMR.
			ComputationalResourceAws computational = new ComputationalResourceAws();
			SelfServiceApplication.getInjector().injectMembers(computational);
			UserInfo ui = new UserInfo(user, accessToken);
			computational.terminate(ui, exploratoryName, computationalName);
		} catch (Exception e) {
			// Cannot terminate EMR, just update status to terminated
			LOGGER.warn("Can't terminate computational for user {} with exploratory {} and computational {}. {}",
					user, exploratoryName, computationalName, e.getLocalizedMessage(), e);
			return false;
		}

		return true;
	}


	/**
	 * Add the resource to list if it have instance_id.
	 *
	 * @param list            the list to add.
	 * @param document        document with resource.
	 * @param statusFieldName name of field that contains status information
	 * @param resourceType
	 */
	private void addResource(List<EnvResource> list, Document document, String statusFieldName, ResourceType
			resourceType, String name) {
		LOGGER.trace("Add resource from {}", document);
		getInstanceId(document).ifPresent(instanceId -> {
			UserInstanceStatus status = UserInstanceStatus.of(document.getString(statusFieldName));
			if (status == null) {
				LOGGER.error("Unknown status {} from field {}, content is {}", document.getString(statusFieldName),
						statusFieldName, document);
				return;
			}
			switch (status) {
				case CONFIGURING:
				case CREATING:
				case RUNNING:
				case STARTING:
				case STOPPED:
				case STOPPING:
				case TERMINATING:
					EnvResource host = new EnvResource().withId(instanceId)
							.withResourceType(resourceType)
							.withName(name);
					list.add(host);
					break;
				case FAILED:
				case TERMINATED:
				default:
					break;
			}
		});
	}

	private boolean notEmpty(List<EnvResource> hostList) {
		return hostList != null && !hostList.isEmpty();
	}
}