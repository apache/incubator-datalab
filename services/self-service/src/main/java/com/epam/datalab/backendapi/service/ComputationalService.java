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

package com.epam.datalab.backendapi.service;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.datalab.backendapi.resources.dto.ComputationalTemplatesDTO;
import com.epam.datalab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.computational.UserComputationalResource;

import java.util.List;
import java.util.Optional;

public interface ComputationalService {
    ComputationalTemplatesDTO getComputationalNamesAndTemplates(UserInfo user, String project, String endpoint);

    /**
     * Asynchronously triggers creation of Spark cluster
     *
     * @param userInfo     user authentication info
     * @param resourceName name of computational resource
     * @param form         input cluster parameters
     * @param auditInfo    additional info for audit
     * @return <code>true</code> if action is successfully triggered, <code>false</code>false if cluster with the same
     * name already exists
     * @throws IllegalArgumentException if input parameters exceed limits or docker image name is malformed
     */
    boolean createSparkCluster(UserInfo userInfo, String resourceName, SparkStandaloneClusterCreateForm form, String project, String auditInfo);

    /**
     * Asynchronously triggers termination of computational resources
     *
     * @param userInfo          user info of authenticated user
     * @param resourceCreator   username of resource creator
     * @param project           project name
     * @param exploratoryName   name of exploratory where to terminate computational resources with <code>computationalName</code>
     * @param computationalName computational name
     * @param auditInfo         additional info for audit
     */
    void terminateComputational(UserInfo userInfo, String resourceCreator, String project, String exploratoryName, String computationalName, String auditInfo);

    boolean createDataEngineService(UserInfo userInfo, String resourceName, ComputationalCreateFormDTO formDTO, UserComputationalResource
            computationalResource, String project, String auditInfo);

    void stopSparkCluster(UserInfo userInfo, String resourceCreator, String project, String exploratoryName, String computationalName, String auditInfo);

    void startSparkCluster(UserInfo userInfo, String exploratoryName, String computationalName, String project, String auditInfo);

    void updateSparkClusterConfig(UserInfo userInfo, String project, String exploratoryName, String computationalName,
                                  List<ClusterConfig> config, String auditInfo);

    Optional<UserComputationalResource> getComputationalResource(String user, String project, String exploratoryName,
                                                                 String computationalName);

    List<ClusterConfig> getClusterConfig(UserInfo userInfo, String project, String exploratoryName, String computationalName);

    void updateAfterStatusCheck(UserInfo systemUser, String project, String endpoint, String name, String instanceID, UserInstanceStatus status, String auditInfo);
}
