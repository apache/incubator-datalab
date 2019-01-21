package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;

import java.time.LocalDateTime;

public interface InactivityService {

	void updateRunningResourcesLastActivity(UserInfo userInfo);

	void updateLastActivityForExploratory(UserInfo userInfo, String exploratoryName, LocalDateTime lastActivity);

	void updateLastActivityForComputational(UserInfo userInfo, String exploratoryName,
											String computationalName, LocalDateTime lastActivity);
}
