package com.epam.dlab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import lombok.Data;

@Data
public class ExploratoryImageDTO extends ExploratoryActionDTO<ExploratoryImageDTO> {

    @JsonProperty("notebook_image_name")
    private String imageName;

    public ExploratoryImageDTO withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("imageName", imageName);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
