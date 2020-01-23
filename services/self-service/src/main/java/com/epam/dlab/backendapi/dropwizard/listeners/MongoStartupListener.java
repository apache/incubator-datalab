package com.epam.dlab.backendapi.dropwizard.listeners;

import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.cloud.CloudProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;


@Slf4j
public class MongoStartupListener implements ServerLifecycleListener {

	private static final String ROLES_FILE_FORMAT = "/mongo/%s/mongo_roles.json";
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private final UserRoleDao userRoleDao;
	private final SelfServiceApplicationConfiguration configuration;
	private final SettingsDAO settingsDAO;
	private final EndpointDAO endpointDAO;

	@Inject
	public MongoStartupListener(UserRoleDao userRoleDao, SelfServiceApplicationConfiguration configuration,
								SettingsDAO settingsDAO, EndpointDAO endpointDAO) {
		this.userRoleDao = userRoleDao;
		this.configuration = configuration;
		this.settingsDAO = settingsDAO;
		this.endpointDAO = endpointDAO;
	}

	@Override
	public void serverStarted(Server server) {
		settingsDAO.setServiceBaseName(configuration.getServiceBaseName());
		settingsDAO.setConfOsFamily(configuration.getOs());
		settingsDAO.setSsnInstanceSize(configuration.getSsnInstanceSize());
		if (userRoleDao.findAll().isEmpty()) {
			log.debug("Populating DLab roles into database");
			userRoleDao.insert(getRoles());
		} else {
			log.info("Roles already populated. Do nothing ...");
		}
	}

	private List<UserRoleDto> getRoles() {
		Set<UserRoleDto> userRoles = new HashSet<>();
		endpointDAO.getEndpoints().forEach(e -> userRoles.addAll(getUserRoleFromFile(e.getCloudProvider())));
		return userRoles.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(UserRoleDto::getId))),
				ArrayList::new));
	}

	private List<UserRoleDto> getUserRoleFromFile(CloudProvider cloudProvider) {
		try (InputStream is = getClass().getResourceAsStream(format(ROLES_FILE_FORMAT, cloudProvider.getName()))) {
			return MAPPER.readValue(is, new TypeReference<List<UserRoleDto>>() {
			});
		} catch (IOException e) {
			log.error("Can not marshall dlab roles due to: {}", e.getMessage());
			throw new IllegalStateException("Can not marshall dlab roles due to: " + e.getMessage());
		}
	}
}
