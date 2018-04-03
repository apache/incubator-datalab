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
	 * Updates scheduler job data for user <code>user<code/> and his exploratory <code>exploratoryName<code/>
	 *
	 * @param user            user's name
	 * @param exploratoryName name of exploratory resource
	 * @param dto             scheduler job data
	 */
	void updateSchedulerDataForUserAndExploratory(String user, String exploratoryName, SchedulerJobDTO dto);

	/**
	 * Executes start scheduler job for corresponding exploratories
	 */
	void executeStartExploratoryJob();

	/**
	 * Executes stop scheduler job for corresponding exploratories
	 */
	void executeStopExploratoryJob();

	/**
	 * Executes terminate scheduler job for corresponding exploratories
	 */
	void executeTerminateExploratoryJob();
}
