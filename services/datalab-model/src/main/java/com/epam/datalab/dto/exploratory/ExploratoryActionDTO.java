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
import com.google.common.base.MoreObjects;

public class ExploratoryActionDTO<T extends ExploratoryActionDTO<?>> extends ExploratoryBaseDTO<T> {
    @JsonProperty("notebook_instance_name")
    private String notebookInstanceName;

    @JsonProperty("reupload_key_required")
    private boolean reuploadKeyRequired;

    public boolean isReuploadKeyRequired() {
        return reuploadKeyRequired;
    }

    public void setReuploadKeyRequired(boolean reuploadKeyRequired) {
        this.reuploadKeyRequired = reuploadKeyRequired;
    }

    @SuppressWarnings("unchecked")
    public T withReuploadKeyRequired(boolean reuploadKeyRequired) {
        setReuploadKeyRequired(reuploadKeyRequired);
        return (T) this;
    }

    public String getNotebookInstanceName() {
        return notebookInstanceName;
    }

    public void setNotebookInstanceName(String notebookInstanceName) {
        this.notebookInstanceName = notebookInstanceName;
    }

    @SuppressWarnings("unchecked")
    public T withNotebookInstanceName(String notebookInstanceName) {
        setNotebookInstanceName(notebookInstanceName);
        return (T) this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("notebookInstanceName", notebookInstanceName);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
