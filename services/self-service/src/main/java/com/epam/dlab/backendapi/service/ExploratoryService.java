package com.epam.dlab.backendapi.service;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.model.exloratory.Exploratory;

public interface ExploratoryService {

	String start(UserInfo userInfo, String exploratoryName);

	String stop(UserInfo userInfo, String exploratoryName);

	String terminate(UserInfo userInfo, String exploratoryName);

	String create(UserInfo userInfo, Exploratory exploratory);

	void updateExploratoryStatuses(String user, UserInstanceStatus status);

	void setReuploadKeyRequiredForCorrespondingExploratoriesAndComputationals(String user);

	void cancelReuploadKeyRequirementForAllUserInstances(String user);
}
