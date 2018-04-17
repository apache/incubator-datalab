package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.InfrastructureInfo;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.Map;

@Slf4j
public abstract class InfrastructureInfoServiceBase<T> implements InfrastructureInfoService {

	@Inject
	private ExploratoryDAO expDAO;
	@Inject
	private KeyDAO keyDAO;
	@Inject
	private EnvStatusDAO envDAO;
	@Inject
	private SelfServiceApplicationConfiguration configuration;


	@SuppressWarnings("unchecked")
	private Map<String, String> getSharedInfo(EdgeInfo edgeInfo) {
		return getSharedInfo((T) edgeInfo);
	}

	@Override
	public InfrastructureInfo getUserResources(String user) {
		log.debug("Loading list of provisioned resources for user {}", user);
		try {
			Iterable<Document> documents = expDAO.findExploratory(user);
			EdgeInfo edgeInfo = keyDAO.getEdgeInfo(user);
			return new InfrastructureInfo(getSharedInfo(edgeInfo), documents);
		} catch (Exception e) {
			log.error("Could not load list of provisioned resources for user: {}", user, e);
			throw new DlabException("Could not load list of provisioned resources for user: ");
		}
	}

	@Override
	public HealthStatusPageDTO getHeathStatus(String user, boolean fullReport, boolean isAdmin) {
		log.debug("Request the status of resources for user {}, report type {}", user, fullReport);
		try {
			return envDAO.getHealthStatusPageDTO(user, fullReport)
					.withBillingEnabled(configuration.isBillingSchedulerEnabled())
					.withAdmin(isAdmin);
		} catch (Exception e) {
			log.warn("Could not return status of resources for user {}: {}", user, e.getLocalizedMessage(), e);
			throw new DlabException(e.getMessage(), e);
		}
	}

	protected abstract Map<String, String> getSharedInfo(T sharedInfo);
}
