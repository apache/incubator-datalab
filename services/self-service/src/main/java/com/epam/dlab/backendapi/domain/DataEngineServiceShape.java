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

package com.epam.dlab.backendapi.domain;

import com.epam.dlab.backendapi.service.ShapeFormat;
import com.epam.dlab.dto.UserInstanceStatus;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;


@Slf4j
public class DataEngineServiceShape extends BaseShape implements ShapeFormat {
    private static final String DES_NAME_FORMAT = "Master: %s%sSlave:  %d x %s";
    private String slaveCount;
    private String slaveShape;

    @Builder
    public DataEngineServiceShape(String shape, UserInstanceStatus status, String slaveCount, String slaveShape,
                                  Map<String, String> tags) {
        super(shape, status, tags);
        this.slaveCount = slaveCount;
        this.slaveShape = slaveShape;
    }

    @Override
    public String format() {
        Integer count;
        try {
            count = Integer.valueOf(slaveCount);
        } catch (NumberFormatException e) {
            log.error("Cannot parse string {} to integer", slaveCount);
            return StringUtils.EMPTY;
        }
        return String.format(DES_NAME_FORMAT, shape, System.lineSeparator(), count - 1, slaveShape);
    }
}
