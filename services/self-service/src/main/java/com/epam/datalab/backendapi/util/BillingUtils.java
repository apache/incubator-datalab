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

package com.epam.datalab.backendapi.util;

import com.epam.datalab.backendapi.domain.BillingReportLine;
import com.epam.datalab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.computational.UserComputationalResource;
import jersey.repackaged.com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Stream;

import static com.epam.datalab.dto.billing.BillingResourceType.*;

@Slf4j
public class BillingUtils {
    private static final String[] AVAILABLE_NOTEBOOKS = {"zeppelin", "tensor-rstudio", "tensor-jupyterlab", "rstudio", "tensor", "superset", "jupyterlab", "jupyter", "jupyter-gpu", "deeplearning"};
    private static final String[] BILLING_FILTERED_REPORT_HEADERS = {"DataLab ID", "Project", "DataLab Resource Type", "Status", "Shape", "Product", "Cost"};
    private static final String[] COMPLETE_REPORT_REPORT_HEADERS = {"DataLab ID", "User", "Project", "DataLab Resource Type", "Status", "Shape", "Product", "Cost"};

    private static final String REPORT_FIRST_LINE = "Service base name: %s. Available reporting period from: %s to: %s";
    private static final String TOTAL_LINE = "Total: %s %s";
    private static final String SSN_FORMAT = "%s-ssn";
    private static final String ENDPOINT_FORMAT = "%s-%s-endpoint";
    private static final String EDGE_FORMAT = "%s-%s-%s-edge";
    private static final String EDGE_VOLUME_FORMAT = "%s-%s-%s-edge-volume-primary";
    private static final String PROJECT_ENDPOINT_BUCKET_FORMAT = "%s-%s-%s-bucket";
    private static final String ENDPOINT_SHARED_BUCKET_FORMAT = "%s-%s-shared-bucket";
    private static final String DATA_ENGINE_BUCKET_FORMAT = "%s-bucket";

    private static final String VOLUME_PRIMARY_FORMAT = "%s-volume-primary";
    private static final String VOLUME_PRIMARY_COMPUTATIONAL_FORMAT = "%s-%s-volume-primary";
    private static final String VOLUME_SECONDARY_FORMAT = "%s-volume-secondary";
    private static final String VOLUME_SECONDARY_COMPUTATIONAL_FORMAT = "%s-%s-volume-secondary";

    private static final String IMAGE_STANDARD_FORMAT1 = "%s-%s-%s-%s-notebook-image";
    private static final String IMAGE_STANDARD_FORMAT2 = "%s-%s-%s-notebook-image";
    private static final String IMAGE_CUSTOM_FORMAT = "%s-%s-%s-%s-%s";
    // GCP specific
    private static final String IMAGE_VOLUME_PRIMARY_GCP = "%s-%s-%s-%s-primary-image";
    private static final String IMAGE_VOLUME_SECONDARY_GCP = "%s-%s-%s-%s-secondary-image";
    private static final String IMAGE_CUSTOM_VOLUME_PRIMARY_GCP = "%s-%s-%s-%s-primary-image-%s";
    private static final String IMAGE_CUSTOM_VOLUME_SECONDARY_GCP = "%s-%s-%s-%s-secondary-image-%s";

    private static final String SHARED_RESOURCE = "Shared resource";
    private static final String IMAGE_NAME = "Image";

    private static final String DATAENGINE_NAME_FORMAT = "%d x %s";
    private static final String DATAENGINE_SERVICE_NAME_FORMAT = "Master: %sSlave: %d x %s";

    public static Stream<BillingReportLine> edgeBillingDataStream(String project, String sbn, String endpoint) {
        final String userEdgeId = String.format(EDGE_FORMAT, sbn, project, endpoint).toLowerCase();
        final String edgeVolumeId = String.format(EDGE_VOLUME_FORMAT, sbn, project, endpoint).toLowerCase();
        final String endpointBucketId = String.format(PROJECT_ENDPOINT_BUCKET_FORMAT, sbn, project, endpoint).toLowerCase();

        return Stream.concat(Stream.of(
                BillingReportLine.builder()
                        .resourceName(endpoint)
                        .user(SHARED_RESOURCE)
                        .project(project)
                        .datalabId(userEdgeId)
                        .resourceType(EDGE)
                        .build(),
                BillingReportLine.builder()
                        .resourceName("EDGE volume")
                        .user(SHARED_RESOURCE)
                        .project(project)
                        .datalabId(edgeVolumeId)
                        .resourceType(VOLUME)
                        .build(),
                BillingReportLine.builder()
                        .resourceName("Project endpoint shared bucket")
                        .user(SHARED_RESOURCE)
                        .project(project)
                        .datalabId(endpointBucketId)
                        .resourceType(BUCKET)
                        .build()
                ),
                standardImageBillingDataStream(sbn, project, endpoint)
        );
    }

    public static Stream<BillingReportLine> ssnBillingDataStream(String sbn) {
        final String ssnId = String.format(SSN_FORMAT, sbn);
        return Stream.of(
                BillingReportLine
                        .builder()
                        .user(SHARED_RESOURCE)
                        .project(SHARED_RESOURCE)
                        .resourceName("SSN")
                        .datalabId(ssnId)
                        .resourceType(SSN)
                        .build(),
                BillingReportLine.builder()
                        .user(SHARED_RESOURCE)
                        .project(SHARED_RESOURCE)
                        .resourceName("SSN Volume")
                        .datalabId(String.format(VOLUME_PRIMARY_FORMAT, ssnId))
                        .resourceType(VOLUME).build()
        );
    }

    public static Stream<BillingReportLine> sharedEndpointBillingDataStream(String endpoint, String sbn) {
        final String projectEndpointBucketId = String.format(ENDPOINT_SHARED_BUCKET_FORMAT, sbn, endpoint).toLowerCase();
        final String endpointId = String.format(ENDPOINT_FORMAT, sbn, endpoint).toLowerCase();
        return Stream.concat(Stream.of(
                BillingReportLine.builder()
                        .resourceName("Endpoint shared bucket")
                        .user(SHARED_RESOURCE)
                        .project(SHARED_RESOURCE)
                        .datalabId(projectEndpointBucketId)
                        .resourceType(BUCKET)
                        .build(),
                BillingReportLine.builder()
                        .resourceName("Endpoint")
                        .user(SHARED_RESOURCE)
                        .project(SHARED_RESOURCE)
                        .datalabId(endpointId)
                        .resourceType(ENDPOINT).build()
                ),
                standardImageBillingDataStream(sbn, endpoint));
    }

    public static Stream<BillingReportLine> exploratoryBillingDataStream(UserInstanceDTO userInstance, Integer maxSparkInstanceCount) {
        final Stream<BillingReportLine> computationalStream = userInstance.getResources()
                .stream()
                .filter(cr -> cr.getComputationalId() != null)
                .flatMap(cr -> {
                    final String computationalId = getDatalabIdForComputeResources(cr);
                    return Stream.concat(Stream.of(
                            withUserProjectEndpoint(userInstance)
                                    .resourceName(cr.getComputationalName())
                                    .datalabId(computationalId)
                                    .resourceType(COMPUTATIONAL)
                                    .shape(getComputationalShape(cr))
                                    .exploratoryName(userInstance.getExploratoryName())
                                    .build(),
                            withUserProjectEndpoint(userInstance)
                                    .resourceName(cr.getComputationalName())
                                    .datalabId(String.format(VOLUME_PRIMARY_FORMAT, computationalId))
                                    .resourceType(VOLUME)
                                    .build(),
                            withUserProjectEndpoint(userInstance)
                                    .resourceName(cr.getComputationalName())
                                    .datalabId(String.format(VOLUME_SECONDARY_FORMAT, computationalId))
                                    .resourceType(VOLUME)
                                    .build(),
                            withUserProjectEndpoint(userInstance)
                                    .resourceName(cr.getComputationalName())
                                    .datalabId(String.format(VOLUME_PRIMARY_COMPUTATIONAL_FORMAT, computationalId, "m"))
                                    .resourceType(VOLUME)
                                    .build(),
                            withUserProjectEndpoint(userInstance)
                                    .resourceName(cr.getComputationalName())
                                    .datalabId(String.format(VOLUME_SECONDARY_COMPUTATIONAL_FORMAT, computationalId, "m"))
                                    .resourceType(VOLUME)
                                    .build(),
                            withUserProjectEndpoint(userInstance)
                                    .resourceName(cr.getComputationalName())
                                    .datalabId(String.format(DATA_ENGINE_BUCKET_FORMAT, computationalId))
                                    .resourceType(BUCKET)
                                    .build()
                            ),
                            getSlaveVolumes(userInstance, cr, maxSparkInstanceCount)
                    );
                });

        final String exploratoryName = userInstance.getExploratoryName();
        final String exploratoryId = userInstance.getExploratoryId().toLowerCase();
        final String primaryVolumeId = String.format(VOLUME_PRIMARY_FORMAT, exploratoryId);
        final String secondaryVolumeId = String.format(VOLUME_SECONDARY_FORMAT, exploratoryId);
        final Stream<BillingReportLine> exploratoryStream = Stream.of(
                withUserProjectEndpoint(userInstance)
                        .resourceName(exploratoryName)
                        .datalabId(exploratoryId)
                        .resourceType(EXPLORATORY)
                        .shape(userInstance.getShape())
                        .build(),
                withUserProjectEndpoint(userInstance)
                        .resourceName(exploratoryName)
                        .datalabId(primaryVolumeId)
                        .resourceType(VOLUME)
                        .build(),
                withUserProjectEndpoint(userInstance)
                        .resourceName(exploratoryName)
                        .datalabId(secondaryVolumeId)
                        .resourceType(VOLUME)
                        .build());

        return Stream.concat(computationalStream, exploratoryStream);
    }

    public static Stream<BillingReportLine> customImageBillingDataStream(ImageInfoRecord image, String sbn) {
        String imageId = String.format(IMAGE_CUSTOM_FORMAT, sbn, image.getProject(), image.getEndpoint(), image.getApplication(), image.getName()).toLowerCase();
        String imageIdGCP1 = String.format(IMAGE_CUSTOM_VOLUME_PRIMARY_GCP, sbn, image.getProject(), image.getEndpoint(), image.getApplication(), image.getName()).toLowerCase();
        String imageIdGCP2 = String.format(IMAGE_CUSTOM_VOLUME_SECONDARY_GCP, sbn, image.getProject(), image.getEndpoint(), image.getApplication(), image.getName()).toLowerCase();
        return Stream.of(
                BillingReportLine.builder().resourceName(image.getName()).project(image.getProject()).datalabId(imageId).user(image.getUser()).resourceType(IMAGE).build(),
                BillingReportLine.builder().resourceName(image.getName()).project(image.getProject()).datalabId(imageIdGCP1).user(image.getUser()).resourceType(IMAGE).build(),
                BillingReportLine.builder().resourceName(image.getName()).project(image.getProject()).datalabId(imageIdGCP2).user(image.getUser()).resourceType(IMAGE).build()

        );
    }

    /**
        For HDInsight computational_id begins with random id
        which differs from datalab_id tag
     */
    private static String getDatalabIdForComputeResources(UserComputationalResource cr){
        if(cr.getTemplateName().equals("HDInsight cluster")){
            return cr.getComputationalId().toLowerCase().substring(7);
        }
        return cr.getComputationalId().toLowerCase();
    }

    private static Stream<BillingReportLine> getSlaveVolumes(UserInstanceDTO userInstance, UserComputationalResource cr, Integer maxSparkInstanceCount) {
        List<BillingReportLine> list = new ArrayList<>();
        for (int i = 1; i <= maxSparkInstanceCount; i++) {
            list.add(withUserProjectEndpoint(userInstance).resourceName(cr.getComputationalName()).datalabId(String.format(VOLUME_PRIMARY_COMPUTATIONAL_FORMAT, getDatalabIdForComputeResources(cr), "s" + i))
                    .resourceType(VOLUME).build());
            list.add(withUserProjectEndpoint(userInstance).resourceName(cr.getComputationalName()).datalabId(String.format(VOLUME_SECONDARY_COMPUTATIONAL_FORMAT, getDatalabIdForComputeResources(cr), "s" + i))
                    .resourceType(VOLUME).build());
        }
        return list.stream();
    }

    private static BillingReportLine.BillingReportLineBuilder withUserProjectEndpoint(UserInstanceDTO userInstance) {
        return BillingReportLine.builder()
                .user(userInstance.getUser())
                .project(userInstance.getProject())
                .endpoint(userInstance.getEndpoint());
    }

    public static String getComputationalShape(UserComputationalResource resource) {
        return DataEngineType.fromDockerImageName(resource.getImageName()) != DataEngineType.SPARK_STANDALONE ?
                String.format(DATAENGINE_NAME_FORMAT, resource.getDataengineInstanceCount(), resource.getDataengineShape()) :
                String.format(DATAENGINE_SERVICE_NAME_FORMAT, resource.getMasterNodeShape(), resource.getTotalInstanceCount() - 1, resource.getSlaveNodeShape());
    }

    private static Stream<BillingReportLine> standardImageBillingDataStream(String sbn, String endpoint) {
        List<BillingReportLine> list = new ArrayList<>();
        for (String notebook : AVAILABLE_NOTEBOOKS) {
            list.add(BillingReportLine.builder()
                    .resourceName(IMAGE_NAME)
                    .datalabId(String.format(IMAGE_STANDARD_FORMAT2, sbn, endpoint, notebook).toLowerCase())
                    .user(SHARED_RESOURCE)
                    .project(SHARED_RESOURCE)
                    .resourceType(IMAGE)
                    .build());
        }
        return list.stream();
    }

    private static Stream<BillingReportLine> standardImageBillingDataStream(String sbn, String project, String endpoint) {
        List<BillingReportLine> list = new ArrayList<>();
        for (String notebook : AVAILABLE_NOTEBOOKS) {
            list.add(BillingReportLine
                    .builder()
                    .resourceName(IMAGE_NAME)
                    .datalabId(String.format(IMAGE_STANDARD_FORMAT1, sbn, project, endpoint, notebook).toLowerCase())
                    .project(project)
                    .user(SHARED_RESOURCE)
                    .resourceType(IMAGE)
                    .build());
            list.add(BillingReportLine
                    .builder()
                    .resourceName(IMAGE_NAME)
                    .datalabId(String.format(IMAGE_VOLUME_PRIMARY_GCP, sbn, project, endpoint, notebook).toLowerCase())
                    .project(project)
                    .user(SHARED_RESOURCE)
                    .resourceType(IMAGE)
                    .build());
            list.add(BillingReportLine
                    .builder()
                    .resourceName(IMAGE_NAME)
                    .datalabId(String.format(IMAGE_VOLUME_SECONDARY_GCP, sbn, project, endpoint, notebook).toLowerCase())
                    .project(project)
                    .user(SHARED_RESOURCE)
                    .resourceType(IMAGE)
                    .build());
        }
        return list.stream();
    }

    /**
     * @param sbn    Service Base Name
     * @param from   formatted date, like 2020-04-07
     * @param to     formatted date, like 2020-05-07
     * @param locale user's locale
     * @return line, like:
     * "Service base name: SERVICE_BASE_NAME. Available reporting period from: 2020-04-07 to: 2020-04-07"
     */
    public static String getFirstLine(String sbn, LocalDate from, LocalDate to, String locale) {
        return CSVFormatter.formatLine(Lists.newArrayList(String.format(REPORT_FIRST_LINE, sbn,
                Optional.ofNullable(from).map(date -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(locale)))).orElse(StringUtils.EMPTY),
                Optional.ofNullable(to).map(date -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(locale)))).orElse(StringUtils.EMPTY))),
                CSVFormatter.SEPARATOR, '\"');
    }

    /**
     * headerType there are two types of header according user role
     *
     * @return line, like DataLab ID,User,Project,DataLab Resource Type,Status,Shape,Product,Cost
     * in case of additional header type, the ENUM object will be propagated from the Service Impl Class
     */
    public static String getHeader(boolean isReportHeaderCompletable) {
        if (!isReportHeaderCompletable) {
            return CSVFormatter.formatLine(Arrays.asList(BillingUtils.BILLING_FILTERED_REPORT_HEADERS), CSVFormatter.SEPARATOR);
        }
        return CSVFormatter.formatLine(Arrays.asList(BillingUtils.COMPLETE_REPORT_REPORT_HEADERS), CSVFormatter.SEPARATOR);
    }

    public static String printLine(BillingReportLine line, boolean isReportHeaderCompletable) {
        List<String> lines = new ArrayList<>();
        lines.add(getOrEmpty(line.getDatalabId()));
        //if user does not have the billing role, the User field should not be present in report
        if (isReportHeaderCompletable) {
            lines.add(getOrEmpty(line.getUser()));
        }
        lines.add(getOrEmpty(line.getProject()));
        lines.add(getOrEmpty(Optional.ofNullable(line.getResourceType()).map(r -> StringUtils.capitalize(r.toString().toLowerCase())).orElse(null)));
        lines.add(getOrEmpty(Optional.ofNullable(line.getStatus()).map(UserInstanceStatus::toString).orElse(null)));
        lines.add(getOrEmpty(line.getShape()));
        lines.add(getOrEmpty(line.getProduct()));
        lines.add(getOrEmpty(Optional.ofNullable(line.getCost()).map(String::valueOf).orElse(null)));
        return CSVFormatter.formatLine(lines, CSVFormatter.SEPARATOR);
    }

    /**
     * @param total                  monetary amount
     * @param currency               user's currency
     * @param stringOfAdjustedHeader filtered fields of report header
     * @return line with cost of resources
     */
    public static String getTotal(Double total, String currency, String stringOfAdjustedHeader) {
        List<String> totalLine = new ArrayList<>();
        String[] headerFieldsList = stringOfAdjustedHeader.split(String.valueOf(CSVFormatter.SEPARATOR));
        for (int i = 0; i < headerFieldsList.length - 1; i++) {
            totalLine.add(StringUtils.EMPTY);
        }
        totalLine.add(headerFieldsList.length - 1, String.format(TOTAL_LINE, getOrEmpty(String.valueOf(total)), getOrEmpty(currency)));
        return CSVFormatter.formatLine(totalLine, CSVFormatter.SEPARATOR);

    }

    private static String getOrEmpty(String s) {
        return Objects.nonNull(s) ? s : StringUtils.EMPTY;
    }
}
