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

package com.epam.dlab.billing.gcp.util;

import com.epam.dlab.billing.gcp.documents.UserInstance;
import com.epam.dlab.billing.gcp.model.BillingData;

import java.util.stream.Stream;

public class BillingUtils {

	private static final String EDGE_FORMAT = "%s-%s-edge";
	private static final String EDGE_VOLUME_FORMAT = "%s-%s-edge-volume-primary";
	private static final String EDGE_BUCKET_FORMAT = "%s-%s-bucket";
	private static final String VOLUME_PRIMARY_FORMAT = "%s-volume-primary";
	private static final String VOLUME_SECONDARY_FORMAT = "%s-volume-secondary";
	private static final String VOLUME_PRIMARY = "Volume primary";
	private static final String VOLUME_SECONDARY = "Volume secondary";
	private static final String SHARED_RESOURCE = "Shared resource";

	public static Stream<BillingData> edgeBillingDataStream(String project, String sbn) {
		final String userEdgeId = String.format(EDGE_FORMAT, sbn, project);
		final String edgeVolumeId = String.format(EDGE_VOLUME_FORMAT, sbn, project);
		final String edgeBucketId = String.format(EDGE_BUCKET_FORMAT, sbn, project);
		return Stream.of(
				BillingData.builder().displayName("EDGE node").user(SHARED_RESOURCE).project(project).dlabId(userEdgeId).resourceType(BillingData.ResourceType.EDGE).build(),
				BillingData.builder().displayName("EDGE volume").user(SHARED_RESOURCE).project(project).dlabId(edgeVolumeId).resourceType(BillingData.ResourceType.VOLUME).build(),
				BillingData.builder().displayName("EDGE bucket").user(SHARED_RESOURCE).project(project).dlabId(edgeBucketId).resourceType(BillingData.ResourceType.EDGE_BUCKET).build()
		);
	}

	public static Stream<BillingData> ssnBillingDataStream(String sbn) {
		final String ssnId = sbn + "-ssn";
		final String bucketName = sbn.replaceAll("_", "-");
		return Stream.of(
				BillingData.builder().user(SHARED_RESOURCE).displayName("SSN").dlabId(ssnId).resourceType(BillingData.ResourceType.SSN).build(),
				BillingData.builder().user(SHARED_RESOURCE).displayName("SSN Volume").dlabId(String.format(VOLUME_PRIMARY_FORMAT, ssnId)).resourceType(BillingData.ResourceType.VOLUME).build(),
				BillingData.builder().user(SHARED_RESOURCE).displayName("SSN bucket").dlabId(bucketName + "-ssn" +
						"-bucket").resourceType(BillingData.ResourceType.SSN_BUCKET).build(),
				BillingData.builder().user(SHARED_RESOURCE).displayName("Collaboration bucket").dlabId(bucketName +
						"-shared-bucket").resourceType(BillingData.ResourceType.SHARED_BUCKET).build()
		);
	}

	public static Stream<BillingData> exploratoryBillingDataStream(UserInstance userInstance) {
		final Stream<BillingData> computationalStream = userInstance.getComputationalResources()
				.stream()
				.flatMap(cr -> Stream.of(computationalBillableResource(userInstance, cr)));
		final String exploratoryId = userInstance.getExploratoryId();
		final String primaryVolumeId = String.format(VOLUME_PRIMARY_FORMAT, exploratoryId);
		final String secondaryVolumeId = String.format(VOLUME_SECONDARY_FORMAT, exploratoryId);
		final Stream<BillingData> exploratoryStream = Stream.of(
				withExploratoryName(userInstance).displayName(userInstance.getExploratoryName()).dlabId(exploratoryId).resourceType(BillingData.ResourceType.EXPLORATORY).build(),
				withExploratoryName(userInstance).displayName(VOLUME_PRIMARY).dlabId(primaryVolumeId).resourceType(BillingData.ResourceType.VOLUME).build(),
				withExploratoryName(userInstance).displayName(VOLUME_SECONDARY).dlabId(secondaryVolumeId).resourceType(BillingData.ResourceType.VOLUME).build());
		return Stream.concat(computationalStream, exploratoryStream);
	}

	private static BillingData computationalBillableResource(UserInstance userInstance,
															 UserInstance.ComputationalResource cr) {
		return withExploratoryName(userInstance)
				.dlabId(cr.getComputationalId())
				.displayName(cr.getComputationalName())
				.resourceType(BillingData.ResourceType.COMPUTATIONAL)
				.computationalName(cr.getComputationalName())
				.project(userInstance.getProject())
				.build();
	}

	private static BillingData.BillingDataBuilder withExploratoryName(UserInstance userInstance) {
		return BillingData.builder().user(userInstance.getUser()).exploratoryName(userInstance.getExploratoryName())
				.project(userInstance.getProject());
	}

}
