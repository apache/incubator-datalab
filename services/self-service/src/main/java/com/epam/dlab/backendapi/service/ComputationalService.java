/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.ComputationalTemplatesDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.dto.computational.UserComputationalResource;

import java.util.List;
import java.util.Optional;

public interface ComputationalService {
	ComputationalTemplatesDTO getComputationalNamesAndTemplates(UserInfo user, String project, String endpoint);

	/**
	 * Asynchronously triggers creation of Spark cluster
	 *
	 * @param userInfo user authentication info
	 * @param form     input cluster parameters
	 * @return <code>true</code> if action is successfully triggered, <code>false</code>false if cluster with the same
	 * name already exists
	 * @throws IllegalArgumentException if input parameters exceed limits or docker image name is malformed
	 */
	boolean createSparkCluster(UserInfo userInfo, SparkStandaloneClusterCreateForm form, String project);

	/**
	 * Asynchronously triggers termination of computational resources
	 *
	 * @param userInfo          user info of authenticated user
	 * @param project           project name
	 * @param exploratoryName   name of exploratory where to terminate computational resources with
	 *                          <code>computationalName</code>
	 * @param computationalName computational name
	 */
	void terminateComputational(UserInfo userInfo, String project, String exploratoryName, String computationalName);

	boolean createDataEngineService(UserInfo userInfo, ComputationalCreateFormDTO formDTO, UserComputationalResource
			computationalResource, String project);

	void stopSparkCluster(UserInfo userInfo, String project, String exploratoryName, String computationalName);

	void startSparkCluster(UserInfo userInfo, String exploratoryName, String computationalName, String project);

	void updateSparkClusterConfig(UserInfo userInfo, String project, String exploratoryName, String computationalName,
								  List<ClusterConfig> config);

	Optional<UserComputationalResource> getComputationalResource(String user, String project, String exploratoryName,
																 String computationalName);

	List<ClusterConfig> getClusterConfig(UserInfo userInfo, String project, String exploratoryName, String computationalName);
}
