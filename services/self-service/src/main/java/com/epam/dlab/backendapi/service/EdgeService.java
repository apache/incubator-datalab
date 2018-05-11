package com.epam.dlab.backendapi.service;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;

public interface EdgeService {
	String start(UserInfo userInfo);

	String stop(UserInfo userInfo);

	String terminate(UserInfo userInfo);

	void updateReuploadKeyFlag(String user, boolean reuploadKeyRequired, UserInstanceStatus... edgeStatuses);
}
