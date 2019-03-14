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

package com.epam.dlab.billing.gcp.documents;

import com.epam.dlab.billing.gcp.model.BillingData;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "userInstances")
@Data
public class UserInstance {

    @Id
    private String id;
    @Field("user")
    private String user;
    @Field("exploratory_name")
    private String exploratoryName;
    @Field("exploratory_id")
    private String exploratoryId;
    private List<BillingData> billing;
    private Double cost;
    @Field("computational_resources")
    private List<ComputationalResource> computationalResources;

    @Data
    public class ComputationalResource {
        @Field("computational_name")
        private String computationalName;
        @Field("computational_id")
        private String computationalId;
    }
}
