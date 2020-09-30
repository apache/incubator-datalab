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

package com.epam.datalab.backendapi.validation;

import com.epam.datalab.backendapi.validation.annotation.SchedulerJobDTOValid;
import com.epam.datalab.dto.SchedulerJobDTO;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

public class SchedulerJobDTOValidator implements ConstraintValidator<SchedulerJobDTOValid, SchedulerJobDTO> {
    @Override
    public void initialize(SchedulerJobDTOValid schedulerJobDTOValid) {
        //do nothing
    }

    @Override
    public boolean isValid(SchedulerJobDTO schedulerJobDTO, ConstraintValidatorContext constraintValidatorContext) {
        if (!schedulerJobDTO.isCheckInactivityRequired() && Objects.isNull(schedulerJobDTO.getTerminateDateTime())) {
            return !schedulerJobDTO.getStartDaysRepeat().isEmpty() || !schedulerJobDTO.getStopDaysRepeat().isEmpty();
        } else if (schedulerJobDTO.isCheckInactivityRequired() && Objects.isNull(schedulerJobDTO.getMaxInactivity())) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Max inactivity time should be set").addConstraintViolation();
            return false;
        } else {
            return true;
        }
    }
}
