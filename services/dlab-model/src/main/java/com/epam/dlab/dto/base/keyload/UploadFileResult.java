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

package com.epam.dlab.dto.base.keyload;

import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.google.common.base.MoreObjects;

public class UploadFileResult<T extends EdgeInfo> extends StatusBaseDTO<UploadFileResult<T>> {

    private T edgeInfo;

    public T getEdgeInfo() {
        return edgeInfo;
    }

    public UploadFileResult<T> withEdgeInfo(T edgeInfo) {
        this.edgeInfo = edgeInfo;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("edgeInfo", edgeInfo);
    }


}
