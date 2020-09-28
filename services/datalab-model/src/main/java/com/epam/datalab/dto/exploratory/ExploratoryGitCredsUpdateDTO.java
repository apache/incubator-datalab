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
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.List;

/**
 * Store GIT credentials which should be updated on exploratory.
 */
public class ExploratoryGitCredsUpdateDTO extends ExploratoryActionDTO<ExploratoryGitCredsUpdateDTO> {
    @JsonProperty("git_creds")
    private List<ExploratoryGitCreds> gitCreds;

    /**
     * Return the list of GIT credentials.
     */
    public List<ExploratoryGitCreds> getGitCreds() {
        return gitCreds;
    }

    /**
     * Set the list of GIT credentials.
     */
    public void setGitCreds(List<ExploratoryGitCreds> gitCreds) {
        this.gitCreds = gitCreds;
    }

    /**
     * Set the list of GIT credentials and return this object.
     */
    public ExploratoryGitCredsUpdateDTO withGitCreds(List<ExploratoryGitCreds> gitCreds) {
        setGitCreds(gitCreds);
        return this;
    }


    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("gitCreds", gitCreds);
    }
}
