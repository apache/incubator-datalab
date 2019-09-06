package com.epam.dlab.backendapi.dropwizard.listeners;

import com.epam.dlab.backendapi.conf.CloudConfiguration;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
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
import java.util.List;

import static java.lang.String.format;


@Slf4j
public class MongoStartupListener implements ServerLifecycleListener {

	private static final String ROLES_FILE_FORMAT = "/mongo/%s/mongo_roles.json";
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private final UserRoleDao userRoleDao;
	private final SettingsDAO settingsDAO;
	private final SelfServiceApplicationConfiguration configuration;

	@Inject
	public MongoStartupListener(UserRoleDao userRoleDao, SettingsDAO settingsDAO,
								SelfServiceApplicationConfiguration configuration) {
		this.userRoleDao = userRoleDao;
		this.settingsDAO = settingsDAO;
		this.configuration = configuration;
	}

	@Override
	public void serverStarted(Server server) {
		insertRoles();
		insertCloudSettings();
	}

	private void insertCloudSettings() {
		log.debug("Populating DLab cloud properties into database");
		final CloudConfiguration cloudConfiguration = configuration.getCloudConfiguration();
		settingsDAO.setServiceBaseName(cloudConfiguration.getServiceBaseName());
		settingsDAO.setConfKeyDir(cloudConfiguration.getConfKeyDir());
		settingsDAO.setConfOsFamily(cloudConfiguration.getOs());
		settingsDAO.setConfTagResourceId(cloudConfiguration.getConfTagResourceId());
		final CloudConfiguration.LdapConfig ldapConfig = cloudConfiguration.getLdapConfig();
		settingsDAO.setLdapDn(ldapConfig.getDn());
		settingsDAO.setLdapHost(ldapConfig.getHost());
		settingsDAO.setLdapOu(ldapConfig.getOu());
		settingsDAO.setLdapUser(ldapConfig.getUser());
		settingsDAO.setLdapPassword(ldapConfig.getPassword());
		settingsDAO.setSsnStorageAccountTagName(cloudConfiguration.getSsnStorageAccountTagName());
		settingsDAO.setPeeringId(cloudConfiguration.getPeeringId());

		if (configuration.getCloudProvider() == CloudProvider.AWS) {
			settingsDAO.setAwsZone(cloudConfiguration.getZone());
			settingsDAO.setAwsRegion(cloudConfiguration.getRegion());
			settingsDAO.setAwsVpcId(cloudConfiguration.getVpcId());
			settingsDAO.setAwsSubnetId(cloudConfiguration.getSubnetId());
			settingsDAO.setAwsNotebookVpcId(cloudConfiguration.getNotebookVpcId());
			settingsDAO.setAwsNotebookSubnetId(cloudConfiguration.getNotebookSubnetId());
			settingsDAO.setAwsSecurityGroups(cloudConfiguration.getSecurityGroupIds());
		}

		if (configuration.getCloudProvider() == CloudProvider.AZURE) {
			settingsDAO.setAzureRegion(cloudConfiguration.getRegion());
			settingsDAO.setAzureVpcName(cloudConfiguration.getVpcId());
			settingsDAO.setAzureSubnetName(cloudConfiguration.getSubnetId());
			settingsDAO.setAzureDataLakeClientId(cloudConfiguration.getAzureClientId());
			settingsDAO.setAzureResourceGroupName(cloudConfiguration.getAzureResourceGroupName());
			settingsDAO.setAzureSecurityGroupName(cloudConfiguration.getSecurityGroupIds());
			settingsDAO.setAzureDataLakeNameTag(cloudConfiguration.getDatalakeTagName());
			settingsDAO.setSsnStorageAccountTagName(cloudConfiguration.getSsnStorageAccountTagName());
			settingsDAO.setSharedStorageAccountTagName(cloudConfiguration.getSharedStorageAccountTagName());

		}
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
