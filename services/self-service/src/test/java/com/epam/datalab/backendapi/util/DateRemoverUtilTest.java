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

package com.epam.datalab.backendapi.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateRemoverUtilTest {

    @Test
    public void removeDateFormErrorMessageWithErrorDateFormat() {
        String errorMessage = "All dates with format '[Error-2018-04-12 15:30:35]:' are erroneous";
        String expected = "All dates with format 'yyyy-MM-dd' are erroneous";
        String actual = DateRemoverUtil.removeDateFormErrorMessage(errorMessage, "\\[Error-\\d{4}-\\d{2}-" +
                "\\d{2} \\d{2}:\\d{2}:\\d{2}\\]:", "yyyy-MM-dd");
        assertEquals(expected, actual);
    }

    @Test
    public void removeDateFormErrorMessage1() {
        String errorMessage = "All dates with format '[Error-2018-04-12 15:30:35]:' are erroneous";
        String expected = "All dates with format '[Error]:' are erroneous";
        String actual = DateRemoverUtil.removeDateFormErrorMessage(errorMessage);
        assertEquals(expected, actual);
    }
}