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

package com.epam.datalab.backendapi.core.commands;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class RunDockerCommand implements CmdCommand {
    public static final String EDGE_USER_NAME_FORMAT = "-e \"edge_user_name=%s\"";
    private final String command = "docker run";
    private final List<String> options = new LinkedList<>();
    private String image;
    private DockerAction action;

    private static final String ROOT_KEYS_PATH = "/root/keys";
    private static final String RESPONSE_PATH = "/response";
    private static final String LOG_PATH = "/logs";
    private static final String AZURE_AUTH_FILE = "/root/azure_auth.json";

    public RunDockerCommand withVolume(String hostSrcPath, String bindPath) {
        options.add(String.format("-v %s:%s", hostSrcPath, bindPath));
        return this;
    }

    public RunDockerCommand withVolumeForRootKeys(String hostSrcPath) {
        return withVolume(hostSrcPath, ROOT_KEYS_PATH);
    }

    public RunDockerCommand withVolumeForResponse(String hostSrcPath) {
        return withVolume(hostSrcPath, RESPONSE_PATH);
    }

    public RunDockerCommand withVolumeFoAzureAuthFile(String hostSrcPath) {
        return withVolume(hostSrcPath, AZURE_AUTH_FILE);
    }

    public RunDockerCommand withVolumeForLog(String hostSrcPath, String logDirectory) {
        return withVolume(Paths.get(hostSrcPath, logDirectory).toString(),
                Paths.get(LOG_PATH, logDirectory).toString());
    }

    public RunDockerCommand withName(String name) {
        options.add(String.format("--name %s", name));
        return this;
    }

    public RunDockerCommand withRequestId(String requestId) {
        options.add(String.format("-e \"request_id=%s\"", requestId));
        return this;
    }

    public RunDockerCommand withAtach(String value) {
        options.add(String.format("-a %s", value));
        return this;
    }

    public RunDockerCommand withInteractive() {
        options.add("-i");
        return this;
    }

    public RunDockerCommand withDetached() {
        options.add("-d");
        return this;
    }

    public RunDockerCommand withPseudoTTY() {
        options.add("-t");
        return this;
    }

    public RunDockerCommand withImage(String image) {
        this.image = image;
        return this;
    }

    public RunDockerCommand withAction(DockerAction action) {
        this.action = action;
        return this;
    }

    public RunDockerCommand withActionDescribe(String toDescribe) {
        this.image = toDescribe;
        this.action = DockerAction.DESCRIBE;
        return this;
    }

    public RunDockerCommand withActionCreate(String toCreate) {
        this.image = toCreate;
        this.action = DockerAction.CREATE;
        return this;
    }

    public RunDockerCommand withActionConfigure(String toConfigue) {
        this.image = toConfigue;
        this.action = DockerAction.CONFIGURE;
        return this;
    }

    public RunDockerCommand withActionStatus(String toStatus) {
        this.options.add("--label edge_status");
        this.image = toStatus;
        this.action = DockerAction.STATUS;
        return this;
    }

    public RunDockerCommand withActionStart(String toStart) {
        this.image = toStart;
        this.action = DockerAction.START;
        return this;
    }

    public RunDockerCommand withActionRun(String toRun) {
        this.image = toRun;
        this.action = DockerAction.RUN;
        return this;
    }

    public RunDockerCommand withActionTerminate(String toTerminate) {
        this.image = toTerminate;
        this.action = DockerAction.TERMINATE;
        return this;
    }

    public RunDockerCommand withActionStop(String toStop) {
        this.image = toStop;
        this.action = DockerAction.STOP;
        return this;
    }

    public RunDockerCommand withConfKeyName(String confKeyName) {
        options.add(String.format("-e \"conf_key_name=%s\"", confKeyName));
        return this;
    }

    public RunDockerCommand withConfServiceBaseName(String confServiceBaseName) {
        options.add(String.format("-e \"conf_service_base_name=%s\"", confServiceBaseName));
        return this;
    }

    public RunDockerCommand withConfOsFamily(String confOsFamily) {
        options.add(String.format("-e \"conf_os_family=%s\"", confOsFamily));
        return this;
    }

    public RunDockerCommand withEmrInstanceCount(String emrInstanceCount) {
        options.add(String.format("-e \"emr_instance_count=%s\"", emrInstanceCount));
        return this;
    }

    public RunDockerCommand withAwsVpcId(String awsVpcId) {
        options.add(String.format("-e \"aws_vpc_id=%s\"", awsVpcId));
        return this;
    }

    public RunDockerCommand withAwsSubnetId(String awsSubnetId) {
        options.add(String.format("-e \"aws_subnet_id=%s\"", awsSubnetId));
        return this;
    }

    public RunDockerCommand withEmrInstanceType(String emrInstanceType) {
        options.add(String.format("-e \"emr_instance_type=%s\"", emrInstanceType));
        return this;
    }

    public RunDockerCommand withEmrVersion(String emrVersion) {
        options.add(String.format("-e \"emr_version=%s\"", emrVersion));
        return this;
    }

    public RunDockerCommand withEmrTimeout(String emrTimeout) {
        options.add(String.format("-e \"emr_timeout=%s\"", emrTimeout));
        return this;
    }

    public RunDockerCommand withEc2Role(String ec2Role) {
        options.add(String.format("-e \"ec2_role=%s\"", ec2Role));
        return this;
    }

    public RunDockerCommand withServiceRole(String serviceRole) {
        options.add(String.format("-e \"service_role=%s\"", serviceRole));
        return this;
    }

    public RunDockerCommand withNotebookName(String notebookName) {
        options.add(String.format("-e \"notebook_name=%s\"", notebookName));
        return this;
    }

    public RunDockerCommand withEdgeSubnetCidr(String edgeSubnetCidr) {
        options.add(String.format("-e \"edge_subnet_cidr=%s\"", edgeSubnetCidr));
        return this;
    }

    public RunDockerCommand withAwsRegion(String awsRegion) {
        options.add(String.format("-e \"aws_region=%s\"", awsRegion));
        return this;
    }

    public RunDockerCommand withEdgeUserName(String edgeUserName) {
        options.add(String.format(EDGE_USER_NAME_FORMAT, edgeUserName));
        return this;
    }

    public RunDockerCommand withEmrClusterName(String emrClusterName) {
        options.add(String.format("-e \"emr_cluster_name=%s\"", emrClusterName));
        return this;
    }

    public RunDockerCommand withNotebookUserName(String notebookUserName) {
        options.add(String.format(EDGE_USER_NAME_FORMAT, notebookUserName));
        return this;
    }

    public RunDockerCommand withNotebookSubnetCidr(String notebookSubnetCidr) {
        options.add(String.format("-e \"notebook_subnet_cidr=%s\"", notebookSubnetCidr));
        return this;
    }

    public RunDockerCommand withAwsSecurityGroupsIds(String awsSecurityGroupsIds) {
        options.add(String.format("-e \"aws_security_groups_ids=%s\"", awsSecurityGroupsIds));
        return this;
    }

    public RunDockerCommand withNotebookInstanceName(String notebookInstanceName) {
        options.add(String.format("-e \"notebook_instance_name=%s\"", notebookInstanceName));
        return this;
    }

    public RunDockerCommand withUserKeyName(String userKeyName) {
        options.add(String.format(EDGE_USER_NAME_FORMAT, userKeyName));
        return this;
    }

    public RunDockerCommand withDryRun() {
        options.add("-e \"dry_run=true\"");
        return this;
    }

    public RunDockerCommand withResource(String resourceType) {
        options.add(String.format("-e \"conf_resource=%s\"", resourceType));
        return this;
    }

    public RunDockerCommand withRemove() {
        options.add("--rm");
        return this;
    }

    @Override
    public String toCMD() {
        StringBuilder sb = new StringBuilder(command);
        for (String option : options) {
            sb.append(" ").append(option);
        }
        if (image != null && action != null) {
            sb.append(" ").append(image).append(" --action ").append(action.toString());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toCMD();
    }

}