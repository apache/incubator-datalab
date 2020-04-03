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

import com.epam.dlab.backendapi.domain.BillingReportLine;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.epam.dlab.dto.billing.BillingResourceType.BUCKET;
import static com.epam.dlab.dto.billing.BillingResourceType.COMPUTATIONAL;
import static com.epam.dlab.dto.billing.BillingResourceType.EDGE;
import static com.epam.dlab.dto.billing.BillingResourceType.ENDPOINT;
import static com.epam.dlab.dto.billing.BillingResourceType.EXPLORATORY;
import static com.epam.dlab.dto.billing.BillingResourceType.IMAGE;
import static com.epam.dlab.dto.billing.BillingResourceType.SSN;
import static com.epam.dlab.dto.billing.BillingResourceType.VOLUME;

public class BillingUtils {
    private static final String[] AVAILABLE_NOTEBOOKS = {"zeppelin", "tensor-rstudio", "rstudio", "tensor", "superset", "jupyterlab", "jupyter", "deeplearning"};
    private static final String[] REPORT_HEADERS = {"DLab ID", "User", "Project", "DLab Resource Type", "Shape", "Product", "Cost"};
    private static final String REPORT_FIRST_LINE = "Service base name: %s. Available reporting period from: %s to: %s";
    private static final String TOTAL_LINE = "Total: %s %s";
    private static final String SSN_FORMAT = "%s-ssn";
    private static final String ENDPOINT_FORMAT = "%s-%s-endpoint";
    private static final String EDGE_FORMAT = "%s-%s-%s-edge";
    private static final String EDGE_VOLUME_FORMAT = "%s-%s-%s-edge-volume-primary";
    private static final String PROJECT_ENDPOINT_BUCKET_FORMAT = "%s-%s-%s-bucket";
    private static final String ENDPOINT_SHARED_BUCKET_FORMAT = "%s-%s-shared-bucket";
    private static final String VOLUME_PRIMARY_FORMAT = "%s-volume-primary";
    private static final String VOLUME_PRIMARY_COMPUTATIONAL_FORMAT = "%s-%s-volume-primary";
    private static final String VOLUME_SECONDARY_FORMAT = "%s-volume-secondary";
    private static final String VOLUME_SECONDARY_COMPUTATIONAL_FORMAT = "%s-%s-volume-secondary";
    private static final String IMAGE_STANDARD_FORMAT1 = "%s-%s-%s-%s-notebook-image";
    private static final String IMAGE_STANDARD_FORMAT2 = "%s-%s-%s-notebook-image";
    private static final String IMAGE_CUSTOM_FORMAT = "%s-%s-%s-%s-%s";
    private static final String IMAGE_NAME_PREFIX = "docker.dlab-";

    private static final String VOLUME_PRIMARY = "Volume primary";
    private static final String VOLUME_SECONDARY = "Volume secondary";
    private static final String SHARED_RESOURCE = "Shared resource";
    private static final String IMAGE_NAME = "Image";

    private static final String DATAENGINE_NAME_FORMAT = "%d x %s";
    private static final String DATAENGINE_SERVICE_NAME_FORMAT = "Master: %s%sSlave:  %d x %s";

    public static Stream<BillingReportLine> edgeBillingDataStream(String project, String sbn, String endpoint, String status) {
        final String userEdgeId = String.format(EDGE_FORMAT, sbn, project.toLowerCase(), endpoint);
        final String edgeVolumeId = String.format(EDGE_VOLUME_FORMAT, sbn, project.toLowerCase(), endpoint);
        final String endpointBucketId = String.format(PROJECT_ENDPOINT_BUCKET_FORMAT, sbn, project.toLowerCase(), endpoint);

        return Stream.concat(Stream.of(
                BillingReportLine.builder().resourceName("EDGE node").user(SHARED_RESOURCE).project(project).dlabId(userEdgeId).resourceType(EDGE).status(UserInstanceStatus.of(status)).build(),
                BillingReportLine.builder().resourceName("EDGE volume").user(SHARED_RESOURCE).project(project).dlabId(edgeVolumeId).resourceType(VOLUME).build(),
                BillingReportLine.builder().resourceName("Project endpoint shared bucket").user(SHARED_RESOURCE).project(project).dlabId(endpointBucketId).resourceType(BUCKET).build()
                ),
                standardImageBillingDataStream(sbn, project, endpoint)
        );
    }

    public static Stream<BillingReportLine> ssnBillingDataStream(String sbn) {
        final String ssnId = String.format(SSN_FORMAT, sbn);
        return Stream.of(
                BillingReportLine.builder().user(SHARED_RESOURCE).project(SHARED_RESOURCE).resourceName("SSN").dlabId(ssnId).resourceType(SSN).build(),
                BillingReportLine.builder().user(SHARED_RESOURCE).project(SHARED_RESOURCE).resourceName("SSN Volume").dlabId(String.format(VOLUME_PRIMARY_FORMAT, ssnId)).resourceType(VOLUME).build()
        );
    }

    public static Stream<BillingReportLine> sharedEndpointBillingDataStream(String endpoint, String sbn) {
        final String projectEndpointBucketId = String.format(ENDPOINT_SHARED_BUCKET_FORMAT, sbn, endpoint.toLowerCase());
        final String endpointId = String.format(ENDPOINT_FORMAT, sbn, endpoint.toLowerCase());
        return Stream.of(
                BillingReportLine.builder().resourceName("Endpoint shared bucket").user(SHARED_RESOURCE).project(SHARED_RESOURCE).dlabId(projectEndpointBucketId).resourceType(BUCKET).build(),
                BillingReportLine.builder().resourceName("Endpoint").user(SHARED_RESOURCE).project(SHARED_RESOURCE).dlabId(endpointId).resourceType(ENDPOINT).build()
        );
    }

    public static Stream<BillingReportLine> exploratoryBillingDataStream(UserInstanceDTO userInstance, Integer maxSparkInstanceCount, String sbn) {
        final Stream<BillingReportLine> computationalStream = userInstance.getResources()
                .stream()
                .filter(cr -> cr.getComputationalId() != null)
                .flatMap(cr -> Stream.concat(Stream.of(
                        withUserProject(userInstance).dlabId(cr.getComputationalId()).resourceName(cr.getComputationalName()).resourceType(COMPUTATIONAL)
                                .status(UserInstanceStatus.of(cr.getStatus())).shape(getComputationalShape(cr)).build(),
                        withUserProject(userInstance).resourceName(cr.getComputationalName() + ":" + VOLUME_PRIMARY).dlabId(String.format(VOLUME_PRIMARY_COMPUTATIONAL_FORMAT, cr.getComputationalId(), "m"))
                                .resourceType(VOLUME).build(),
                        withUserProject(userInstance).resourceName(cr.getComputationalName() + ":" + VOLUME_SECONDARY).dlabId(String.format(VOLUME_SECONDARY_COMPUTATIONAL_FORMAT, cr.getComputationalId(), "m"))
                                .resourceType(VOLUME).build()
                        ),
                        getSlaveVolumes(userInstance, cr, maxSparkInstanceCount)
                ));
        final String exploratoryId = userInstance.getExploratoryId();
        final String primaryVolumeId = String.format(VOLUME_PRIMARY_FORMAT, exploratoryId);
        final String secondaryVolumeId = String.format(VOLUME_SECONDARY_FORMAT, exploratoryId);
        final Stream<BillingReportLine> exploratoryStream = Stream.of(
                withUserProject(userInstance).resourceName(userInstance.getExploratoryName()).dlabId(exploratoryId).resourceType(EXPLORATORY).status(UserInstanceStatus.of(userInstance.getStatus())).shape(userInstance.getShape()).build(),
                withUserProject(userInstance).resourceName(VOLUME_PRIMARY).dlabId(primaryVolumeId).resourceType(VOLUME).build(),
                withUserProject(userInstance).resourceName(VOLUME_SECONDARY).dlabId(secondaryVolumeId).resourceType(VOLUME).build());

        return Stream.concat(computationalStream, exploratoryStream);
    }

    public static Stream<BillingReportLine> customImageBillingDataStream(ImageInfoRecord image, String sbn) {
        String imageId = String.format(IMAGE_CUSTOM_FORMAT, sbn, image.getProject(), image.getEndpoint(), image.getApplication(), image.getName());
        return Stream.of(
                BillingReportLine.builder().resourceName(IMAGE_NAME).project(image.getProject()).dlabId(imageId).resourceType(IMAGE).build()
        );
    }

    private static Stream<BillingReportLine> getSlaveVolumes(UserInstanceDTO userInstance, UserComputationalResource cr, Integer maxSparkInstanceCount) {
        List<BillingReportLine> list = new ArrayList<>();
        for (int i = 1; i <= maxSparkInstanceCount; i++) {
            list.add(withUserProject(userInstance).resourceName(cr.getComputationalName() + ":" + VOLUME_PRIMARY).dlabId(String.format(VOLUME_PRIMARY_COMPUTATIONAL_FORMAT, cr.getComputationalId(), "s" + i))
                    .resourceType(VOLUME).build());
            list.add(withUserProject(userInstance).resourceName(cr.getComputationalName() + ":" + VOLUME_PRIMARY).dlabId(String.format(VOLUME_SECONDARY_COMPUTATIONAL_FORMAT, cr.getComputationalId(), "s" + i))
                    .resourceType(VOLUME).build());
        }
        return list.stream();
    }

    private static BillingReportLine.BillingReportLineBuilder withUserProject(UserInstanceDTO userInstance) {
        return BillingReportLine.builder().user(userInstance.getUser()).project(userInstance.getProject());
    }

    public static List<String> getComputationalIds(String computationalId) {
        return Arrays.asList(computationalId, String.format(VOLUME_PRIMARY_FORMAT, computationalId));
    }

    public static List<String> getExploratoryIds(String exploratoryId) {
        return Arrays.asList(exploratoryId, String.format(VOLUME_PRIMARY_FORMAT, exploratoryId), String.format(VOLUME_SECONDARY_FORMAT, exploratoryId));
    }

    private static String getComputationalShape(UserComputationalResource resource) {
        return DataEngineType.fromDockerImageName(resource.getImageName()) == DataEngineType.SPARK_STANDALONE ?
                String.format(DATAENGINE_NAME_FORMAT, resource.getDataengineInstanceCount(), resource.getDataengineShape()) :
                String.format(DATAENGINE_SERVICE_NAME_FORMAT, resource.getMasterNodeShape(), System.lineSeparator(), null, null);
    }

    public static Stream<BillingReportLine> standardImageBillingDataStream(String sbn, String project, String endpoint) {
        List<BillingReportLine> list = new ArrayList<>();
        for (String notebook : AVAILABLE_NOTEBOOKS) {
            list.add(BillingReportLine.builder().resourceName(IMAGE_NAME).dlabId(String.format(IMAGE_STANDARD_FORMAT1, sbn, project, endpoint, notebook))
                    .project(SHARED_RESOURCE).resourceType(IMAGE).build());
            list.add(BillingReportLine.builder().resourceName(IMAGE_NAME).dlabId(String.format(IMAGE_STANDARD_FORMAT2, sbn, endpoint, notebook))
                    .project(SHARED_RESOURCE).resourceType(IMAGE).build());
        }

        return list.stream();
    }

    public static String getFirstLine(String sbn, LocalDate from, LocalDate to) {
        return CSVFormatter.formatLine(Lists.newArrayList(String.format(REPORT_FIRST_LINE, sbn,
                Optional.ofNullable(from).map(date -> date.format(DateTimeFormatter.ISO_DATE)).orElse(StringUtils.EMPTY),
                Optional.ofNullable(to).map(date -> date.format(DateTimeFormatter.ISO_DATE)).orElse(StringUtils.EMPTY))),
                CSVFormatter.SEPARATOR, '\"');
    }

    public static String getHeader() {
        return CSVFormatter.formatLine(new ArrayList<>(Arrays.asList(BillingUtils.REPORT_HEADERS)), CSVFormatter.SEPARATOR);
    }

    public static String printLine(BillingReportLine line) {
        List<String> lines = new ArrayList<>();
        lines.add(getOrEmpty(line.getDlabId()));
        lines.add(getOrEmpty(line.getUser()));
        lines.add(getOrEmpty(line.getProject()));
        lines.add(getOrEmpty(Optional.ofNullable(line.getResourceType()).map(Enum::name).orElse(null)));
        lines.add(getOrEmpty(line.getShape()));
        lines.add(getOrEmpty(line.getProduct()));
        lines.add(getOrEmpty(Optional.ofNullable(line.getCost()).map(String::valueOf).orElse(null)));
        return CSVFormatter.formatLine(lines, CSVFormatter.SEPARATOR);
    }

    public static String getTotal(Double total, String currency) {
        List<String> totalLine = new ArrayList<>();
        for (int i = 0; i < REPORT_HEADERS.length - 1; i++) {
            totalLine.add(StringUtils.EMPTY);
        }
        totalLine.add(REPORT_HEADERS.length - 1, String.format(TOTAL_LINE, getOrEmpty(String.valueOf(total)), getOrEmpty(currency)));
        return CSVFormatter.formatLine(totalLine, CSVFormatter.SEPARATOR);

    }

    private static String getOrEmpty(String s) {
        return Objects.nonNull(s) ? s : StringUtils.EMPTY;
    }
}
