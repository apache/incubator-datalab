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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExploratoryMetadataDTO extends ImageMetadataDTO {
    @JsonProperty(value = "exploratory_environment_versions")
    private List<ExploratoryEnvironmentVersion> exploratoryEnvironmentVersions;
    @JsonProperty(value = "exploratory_environment_shapes")
    private List<ComputationalResourceShapeDto> exploratoryEnvironmentShapes;
    @JsonProperty
    protected String image;
    @JsonProperty(value = "request_id")
    private String requestId;

    public ExploratoryMetadataDTO(String imageName) {
        this.image = imageName;
        setImageType(ImageType.EXPLORATORY);
    }

    public ExploratoryMetadataDTO() {
        this("");
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }


    public List<ExploratoryEnvironmentVersion> getExploratoryEnvironmentVersions() {
        return exploratoryEnvironmentVersions;
    }

    public void setExploratoryEnvironmentVersions(
            List<ExploratoryEnvironmentVersion> exploratoryEnvironmentVersions) {
        this.exploratoryEnvironmentVersions = exploratoryEnvironmentVersions;
    }

    public List<ComputationalResourceShapeDto> getExploratoryEnvironmentShapes() {
        return exploratoryEnvironmentShapes;
    }

    public void setExploratoryEnvironmentShapes(
            List<ComputationalResourceShapeDto> exploratoryEnvironmentShapes) {
        this.exploratoryEnvironmentShapes = exploratoryEnvironmentShapes;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExploratoryMetadataDTO that = (ExploratoryMetadataDTO) o;

        if (exploratoryEnvironmentVersions != null
                ? !exploratoryEnvironmentVersions
                .equals(that.exploratoryEnvironmentVersions)
                : that.exploratoryEnvironmentVersions != null) {
            return false;
        }
        if (exploratoryEnvironmentShapes != null ? !exploratoryEnvironmentShapes
                .equals(that.exploratoryEnvironmentShapes)
                : that.exploratoryEnvironmentShapes != null) {
            return false;
        }
        if (image != null ? !image.equals(that.image) : that.image != null) {
            return false;
        }
        return requestId != null ? requestId.equals(that.requestId)
                : that.requestId == null;
    }

    @Override
    public int hashCode() {
        int result = exploratoryEnvironmentVersions != null
                ? exploratoryEnvironmentVersions.hashCode() : 0;
        result = 31 * result + (exploratoryEnvironmentShapes != null
                ? exploratoryEnvironmentShapes.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
        return result;
    }
}
