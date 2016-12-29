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

package com.epam.dlab.dto.keyload;

import com.epam.dlab.dto.edge.EdgeCreateDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadFileDTO {
    @JsonProperty
    private EdgeCreateDTO edge;
    @JsonProperty
    private String content;

    public UploadFileDTO() {
    }

    public EdgeCreateDTO getEdge() {
        return edge;
    }

    public void setEdge(EdgeCreateDTO edge) {
        this.edge = edge;
    }

    public UploadFileDTO withEdge(EdgeCreateDTO edge) {
        setEdge(edge);
        return this;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UploadFileDTO withContent(String content) {
        setContent(content);
        return this;
    }

}
