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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.annotation.Audit;
import com.epam.datalab.backendapi.annotation.Project;
import com.epam.datalab.backendapi.annotation.ResourceName;
import com.epam.datalab.backendapi.annotation.User;
import com.epam.datalab.backendapi.dao.ComputationalDAO;
import com.epam.datalab.backendapi.dao.EnvDAO;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.SchedulerJobDAO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.ComputationalService;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.service.SchedulerJobService;
import com.epam.datalab.backendapi.service.SecurityService;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.exceptions.ResourceInappropriateStateException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.model.scheduler.SchedulerJobData;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.SET_UP_SCHEDULER;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.COMPUTE;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.INSTANCE;
import static com.epam.datalab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.datalab.dto.UserInstanceStatus.CONFIGURING;
import static com.epam.datalab.dto.UserInstanceStatus.CREATING;
import static com.epam.datalab.dto.UserInstanceStatus.RUNNING;
import static com.epam.datalab.dto.UserInstanceStatus.STARTING;
import static com.epam.datalab.dto.UserInstanceStatus.STOPPED;
import static com.epam.datalab.dto.UserInstanceStatus.STOPPING;
import static com.epam.datalab.dto.UserInstanceStatus.TERMINATING;
import static com.epam.datalab.dto.base.DataEngineType.getDockerImageName;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.singletonList;
import static java.util.Date.from;

@Slf4j
@Singleton
public class SchedulerJobServiceImpl implements SchedulerJobService {
    private static final String SCHEDULER_NOT_FOUND_MSG = "Scheduler job data not found for user %s with exploratory %s";
    private static final String AUDIT_MESSAGE = "Scheduled action, requested for notebook %s";
    private static final long ALLOWED_INACTIVITY_MINUTES = 1L;

    @Inject
    private SchedulerJobDAO schedulerJobDAO;

    @Inject
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private ComputationalDAO computationalDAO;

    @Inject
    private ExploratoryService exploratoryService;

    @Inject
    private ComputationalService computationalService;

    @Inject
    private SecurityService securityService;

    @Inject
    private EnvDAO envDAO;

    @Inject
    private RequestId requestId;

    @Inject
    @Named(PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @Override
    public SchedulerJobDTO fetchSchedulerJobForUserAndExploratory(String user, String project, String exploratoryName) {
        return schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(user, project, exploratoryName)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(SCHEDULER_NOT_FOUND_MSG, user,
                        exploratoryName)));
    }

    @Override
    public SchedulerJobDTO fetchSchedulerJobForComputationalResource(String user, String project, String exploratoryName,
                                                                     String computationalName) {
        return schedulerJobDAO.fetchSingleSchedulerJobForCluster(user, project, exploratoryName, computationalName)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(SCHEDULER_NOT_FOUND_MSG, user,
                        exploratoryName) + " with computational resource " + computationalName));
    }

    @Audit(action = SET_UP_SCHEDULER, type = INSTANCE)
    @Override
    public void updateExploratorySchedulerData(@User UserInfo user, @Project String project, @ResourceName String exploratoryName, SchedulerJobDTO dto) {
        validateExploratoryStatus(user.getName(), project, exploratoryName);
        populateDefaultSchedulerValues(dto);
        log.debug("Updating exploratory {} for user {} with new scheduler job data: {}...", exploratoryName, user,
                dto);
        exploratoryDAO.updateSchedulerDataForUserAndExploratory(user.getName(), project, exploratoryName, dto);

        if (!dto.inactivityScheduler() && dto.isSyncStartRequired()) {
            shareSchedulerJobDataToSparkClusters(user.getName(), project, exploratoryName, dto);
        } else if (!dto.inactivityScheduler()) {
            computationalDAO.updateSchedulerSyncFlag(user.getName(), project, exploratoryName, dto.isSyncStartRequired());
        }
    }

    @Audit(action = SET_UP_SCHEDULER, type = COMPUTE)
    @Override
    public void updateComputationalSchedulerData(@User UserInfo user, @Project String project, String exploratoryName, @ResourceName String computationalName, SchedulerJobDTO dto) {
        validateExploratoryStatus(user.getName(), project, exploratoryName);
        validateComputationalStatus(user.getName(), project, exploratoryName, computationalName);
        populateDefaultSchedulerValues(dto);
        log.debug("Updating computational resource {} affiliated with exploratory {} for user {} with new scheduler " +
                "job data {}...", computationalName, exploratoryName, user, dto);
        computationalDAO.updateSchedulerDataForComputationalResource(user.getName(), project, exploratoryName, computationalName, dto);
    }

    @Override
    public void stopComputationalByScheduler() {
        getComputationalSchedulersForStopping(OffsetDateTime.now(), true)
                .forEach(this::stopComputational);
    }

    @Override
    public void stopExploratoryByScheduler() {
        getExploratorySchedulersForStopping(OffsetDateTime.now(), true)
                .forEach(this::stopExploratory);
    }

    @Override
    public void startExploratoryByScheduler() {
        getExploratorySchedulersForStarting(OffsetDateTime.now())
                .forEach(this::startExploratory);
    }

    @Override
    public void startComputationalByScheduler() {
        getComputationalSchedulersForStarting(OffsetDateTime.now())
                .forEach(job -> startSpark(job.getUser(), job.getExploratoryName(), job.getComputationalName(),
                        job.getProject()));
    }

    @Override
    public void terminateExploratoryByScheduler() {
        getExploratorySchedulersForTerminating(OffsetDateTime.now())
                .forEach(this::terminateExploratory);

    }

    @Override
    public void terminateComputationalByScheduler() {
        getComputationalSchedulersForTerminating(OffsetDateTime.now()).forEach(this::terminateComputational);

    }

    @Override
    public void removeScheduler(String user, String exploratoryName) {
        schedulerJobDAO.removeScheduler(user, exploratoryName);
    }

    @Override
    public void removeScheduler(String user, String exploratoryName, String computationalName) {
        schedulerJobDAO.removeScheduler(user, exploratoryName, computationalName);
    }

    @Override
    public List<SchedulerJobData> getActiveSchedulers(String user, long minutesOffset) {
        final OffsetDateTime desiredDateTime = OffsetDateTime.now().plusMinutes(minutesOffset);
        final Predicate<SchedulerJobData> userPredicate = s -> user.equals(s.getUser());
        final Stream<SchedulerJobData> computationalSchedulersStream =
                getComputationalSchedulersForStopping(desiredDateTime)
                        .stream()
                        .filter(userPredicate);
        final Stream<SchedulerJobData> exploratorySchedulersStream =
                getExploratorySchedulersForStopping(desiredDateTime)
                        .stream()
                        .filter(userPredicate);
        return Stream.concat(computationalSchedulersStream, exploratorySchedulersStream)
                .collect(Collectors.toList());
    }

    private void stopComputational(SchedulerJobData job) {
        final String project = job.getProject();
        final String expName = job.getExploratoryName();
        final String compName = job.getComputationalName();
        final String user = job.getUser();
        log.debug("Stopping exploratory {} computational {} for user {} by scheduler", expName, compName, user);
        computationalService.stopSparkCluster(securityService.getServiceAccountInfo(user), user, project, expName, compName, String.format(AUDIT_MESSAGE, expName));
    }

    private void terminateComputational(SchedulerJobData job) {
        final String user = job.getUser();
        final String expName = job.getExploratoryName();
        final String compName = job.getComputationalName();
        final UserInfo userInfo = securityService.getServiceAccountInfo(user);
        log.debug("Terminating exploratory {} computational {} for user {} by scheduler", expName, compName, user);
        computationalService.terminateComputational(userInfo, user, job.getProject(), expName, compName, String.format(AUDIT_MESSAGE, expName));
    }

    private void stopExploratory(SchedulerJobData job) {
        final String expName = job.getExploratoryName();
        final String user = job.getUser();
        final String project = job.getProject();
        log.debug("Stopping exploratory {} for user {} by scheduler", expName, user);
        exploratoryService.stop(securityService.getServiceAccountInfo(user), user, project, expName, String.format(AUDIT_MESSAGE, expName));
    }

    private List<SchedulerJobData> getExploratorySchedulersForTerminating(OffsetDateTime now) {
        return schedulerJobDAO.getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED)
                .stream()
                .filter(canSchedulerForTerminatingBeApplied(now))
                .collect(Collectors.toList());
    }

    private List<SchedulerJobData> getComputationalSchedulersForTerminating(OffsetDateTime now) {
        return schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(RUNNING, STOPPED, RUNNING)
                .stream()
                .filter(canSchedulerForTerminatingBeApplied(now))
                .collect(Collectors.toList());
    }

    private void startExploratory(SchedulerJobData schedulerJobData) {
        final String user = schedulerJobData.getUser();
        final String exploratoryName = schedulerJobData.getExploratoryName();
        final String project = schedulerJobData.getProject();
        log.debug("Starting exploratory {} for user {} by scheduler", exploratoryName, user);
        exploratoryService.start(securityService.getServiceAccountInfo(user), exploratoryName, project, String.format(AUDIT_MESSAGE, exploratoryName));
        if (schedulerJobData.getJobDTO().isSyncStartRequired()) {
            log.trace("Starting computational for exploratory {} for user {} by scheduler", exploratoryName, user);
            final DataEngineType sparkCluster = DataEngineType.SPARK_STANDALONE;
            final List<UserComputationalResource> compToBeStarted =
                    computationalDAO.findComputationalResourcesWithStatus(user, project, exploratoryName, STOPPED);

            compToBeStarted
                    .stream()
                    .filter(compResource -> shouldClusterBeStarted(sparkCluster, compResource))
                    .forEach(comp -> startSpark(user, exploratoryName, comp.getComputationalName(), project));
        }
    }

    private void terminateExploratory(SchedulerJobData job) {
        final String user = job.getUser();
        final String project = job.getProject();
        final String expName = job.getExploratoryName();
        log.debug("Terminating exploratory {} for user {} by scheduler", expName, user);
        exploratoryService.terminate(securityService.getUserInfoOffline(user), user, project, expName, String.format(AUDIT_MESSAGE, expName));
    }

    private void startSpark(String user, String expName, String compName, String project) {
        log.debug("Starting exploratory {} computational {} for user {} by scheduler", expName, compName, user);
        computationalService.startSparkCluster(securityService.getServiceAccountInfo(user), expName, compName, project, String.format(AUDIT_MESSAGE, expName));
    }

    private boolean shouldClusterBeStarted(DataEngineType sparkCluster, UserComputationalResource compResource) {
        return Objects.nonNull(compResource.getSchedulerData()) && compResource.getSchedulerData().isSyncStartRequired()
                && compResource.getImageName().equals(getDockerImageName(sparkCluster));
    }

    /**
     * Performs bulk updating operation with scheduler data for corresponding to exploratory Spark clusters.
     * All these resources will obtain data which is equal to exploratory's except 'stopping' operation (it will be
     * performed automatically with notebook stopping since Spark resources have such feature).
     *
     * @param user            user's name
     * @param project         project name
     * @param exploratoryName name of exploratory resource
     * @param dto             scheduler job data.
     */
    private void shareSchedulerJobDataToSparkClusters(String user, String project, String exploratoryName, SchedulerJobDTO dto) {
        List<String> correspondingSparkClusters = computationalDAO.getComputationalResourcesWhereStatusIn(user, project,
                singletonList(DataEngineType.SPARK_STANDALONE),
                exploratoryName, STARTING, RUNNING, STOPPING, STOPPED);
        SchedulerJobDTO dtoWithoutStopData = getSchedulerJobWithoutStopData(dto);
        for (String sparkName : correspondingSparkClusters) {
            log.debug("Updating computational resource {} affiliated with exploratory {} for user {} with new " +
                    "scheduler job data {}...", sparkName, exploratoryName, user, dtoWithoutStopData);
            computationalDAO.updateSchedulerDataForComputationalResource(user, project, exploratoryName,
                    sparkName, dtoWithoutStopData);
        }
    }

    private List<SchedulerJobData> getExploratorySchedulersForStopping(OffsetDateTime currentDateTime) {
        return schedulerJobDAO.getExploratorySchedulerDataWithStatus(RUNNING)
                .stream()
                .filter(canSchedulerForStoppingBeApplied(currentDateTime, true))
                .collect(Collectors.toList());
    }

    private List<SchedulerJobData> getExploratorySchedulersForStopping(OffsetDateTime currentDateTime,
                                                                       boolean checkInactivity) {
        final Date clusterMaxInactivityAllowedDate =
                from(LocalDateTime.now().minusMinutes(ALLOWED_INACTIVITY_MINUTES).atZone(systemDefault()).toInstant());
        return schedulerJobDAO.getExploratorySchedulerWithStatusAndClusterLastActivityLessThan(RUNNING,
                clusterMaxInactivityAllowedDate)
                .stream()
                .filter(canSchedulerForStoppingBeApplied(currentDateTime, false)
                        .or(schedulerJobData -> checkInactivity && exploratoryInactivityCondition(schedulerJobData)))
                .collect(Collectors.toList());
    }

    private List<SchedulerJobData> getExploratorySchedulersForStarting(OffsetDateTime currentDateTime) {
        return schedulerJobDAO.getExploratorySchedulerDataWithStatus(STOPPED)
                .stream()
                .filter(canSchedulerForStartingBeApplied(currentDateTime))
                .collect(Collectors.toList());
    }

    private List<SchedulerJobData> getComputationalSchedulersForStarting(OffsetDateTime currentDateTime) {
        return schedulerJobDAO
                .getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED)
                .stream()
                .filter(canSchedulerForStartingBeApplied(currentDateTime))
                .collect(Collectors.toList());
    }

    private Predicate<SchedulerJobData> canSchedulerForStoppingBeApplied(OffsetDateTime currentDateTime, boolean usingOffset) {
        return schedulerJobData -> shouldSchedulerBeExecuted(schedulerJobData.getJobDTO(),
                currentDateTime, schedulerJobData.getJobDTO().getStopDaysRepeat(),
                schedulerJobData.getJobDTO().getEndTime(), usingOffset);
    }

    private Predicate<SchedulerJobData> canSchedulerForStartingBeApplied(OffsetDateTime currentDateTime) {
        return schedulerJobData -> shouldSchedulerBeExecuted(schedulerJobData.getJobDTO(),
                currentDateTime, schedulerJobData.getJobDTO().getStartDaysRepeat(),
                schedulerJobData.getJobDTO().getStartTime(), false);
    }

    private Predicate<SchedulerJobData> canSchedulerForTerminatingBeApplied(OffsetDateTime currentDateTime) {
        return schedulerJobData -> shouldBeTerminated(currentDateTime, schedulerJobData);
    }

    private boolean shouldBeTerminated(OffsetDateTime currentDateTime, SchedulerJobData schedulerJobData) {
        final SchedulerJobDTO jobDTO = schedulerJobData.getJobDTO();
        final ZoneOffset timeZoneOffset = jobDTO.getTimeZoneOffset();
        final LocalDateTime convertedCurrentTime = localDateTimeAtZone(currentDateTime, timeZoneOffset);
        final LocalDateTime terminateDateTime = jobDTO.getTerminateDateTime();
        return Objects.nonNull(terminateDateTime) && isSchedulerActive(jobDTO, convertedCurrentTime) &&
                convertedCurrentTime.equals(terminateDateTime.atOffset(timeZoneOffset).toLocalDateTime());
    }

    private List<SchedulerJobData> getComputationalSchedulersForStopping(OffsetDateTime currentDateTime) {
        return schedulerJobDAO
                .getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING)
                .stream()
                .filter(canSchedulerForStoppingBeApplied(currentDateTime, true))
                .collect(Collectors.toList());
    }

    private List<SchedulerJobData> getComputationalSchedulersForStopping(OffsetDateTime currentDateTime,
                                                                         boolean checkInactivity) {
        return schedulerJobDAO
                .getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING)
                .stream()
                .filter(canSchedulerForStoppingBeApplied(currentDateTime, false)
                        .or(schedulerJobData -> checkInactivity && computationalInactivityCondition(schedulerJobData)))
                .collect(Collectors.toList());
    }

    private boolean computationalInactivityCondition(SchedulerJobData jobData) {
        final SchedulerJobDTO schedulerData = jobData.getJobDTO();
        return schedulerData.isCheckInactivityRequired() && computationalInactivityExceed(jobData, schedulerData);
    }

    private boolean computationalInactivityExceed(SchedulerJobData schedulerJobData, SchedulerJobDTO schedulerData) {
        final String projectName = schedulerJobData.getProject();
        final String explName = schedulerJobData.getExploratoryName();
        final String compName = schedulerJobData.getComputationalName();
        final String user = schedulerJobData.getUser();
        final UserComputationalResource c = computationalDAO.fetchComputationalFields(user, projectName, explName, compName);
        final Long maxInactivity = schedulerData.getMaxInactivity();
        return inactivityCondition(maxInactivity, c.getStatus(), c.getLastActivity());
    }

    private boolean exploratoryInactivityCondition(SchedulerJobData jobData) {
        final SchedulerJobDTO schedulerData = jobData.getJobDTO();
        return schedulerData.isCheckInactivityRequired() && exploratoryInactivityExceed(jobData, schedulerData);
    }

    private boolean exploratoryInactivityExceed(SchedulerJobData schedulerJobData, SchedulerJobDTO schedulerData) {
        final String project = schedulerJobData.getProject();
        final String expName = schedulerJobData.getExploratoryName();
        final String user = schedulerJobData.getUser();
        final UserInstanceDTO userInstanceDTO = exploratoryDAO.fetchExploratoryFields(user, project, expName, true);
        final boolean canBeStopped = userInstanceDTO.getResources()
                .stream()
                .map(UserComputationalResource::getStatus)
                .map(UserInstanceStatus::of)
                .noneMatch(status -> status.in(TERMINATING, CONFIGURING, CREATING, CREATING));
        return canBeStopped && inactivityCondition(schedulerData.getMaxInactivity(), userInstanceDTO.getStatus(),
                userInstanceDTO.getLastActivity());
    }

    private boolean inactivityCondition(Long maxInactivity, String status, LocalDateTime lastActivity) {
        return UserInstanceStatus.RUNNING.toString().equals(status) &&
                Optional.ofNullable(lastActivity)
                        .map(la -> la.plusMinutes(maxInactivity).isBefore(LocalDateTime.now()))
                        .orElse(Boolean.FALSE);
    }

    private void populateDefaultSchedulerValues(SchedulerJobDTO dto) {
        if (Objects.isNull(dto.getBeginDate()) || StringUtils.isBlank(dto.getBeginDate().toString())) {
            dto.setBeginDate(LocalDate.now());
        }
        if (Objects.isNull(dto.getTimeZoneOffset()) || StringUtils.isBlank(dto.getTimeZoneOffset().toString())) {
            dto.setTimeZoneOffset(OffsetDateTime.now(systemDefault()).getOffset());
        }
    }

    private void validateExploratoryStatus(String user, String project, String exploratoryName) {
        final UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(user, project, exploratoryName);
        validateResourceStatus(userInstance.getStatus());
    }

    private void validateComputationalStatus(String user, String project, String exploratoryName, String computationalName) {
        final UserComputationalResource computationalResource =
                computationalDAO.fetchComputationalFields(user, project, exploratoryName, computationalName);
        final String computationalStatus = computationalResource.getStatus();
        validateResourceStatus(computationalStatus);
    }

    private void validateResourceStatus(String resourceStatus) {
        final UserInstanceStatus status = UserInstanceStatus.of(resourceStatus);
        if (Objects.isNull(status) || status.in(UserInstanceStatus.TERMINATED, TERMINATING,
                UserInstanceStatus.FAILED)) {
            throw new ResourceInappropriateStateException(String.format("Can not create/update scheduler for user " +
                    "instance with status: %s", status));
        }
    }

    private boolean shouldSchedulerBeExecuted(SchedulerJobDTO dto, OffsetDateTime dateTime, List<DayOfWeek> daysRepeat,
                                              LocalTime time, boolean usingOffset) {
        ZoneOffset timeZoneOffset = dto.getTimeZoneOffset();
        LocalDateTime convertedDateTime = localDateTimeAtZone(dateTime, timeZoneOffset);
        return isSchedulerActive(dto, convertedDateTime)
                && daysRepeat.contains(convertedDateTime.toLocalDate().getDayOfWeek())
                && timeFilter(time, convertedDateTime.toLocalTime(), timeZoneOffset, usingOffset);
    }

    private boolean timeFilter(LocalTime time, LocalTime convertedDateTime, ZoneOffset timeZoneOffset, boolean usingOffset) {
        return usingOffset ? (time.isBefore(convertedDateTime) && time.isAfter(LocalDateTime.now(timeZoneOffset).toLocalTime())) :
                convertedDateTime.equals(time);
    }

    private boolean isSchedulerActive(SchedulerJobDTO dto, LocalDateTime convertedDateTime) {
        return !convertedDateTime.toLocalDate().isBefore(dto.getBeginDate())
                && finishDateAfterCurrentDate(dto, convertedDateTime);
    }

    private LocalDateTime localDateTimeAtZone(OffsetDateTime dateTime, ZoneOffset timeZoneOffset) {
        return dateTime.atZoneSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MINUTES)
                .withZoneSameInstant(timeZoneOffset)
                .toLocalDateTime();
    }

    private boolean finishDateAfterCurrentDate(SchedulerJobDTO dto, LocalDateTime currentDateTime) {
        return Objects.isNull(dto.getFinishDate()) || !currentDateTime.toLocalDate().isAfter(dto.getFinishDate());
    }

    private SchedulerJobDTO getSchedulerJobWithoutStopData(SchedulerJobDTO dto) {
        SchedulerJobDTO convertedDto = new SchedulerJobDTO();
        convertedDto.setBeginDate(dto.getBeginDate());
        convertedDto.setFinishDate(dto.getFinishDate());
        convertedDto.setStartTime(dto.getStartTime());
        convertedDto.setStartDaysRepeat(dto.getStartDaysRepeat());
        convertedDto.setTerminateDateTime(dto.getTerminateDateTime());
        convertedDto.setTimeZoneOffset(dto.getTimeZoneOffset());
        convertedDto.setSyncStartRequired(dto.isSyncStartRequired());
        return convertedDto;
    }

}

