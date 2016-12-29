/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExploratoryStatusDTO extends StatusBaseDTO<ExploratoryStatusDTO> {
    @JsonProperty("exploratory_id")
    private String exploratoryId;
    @JsonProperty("exploratory_url")
    private String exploratoryUrl;
    @JsonProperty("exploratory_user")
    private String exploratoryUser;
    @JsonProperty("exploratory_pass")
    private String exploratoryPassword;

    public String getExploratoryId() {
        return exploratoryId;
    }

    public void setExploratoryId(String exploratoryId) {
        this.exploratoryId = exploratoryId;
    }

    public ExploratoryStatusDTO withExploratoryId(String exploratoryId) {
        setExploratoryId(exploratoryId);
        return this;
    }

    public String getExploratoryUrl() {
        return exploratoryUrl;
    }

    public void setExploratoryUrl(String exploratoryUrl) {
        this.exploratoryUrl = exploratoryUrl;
    }

    public ExploratoryStatusDTO withExploratoryUrl(String exploratoryUrl) {
        setExploratoryUrl(exploratoryUrl);
        return this;
    }

    public String getExploratoryUser() { return exploratoryUser; }

    public void setExploratoryUser(String exploratoryUser) { this.exploratoryUser = exploratoryUser; }

    public ExploratoryStatusDTO withExploratoryUser(String exploratoryUser) {
        setExploratoryUser(exploratoryUser);
        return this;
    }

    public String getExploratoryPassword() { return exploratoryPassword; }

    public void setExploratoryPassword(String exploratoryPassword) { this.exploratoryPassword = exploratoryPassword; }

    public ExploratoryStatusDTO withExploratoryPassword(String exploratoryPassword) {
        setExploratoryPassword(exploratoryPassword);
        return this;
    }
}
