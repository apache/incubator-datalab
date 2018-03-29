package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.dto.computational.UserComputationalResource;

public interface ComputationalService {
	/**
	 * Asynchronously triggers creation of Spark cluster
	 *
	 * @param userInfo user authentication info
	 * @param form     input cluster parameters
	 * @return <code>true</code> if action is successfully triggered, <code>false</code>false if cluster with the same
	 * name already exists
	 * @throws IllegalArgumentException if input parameters exceed limits or docker image name is malformed
	 */
	boolean createSparkCluster(UserInfo userInfo, SparkStandaloneClusterCreateForm form);

	/**
	 * Asynchronously triggers termination of computational resources
	 *
	 * @param userInfo          user info of authenticated user
	 * @param exploratoryName   name of exploratory where to terminate computational resources with
	 *                          <code>computationalName</code>
	 * @param computationalName computational name
	 */
	void terminateComputationalEnvironment(UserInfo userInfo, String exploratoryName, String computationalName);

	boolean createDataEngineService(UserInfo userInfo, ComputationalCreateFormDTO formDTO, UserComputationalResource
			computationalResource);

	void stopSparkCluster(UserInfo userInfo, String exploratoryName, String computationalName);
	void startSparkCluster(UserInfo userInfo, String exploratoryName, String computationalName);
}
