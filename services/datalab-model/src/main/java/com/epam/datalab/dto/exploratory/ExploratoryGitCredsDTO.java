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

package com.epam.datalab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Collections;
import java.util.List;

/**
 * Stores info about the GIT credentials.
 */
public class ExploratoryGitCredsDTO {
    @JsonProperty("git_creds")
    private List<ExploratoryGitCreds> gitCreds;

    /**
     * Return the list of GIT credentials.
     */
    public List<ExploratoryGitCreds> getGitCreds() {
        return gitCreds;
    }

    /**
     * Set the list of GIT credentials and check the unique for host names.
     */
    public void setGitCreds(List<ExploratoryGitCreds> gitCreds) {
        if (gitCreds != null) {
            Collections.sort(gitCreds);
            for (int i = 1; i < gitCreds.size(); i++) {
                if (gitCreds.get(i).equals(gitCreds.get(i - 1))) {
                    throw new IllegalArgumentException("Duplicate found for host name in git credentials: " + gitCreds.get(i).getHostname());
                }
            }
        }
        this.gitCreds = gitCreds;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("gitCreds", gitCreds)
                .toString();
    }
}
