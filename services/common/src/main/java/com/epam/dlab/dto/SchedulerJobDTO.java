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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores info about a scheduler job (general duration, days to repeat, time to start and finish).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulerJobDTO {
    @JsonProperty("begin_date")
    private LocalDate beginDate;
    @JsonProperty("finish_date")
    private LocalDate finishDate;
    @JsonProperty("start_time")
    private LocalTime startTime;
    @JsonProperty("end_time")
    private LocalTime endTime;
    @JsonProperty("days_repeat")
    private List<DayOfWeek> daysRepeat = new ArrayList<>();

    /**
     * Sets the date from which scheduler job will work (general duration).
     */
    public SchedulerJobDTO withBeginDate(LocalDate date) {
        setBeginDate(date);
        return this;
    }

    /**
     * Sets the date to which scheduler job will work (general duration).
     */
    public SchedulerJobDTO withFinishDate(LocalDate date) {
        setFinishDate(date);
        return this;
    }

    /**
     * Sets the time of day from which scheduler job will start (local duration).
     */
    public SchedulerJobDTO withStartTime(LocalTime time) throws ParseException {
        setStartTime(time);
        return this;
    }

    /**
     * Sets the time of day to which scheduler job must finish (local duration).
     */
    public SchedulerJobDTO withEndTime(LocalTime time) {
        setEndTime(time);
        return this;
    }


    /**
     * Sets the days of week for scheduler repeating.
     */
    public SchedulerJobDTO withDaysRepeat(List<DayOfWeek> days) {
        setDaysRepeat(days);
        return this;
    }

}