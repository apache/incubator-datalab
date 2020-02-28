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

package com.epam.dlab.backendapi.util;

import com.epam.dlab.backendapi.domain.BillingReportDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.billing.BillingResourceType;
import com.epam.dlab.dto.computational.UserComputationalResource;

import java.util.stream.Stream;

public class BillingUtils {

    private static final String EDGE_FORMAT = "%s-%s-%s-edge";
    private static final String EDGE_VOLUME_FORMAT = "%s-%s-%s-edge-volume-primary";
    private static final String EDGE_BUCKET_FORMAT = "%s-%s-bucket";
    private static final String VOLUME_PRIMARY_FORMAT = "%s-volume-primary";
    private static final String VOLUME_SECONDARY_FORMAT = "%s-volume-secondary";
    private static final String VOLUME_PRIMARY = "Volume primary";
    private static final String VOLUME_SECONDARY = "Volume secondary";
    private static final String SHARED_RESOURCE = "Shared resource";

    public static Stream<BillingReportDTO> edgeBillingDataStream(String project, String sbn, String endpoint) {
        final String userEdgeId = String.format(EDGE_FORMAT, sbn, project.toLowerCase(), endpoint);
        final String edgeVolumeId = String.format(EDGE_VOLUME_FORMAT, sbn, project.toLowerCase(), endpoint);
        final String edgeBucketId = String.format(EDGE_BUCKET_FORMAT, sbn, project.toLowerCase());
        return Stream.of(
                BillingReportDTO.builder().resourceName("EDGE node").user(SHARED_RESOURCE).project(project).dlabId(userEdgeId).resourceType(BillingResourceType.EDGE).build(),
                BillingReportDTO.builder().resourceName("EDGE volume").user(SHARED_RESOURCE).project(project).dlabId(edgeVolumeId).resourceType(BillingResourceType.VOLUME).build(),
                BillingReportDTO.builder().resourceName("EDGE bucket").user(SHARED_RESOURCE).project(project).dlabId(edgeBucketId).resourceType(BillingResourceType.EDGE_BUCKET).build()
        );
    }

    public static Stream<BillingReportDTO> ssnBillingDataStream(String sbn) {
        final String ssnId = sbn + "-ssn";
        final String bucketName = sbn.replaceAll("_", "-");
        return Stream.of(
                BillingReportDTO.builder().user(SHARED_RESOURCE).resourceName("SSN").dlabId(ssnId).resourceType(BillingResourceType.SSN).build(),
                BillingReportDTO.builder().user(SHARED_RESOURCE).resourceName("SSN Volume").dlabId(String.format(VOLUME_PRIMARY_FORMAT, ssnId)).resourceType(BillingResourceType.VOLUME).build(),
                BillingReportDTO.builder().user(SHARED_RESOURCE).resourceName("SSN bucket").dlabId(bucketName + "-ssn" + "-bucket").resourceType(BillingResourceType.SSN_BUCKET).build(),
                BillingReportDTO.builder().user(SHARED_RESOURCE).resourceName("Collaboration bucket").dlabId(bucketName + "-shared-bucket").resourceType(BillingResourceType.SHARED_BUCKET).build()
        );
    }

    public static Stream<BillingReportDTO> exploratoryBillingDataStream(UserInstanceDTO userInstance) {
        final Stream<BillingReportDTO> computationalStream = userInstance.getResources()
                .stream()
                .filter(cr -> cr.getComputationalId() != null)
                .flatMap(cr -> Stream.of(computationalBillableResource(userInstance, cr),
                        withExploratoryName(userInstance).resourceName(cr.getComputationalName() + ":" + VOLUME_PRIMARY).dlabId(String.format(VOLUME_PRIMARY_FORMAT, cr.getComputationalId()))
                                .resourceType(BillingResourceType.VOLUME).build()));
        final String exploratoryId = userInstance.getExploratoryId();
        final String primaryVolumeId = String.format(VOLUME_PRIMARY_FORMAT, exploratoryId);
        final String secondaryVolumeId = String.format(VOLUME_SECONDARY_FORMAT, exploratoryId);
        final Stream<BillingReportDTO> exploratoryStream = Stream.of(
                withExploratoryName(userInstance).resourceName(userInstance.getExploratoryName()).dlabId(exploratoryId).resourceType(BillingResourceType.EXPLORATORY).build(),
                withExploratoryName(userInstance).resourceName(VOLUME_PRIMARY).dlabId(primaryVolumeId).resourceType(BillingResourceType.VOLUME).build(),
                withExploratoryName(userInstance).resourceName(VOLUME_SECONDARY).dlabId(secondaryVolumeId).resourceType(BillingResourceType.VOLUME).build());
        return Stream.concat(computationalStream, exploratoryStream);
    }

    private static BillingReportDTO computationalBillableResource(UserInstanceDTO userInstance,
                                                                  UserComputationalResource cr) {
        return withExploratoryName(userInstance)
                .dlabId(cr.getComputationalId())
                .resourceName(cr.getComputationalName())
                .resourceType(BillingResourceType.COMPUTATIONAL)
                .project(userInstance.getProject())
                .build();
    }

    private static BillingReportDTO.BillingReportDTOBuilder withExploratoryName(UserInstanceDTO userInstance) {
        return BillingReportDTO.builder().user(userInstance.getUser()).project(userInstance.getProject());
    }
}
