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

package com.epam.datalab.automation.test.libs;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

abstract class TestLibStep {
    final String url;
    final String token;
    final String notebookName;
    final long initTimeoutSec; //seconds

    TestLibStep(String url, String token, String notebookName, long initTimeoutSec) {
        this.url = url;
        this.token = token;
        this.notebookName = notebookName;
        this.initTimeoutSec = initTimeoutSec;
    }

    public abstract void verify();

    String getDescription() {
        Annotation annotation = getClass().getAnnotation(TestDescription.class);
        return (annotation != null) ? ((TestDescription) annotation).value() : "";
    }

    public void init() throws InterruptedException {
        if (initTimeoutSec != 0L) {
            TimeUnit.SECONDS.sleep(initTimeoutSec);
        }
    }
}
