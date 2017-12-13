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

package com.epam.dlab.dto.azure.edge;

import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeInfoAzure extends EdgeInfo {
    @JsonProperty("user_storage_account_name")
    private String userStorageAccountName;
    @JsonProperty("user_container_name")
    private String userContainerName;
    @JsonProperty("shared_storage_account_name")
    private String sharedStorageAccountName;
    @JsonProperty("shared_container_name")
    private String sharedContainerName;
    @JsonProperty("user_storage_account_tag_name")
    private String userStorageAccountTagName;
    @JsonProperty("datalake_name")
    private String dataLakeName;
    @JsonProperty("datalake_user_directory_name")
    private String dataLakeDirectoryName;
    @JsonProperty("datalake_shared_directory_name")
    private String dataLakeSharedDirectoryName;
}
