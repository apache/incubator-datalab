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

package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores the info about image libraries.
 */
@Getter
@Setter
public class LibListStatusDTO extends StatusBaseDTO<LibListStatusDTO> {

    @JsonProperty
    private String libs;

    @JsonProperty
    private String imageName;

    /**
     * Set the list of libraries and return this object
     */
    public LibListStatusDTO withLibs(String libs) {
        setLibs(libs);
        return this;
    }

    /**
     * Set the name of image and return this object
     */
    public LibListStatusDTO withImageName(String imageName) {
        setImageName(imageName);
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("imageName", imageName)
                .add("libs", (libs != null) ? "..." : null);
    }
}
