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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.model.scheduler.SchedulerJobData;

import java.util.List;

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

	void stopComputationalByScheduler();

	void stopExploratoryByScheduler();

	void startExploratoryByScheduler();

	void startComputationalByScheduler();

	void terminateExploratoryByScheduler();

	void terminateComputationalByScheduler();

	void removeScheduler(String user, String exploratoryName);

	void removeScheduler(String user, String exploratoryName, String computationalName);

	List<SchedulerJobData> getActiveSchedulers(String user, long timeOffset);
}
