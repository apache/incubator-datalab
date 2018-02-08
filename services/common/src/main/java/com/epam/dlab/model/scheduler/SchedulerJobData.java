package com.epam.dlab.model.scheduler;

import com.epam.dlab.dto.SchedulerJobDTO;
import lombok.Data;

@Data
public class SchedulerJobData {
    private final String user;
    private final String exploratoryName;
    private final SchedulerJobDTO jobDTO;
}

