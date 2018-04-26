package com.epam.dlab.backendapi.service;

import com.epam.dlab.dto.SchedulerJobDTO;

public interface SchedulerJobService {
	/**
	 * Pulls out scheduler job data for user <code>user<code/> and his exploratory <code>exploratoryName<code/>
	 *
	 * @param user            user's name
	 * @param exploratoryName name of exploratory resource
	 * @return dto object
	 */
	SchedulerJobDTO fetchSchedulerJobForUserAndExploratory(String user, String exploratoryName);

	/**
	 * Pulls out scheduler job data for computational resource <code>computationalName<code/> affiliated with
	 * user <code>user<code/> and his exploratory <code>exploratoryName<code/>
	 *
	 * @param user              user's name
	 * @param exploratoryName   name of exploratory resource
	 * @param computationalName name of computational resource
	 * @return dto object
	 */
	SchedulerJobDTO fetchSchedulerJobForComputationalResource(String user, String exploratoryName,
															  String computationalName);

	/**
	 * Updates scheduler job data for user <code>user<code/> and his exploratory <code>exploratoryName<code/>
	 *
	 * @param user            user's name
	 * @param exploratoryName name of exploratory resource
	 * @param dto             scheduler job data
	 */
	void updateExploratorySchedulerData(String user, String exploratoryName, SchedulerJobDTO dto);

	/**
	 * Updates scheduler job data for computational resource <code>computationalName<code/> affiliated with
	 * user <code>user<code/> and his exploratory <code>exploratoryName<code/>
	 *
	 * @param user              user's name
	 * @param exploratoryName   name of exploratory resource
	 * @param computationalName name of computational resource
	 * @param dto               scheduler job data
	 */
	void updateComputationalSchedulerData(String user, String exploratoryName,
										  String computationalName, SchedulerJobDTO dto);

	/**
	 * Executes start scheduler job for corresponding exploratories ('isAppliedForClusters' equals 'false') or
	 * computational resources ('isAppliedForClusters' equals 'true').
	 *
	 * @param isAppliedForClusters true/false
	 */
	void executeStartResourceJob(boolean isAppliedForClusters);

	/**
	 * Executes stop scheduler job for corresponding exploratories ('isAppliedForClusters' equals 'false') or
	 * computational resources ('isAppliedForClusters' equals 'true').
	 *
	 * @param isAppliedForClusters true/false
	 */
	void executeStopResourceJob(boolean isAppliedForClusters);

	/**
	 * Executes terminate scheduler job for corresponding exploratories ('isAppliedForClusters' equals 'false') or
	 * computational resources ('isAppliedForClusters' equals 'true').
	 *
	 * @param isAppliedForClusters true/false
	 */
	void executeTerminateResourceJob(boolean isAppliedForClusters);
}
