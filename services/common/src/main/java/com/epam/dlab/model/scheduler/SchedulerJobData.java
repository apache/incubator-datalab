package com.epam.dlab.model.scheduler;

import com.epam.dlab.dto.SchedulerJobDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SchedulerJobData {

	@JsonProperty
    private final String user;

	@JsonProperty("exploratory_name")
    private final String exploratoryName;

	@JsonProperty("computational_name")
	private final String computationalName;

	@JsonProperty("scheduler_data")
    private final SchedulerJobDTO jobDTO;
}

