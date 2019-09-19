package com.epam.dlab.backendapi.dropwizard.listeners;

import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.lang.String.format;


@Slf4j
public class MongoStartupListener implements ServerLifecycleListener {

	private static final String ROLES_FILE_FORMAT = "/mongo/%s/mongo_roles.json";
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private final UserRoleDao userRoleDao;
	private final SelfServiceApplicationConfiguration configuration;
	private final SettingsDAO settingsDAO;

	@Inject
	public MongoStartupListener(UserRoleDao userRoleDao,
								SelfServiceApplicationConfiguration configuration, SettingsDAO settingsDAO) {
		this.userRoleDao = userRoleDao;
		this.configuration = configuration;
		this.settingsDAO = settingsDAO;
	}

	@Override
	public void serverStarted(Server server) {
		insertRoles();
		settingsDAO.setServiceBaseName(configuration.getServiceBaseName());
	}

	private void insertRoles() {
		log.debug("Populating DLab roles into database");
		userRoleDao.removeAll();
		userRoleDao.insert(getRoles());
	}

	private List<UserRoleDto> getRoles() {
		try (InputStream is = getClass().getResourceAsStream(format(ROLES_FILE_FORMAT,
				configuration.getCloudProvider().getName()))) {
			return MAPPER.readValue(is, new TypeReference<List<UserRoleDto>>() {
			});
		} catch (IOException e) {
			log.error("Can not marshall dlab roles due to: {}", e.getMessage());
			throw new IllegalStateException("Can not marshall dlab roles due to: " + e.getMessage());
		}
	}
}
