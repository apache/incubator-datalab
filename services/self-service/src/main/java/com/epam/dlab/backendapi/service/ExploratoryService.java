package com.epam.dlab.backendapi.service;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.model.exloratory.Exploratory;

import java.util.List;

public interface ExploratoryService {

	String start(UserInfo userInfo, String exploratoryName);

	String stop(UserInfo userInfo, String exploratoryName);

	String terminate(UserInfo userInfo, String exploratoryName);

	String create(UserInfo userInfo, Exploratory exploratory);

	void updateExploratoryStatuses(String user, UserInstanceStatus status);

	void updateExploratoriesReuploadKeyFlag(String user, boolean reuploadKeyRequired,
											UserInstanceStatus... exploratoryStatuses);

	List<String> getResourcesForKeyReuploading(String user, String serviceBaseName,
											   UserInstanceStatus exploratoryStatus,
											   UserInstanceStatus computationalStatus);
}
