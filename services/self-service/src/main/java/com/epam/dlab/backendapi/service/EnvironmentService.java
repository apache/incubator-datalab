package com.epam.dlab.backendapi.service;

import java.util.Set;

public interface EnvironmentService {

	Set<String> getActiveUsers();

	void stopEnvironment(String user);

	void terminateEnvironment(String user);
}
