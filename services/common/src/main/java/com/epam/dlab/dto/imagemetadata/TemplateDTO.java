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

import java.util.List;
import java.util.Objects;

public class TemplateDTO {
    @JsonProperty
    private String version;
    @JsonProperty
    private List<ApplicationDto> applications;

    public TemplateDTO() {
    }

    public TemplateDTO(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ApplicationDto> getApplications() {
        return applications;
    }

    public void setApplications(List<ApplicationDto> applications) {
        this.applications = applications;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TemplateDTO that = (TemplateDTO) o;

        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }
        return applications != null ? applications.equals(that.applications) : that.applications == null;

    }

    @Override
    public int hashCode() {
        return Objects.hash(version, applications);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
