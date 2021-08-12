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

package com.epam.datalab.backendapi.service;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.LibInfoRecord;
import com.epam.datalab.dto.exploratory.LibInstallDTO;
import org.bson.Document;

import java.util.List;

public interface LibraryService {
    List<Document> getLibs(String user, String project, String exploratoryName, String computationalName);

    List<LibInfoRecord> getLibInfo(String user, String project, String exploratoryName);

    String installComputationalLibs(UserInfo userInfo, String project, String exploratoryName, String computationalName,
                                    List<LibInstallDTO> libs, String auditInfo);

    String installExploratoryLibs(UserInfo userInfo, String project, String exploratoryName, List<LibInstallDTO> libs, String auditInfo);

    List<String> getExploratoryLibGroups(UserInfo userInfo, String projectName, String exploratoryName);

    List<String> getComputeLibGroups();
}
