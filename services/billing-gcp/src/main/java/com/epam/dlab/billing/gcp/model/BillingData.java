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

package com.epam.dlab.billing.gcp.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Data
@Builder
@Document(collection = "billing")
public class BillingData {
    @Id
    private String id;
    private String user;
    @Field("resource_name")
    private String displayName;
    private String resourceName;
    @Field("usage_date_start")
    private LocalDate usageDateFrom;
    @Field("usage_date_end")
    private LocalDate usageDateTo;
    @Field("usage_date")
    private String usageDate;
    private String product;
    private String usageType;
    private Double cost;
    @Field("currency_code")
    private String currency;
    private String exploratoryName;
    private String computationalName;
    @Field("dlab_id")
    private String dlabId;
    @Field("dlab_resource_type")
    private ResourceType resourceType;


    public enum ResourceType {
        EDGE,
        SSN,
        SHARED_BUCKET,
        SSN_BUCKET,
        EDGE_BUCKET,
        VOLUME,
        EXPLORATORY,
        COMPUTATIONAL
    }
}
