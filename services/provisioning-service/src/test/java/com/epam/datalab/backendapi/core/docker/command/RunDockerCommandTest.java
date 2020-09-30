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

package com.epam.datalab.backendapi.core.docker.command;

import com.epam.datalab.backendapi.core.commands.RunDockerCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RunDockerCommandTest {

    String DOCKER_BASE = "docker run -v %s:/root/keys -v %s:/response -e \"request_id=%s\" ";

    String GET_IMAGE_METADATA = DOCKER_BASE + "%s --action describe";

    String RUN_IMAGE = DOCKER_BASE + "-e \"dry_run=true\" %s --action run";

    String CREATE_EDGE_METADATA = DOCKER_BASE +
            "-e \"conf_service_base_name=%s\" " +
            "-e \"conf_key_name=%s\" " +
            "-e \"edge_user_name=%s\" " +
            "%s --action create";

    String CREATE_EMR_CLUSTER = DOCKER_BASE +
            "-e \"conf_service_base_name=%s\" " +
            "-e \"emr_instance_count=%s\" " +
            "-e \"emr_instance_type=%s\" " +
            "-e \"emr_version=%s\" " +
            "-e \"ec2_role=%s\" " +
            "-e \"service_role=%s\" " +
            "-e \"notebook_name=%s\" " +
            "-e \"edge_user_name=%s\" " +
            "-e \"edge_subnet_cidr=%s\" " +
            "-e \"aws_region=%s\" " +
            "-e \"conf_key_name=%s\" " +
            "%s --action create";

    String TERMINATE_EMR_CLUSTER = DOCKER_BASE +
            "-e \"conf_service_base_name=%s\" " +
            "-e \"edge_user_name=%s\" " +
            "-e \"emr_cluster_name=%s\" " +
            "-e \"aws_region=%s\" " +
            "-e \"conf_key_name=%s\" " +
            "%s --action terminate";

    String EXPLORATORY_ENVIRONMENT = DOCKER_BASE +
            "-e \"conf_service_base_name=%s\" " +
            "-e \"aws_region=%s\" " +
            "-e \"conf_key_name=%s\" ";

    String CREATE_EXPLORATORY_ENVIRONMENT = EXPLORATORY_ENVIRONMENT +
            "-e \"edge_user_name=%s\" " +
            "-e \"notebook_subnet_cidr=%s\" " +
            "-e \"aws_security_groups_ids=%s\" " +
            "%s --action create";

    String TERMINATE_EXPLORATORY_ENVIRONMENT = EXPLORATORY_ENVIRONMENT +
            "-e \"edge_user_name=%s\" " +
            "-e \"notebook_instance_name=%s\" " +
            "%s --action terminate";

    String STOP_EXPLORATORY_ENVIRONMENT = EXPLORATORY_ENVIRONMENT +
            "-e \"edge_user_name=%s\" " +
            "-e \"notebook_instance_name=%s\" " +
            "%s --action stop";

    String rootKeysVolume = "rkv";
    String responseVolume = "rv";
    String requestID = "rID";
    String toDescribe = "tds";
    String credsKeyName = "ckn";
    String confServiceBaseName = "csbn";
    String edgeUserName = "eun";
    String userKeyName = "ukn";
    String toCreate = "tc";
    String toTerminate = "tt";
    String toStop = "ts";
    String toRun = "tr";
    String emrInstanceCount = "10";
    String emrInstanceType = "emit";
    String emrVersion = "ev";
    String ec2Role = "e2r";
    String serviceRole = "sr";
    String notebookName = "nn";
    String edgeSubnetCidr = "esc";
    String credsRegion = "cr";
    String emrClusterName = "ecn";
    String notebookUserName = "nun";
    String notebookSubnetCidr = "nsc";
    String credsSecurityGroupsIds = "csgi";
    String notebookInstanceName = "nin";

    @Test
    public void testBuildDockerBaseCommand() {
        RunDockerCommand dockerBaseCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID);
        assertEquals(String.format(DOCKER_BASE, rootKeysVolume, responseVolume, requestID), dockerBaseCommand.toCMD() + " ");
    }

    @Test
    public void testBuildGetImageMetadataCommand() {
        RunDockerCommand getImageMetadataCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withActionDescribe(toDescribe);
        assertEquals(String.format(GET_IMAGE_METADATA, rootKeysVolume, responseVolume, requestID, toDescribe), getImageMetadataCommand.toCMD());
    }

    @Test
    public void testBuildCreateEdgeMetadataCommand() {
        RunDockerCommand createEdgeMetadataCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withConfServiceBaseName(confServiceBaseName)
                .withConfKeyName(credsKeyName)
                .withEdgeUserName(edgeUserName)
                .withActionCreate(toCreate);
        assertEquals(String.format(CREATE_EDGE_METADATA, rootKeysVolume, responseVolume,
                requestID, confServiceBaseName, credsKeyName, edgeUserName, toCreate),
                createEdgeMetadataCommand.toCMD());
    }

    @Test
    public void testBuildRunImageCommand() {
        RunDockerCommand runImageCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withDryRun()
                .withActionRun(toRun);
        assertEquals(String.format(RUN_IMAGE, rootKeysVolume, responseVolume, requestID, toRun), runImageCommand.toCMD());
    }

    @Test
    public void testCreateEMRClusterCommand() {
        RunDockerCommand createEMRClusterCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withConfServiceBaseName(confServiceBaseName)
                .withEmrInstanceCount(emrInstanceCount)
                .withEmrInstanceType(emrInstanceType)
                .withEmrVersion(emrVersion)
                .withEc2Role(ec2Role)
                .withServiceRole(serviceRole)
                .withNotebookName(notebookName)
                .withEdgeUserName(edgeUserName)
                .withEdgeSubnetCidr(edgeSubnetCidr)
                .withAwsRegion(credsRegion)
                .withConfKeyName(credsKeyName)
                .withActionCreate(toCreate);

        assertEquals(
                String.format(
                        CREATE_EMR_CLUSTER,
                        rootKeysVolume,
                        responseVolume,
                        requestID,
                        confServiceBaseName,
                        emrInstanceCount,
                        emrInstanceType,
                        emrVersion,
                        ec2Role,
                        serviceRole,
                        notebookName,
                        edgeUserName,
                        edgeSubnetCidr,
                        credsRegion,
                        credsKeyName,
                        toCreate
                ),
                createEMRClusterCommand.toCMD()
        );
    }

    @Test
    public void testTerminateEmrCluster() {
        RunDockerCommand terminateEMRClusterCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withConfServiceBaseName(confServiceBaseName)
                .withEdgeUserName(edgeUserName)
                .withEmrClusterName(emrClusterName)
                .withAwsRegion(credsRegion)
                .withConfKeyName(credsKeyName)
                .withActionTerminate(toTerminate);

        assertEquals(
                String.format(
                        TERMINATE_EMR_CLUSTER,
                        rootKeysVolume,
                        responseVolume,
                        requestID,
                        confServiceBaseName,
                        edgeUserName,
                        emrClusterName,
                        credsRegion,
                        credsKeyName,
                        toTerminate
                ),
                terminateEMRClusterCommand.toCMD()
        );
    }

    @Test
    public void testCreateExploratoryEnvironment() {
        RunDockerCommand createExploratoryEnvironmentCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withConfServiceBaseName(confServiceBaseName)
                .withAwsRegion(credsRegion)
                .withConfKeyName(credsKeyName)
                .withNotebookUserName(notebookUserName)
                .withNotebookSubnetCidr(notebookSubnetCidr)
                .withAwsSecurityGroupsIds(credsSecurityGroupsIds)
                .withActionCreate(toCreate);

        assertEquals(
                String.format(
                        CREATE_EXPLORATORY_ENVIRONMENT,
                        rootKeysVolume,
                        responseVolume,
                        requestID,
                        confServiceBaseName,
                        credsRegion,
                        credsKeyName,
                        notebookUserName,
                        notebookSubnetCidr,
                        credsSecurityGroupsIds,
                        toCreate
                ),
                createExploratoryEnvironmentCommand.toCMD()
        );
    }

    @Test
    public void testTerminateExploratoryEnvironment() {
        RunDockerCommand terminateExploratoryEnvironmentCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withConfServiceBaseName(confServiceBaseName)
                .withAwsRegion(credsRegion)
                .withConfKeyName(credsKeyName)
                .withNotebookUserName(notebookUserName)
                .withNotebookInstanceName(notebookInstanceName)
                .withActionTerminate(toTerminate);

        assertEquals(
                String.format(
                        TERMINATE_EXPLORATORY_ENVIRONMENT,
                        rootKeysVolume,
                        responseVolume,
                        requestID,
                        confServiceBaseName,
                        credsRegion,
                        credsKeyName,
                        notebookUserName,
                        notebookInstanceName,
                        toTerminate
                ),
                terminateExploratoryEnvironmentCommand.toCMD()
        );
    }

    @Test
    public void testStopExploratoryEnvironment() {
        RunDockerCommand stopExploratoryEnvironmentCommand = new RunDockerCommand()
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withConfServiceBaseName(confServiceBaseName)
                .withAwsRegion(credsRegion)
                .withConfKeyName(credsKeyName)
                .withNotebookUserName(notebookUserName)
                .withNotebookInstanceName(notebookInstanceName)
                .withActionStop(toStop);

        assertEquals(
                String.format(
                        STOP_EXPLORATORY_ENVIRONMENT,
                        rootKeysVolume,
                        responseVolume,
                        requestID,
                        confServiceBaseName,
                        credsRegion,
                        credsKeyName,
                        notebookUserName,
                        notebookInstanceName,
                        toStop
                ),
                stopExploratoryEnvironmentCommand.toCMD()
        );
    }

}
