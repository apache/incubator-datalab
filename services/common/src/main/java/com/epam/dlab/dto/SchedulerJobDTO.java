/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.*;
import java.util.Collections;
import java.util.List;

/**
 * Stores info about a scheduler job (general duration, days to repeat, time to start and finish).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulerJobDTO {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@JsonProperty("begin_date")
	private LocalDate beginDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@JsonProperty("finish_date")
	private LocalDate finishDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	@JsonProperty("start_time")
	private LocalTime startTime;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	@JsonProperty("end_time")
	private LocalTime endTime;

	@JsonProperty("days_repeat")
	private List<DayOfWeek> daysRepeat = Collections.emptyList();

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	@JsonProperty("terminate_datetime")
	private LocalDateTime terminateDateTime;

	@JsonProperty("timezone_offset")
	private ZoneOffset timeZoneOffset;

	@JsonProperty("sync_start_required")
	private boolean syncStartRequired = true;

}