/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.dto.imagemetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Common parent for metadata DTO. Holds type information during
 * runtime to make life easier when working with collection of metadatas or
 * filtering by type. Shouldnt be used to hold common attributes for upstream
 * hierarchy as it will requite type information to be serialized within json
 * which is not we really want.
 */
public abstract class ImageMetadataDTO {
    @JsonProperty("image_type")
    private ImageType imageType;

    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    public abstract void setImage(String image);

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
