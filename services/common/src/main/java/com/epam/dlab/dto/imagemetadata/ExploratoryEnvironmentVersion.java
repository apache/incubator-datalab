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


package com.epam.dlab.dto.imagemetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ExploratoryEnvironmentVersion {
    @JsonProperty("template_name")
    private String templateName;
    @JsonProperty
    private String description;
    @JsonProperty("environment_type")
    private String type;
    @JsonProperty("version")
    private String version;
    @JsonProperty("vendor")
    private String vendor;

    public ExploratoryEnvironmentVersion() {
    }

    public ExploratoryEnvironmentVersion(String templateName, String description, String type, String version,
                                         String vendor) {
        this.templateName = templateName;
        this.description = description;
        this.type = type;
        this.version = version;
        this.vendor = vendor;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
