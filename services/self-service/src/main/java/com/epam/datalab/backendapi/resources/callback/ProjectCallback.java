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

package com.epam.datalab.backendapi.resources.callback;

import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.dao.GpuDAO;
import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.project.ProjectResult;
import com.epam.datalab.dto.imagemetadata.EdgeGPU;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;

@Path("/project/status")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ProjectCallback {

    private final ProjectDAO projectDAO;
    private final ExploratoryService exploratoryService;
    private final RequestId requestId;
    private final GpuDAO gpuDAO;

    @Inject
    public ProjectCallback(ProjectDAO projectDAO, EndpointDAO endpointDAO, ExploratoryService exploratoryService, RequestId requestId,
                           GpuDAO gpuDAO) {
        this.projectDAO = projectDAO;
        this.exploratoryService = exploratoryService;
        this.requestId = requestId;
        this.gpuDAO = gpuDAO;
    }


    @POST
    public Response updateProjectStatus(ProjectResult projectResult) {
        requestId.checkAndRemove(projectResult.getRequestId());
        final String projectName = projectResult.getProjectName();
        final UserInstanceStatus status = UserInstanceStatus.of(projectResult.getStatus());
        if (projectResult.getEdgeInfo() != null) {
            saveGpuForProject(projectResult, projectName);
        }
        if (UserInstanceStatus.RUNNING == status && Objects.nonNull(projectResult.getEdgeInfo())) {
            projectDAO.updateEdgeInfo(projectName, projectResult.getEndpointName(), projectResult.getEdgeInfo());
        } else {
            updateExploratoriesStatusIfNeeded(status, projectResult.getProjectName(), projectResult.getEndpointName());
            projectDAO.updateEdgeStatus(projectName, projectResult.getEndpointName(), status);
        }
        return Response.ok().build();
    }

    private void saveGpuForProject(ProjectResult projectResult, String projectName) {
        try {
            if (projectResult.getEdgeInfo().getGpuList() != null
                    && !gpuDAO.getGPUByProjectName(projectName).isPresent()) {
                List<String> gpuList = projectResult.getEdgeInfo().getGpuList();
                log.info("Adding edgeGpu with gpu_types: {}, for project: {}", gpuList, projectName);
                gpuDAO.create(new EdgeGPU(projectName, gpuList));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DatalabException(e.getMessage(), e);
        }
    }

    private void updateExploratoriesStatusIfNeeded(UserInstanceStatus status, String projectName, String endpoint) {
        if (UserInstanceStatus.TERMINATED == status) {
            exploratoryService.updateProjectExploratoryStatuses(projectName, endpoint, UserInstanceStatus.TERMINATED);
        }
    }
}
