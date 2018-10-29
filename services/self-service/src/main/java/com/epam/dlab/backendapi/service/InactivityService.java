package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.status.EnvResource;

import java.util.List;

public interface InactivityService {

	void updateRunningResourcesLastActivity(UserInfo userInfo);

	void stopClustersByInactivity(List<String> computationalIds);

	void updateLastActivityForClusters(List<EnvResource> clusters);


	void stopByInactivity(List<EnvResource> exploratories);

	void updateLastActivity(List<EnvResource> exploratories);
}
