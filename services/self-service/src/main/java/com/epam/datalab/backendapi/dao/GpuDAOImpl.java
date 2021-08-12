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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.dto.imagemetadata.EdgeGPU;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class GpuDAOImpl extends BaseDAO implements GpuDAO {

    private static final String GPU_COLLECTION = "gpuTypes";
    private static final String PROJECT_NAME_FIELD = "projectName";

    @Override
    public Optional<EdgeGPU> getGPUByProjectName(String edgeId) {
        return findOne(GPU_COLLECTION, getByIdCondition(edgeId), EdgeGPU.class);
    }

    @Override
    public void create(EdgeGPU gpu) {
        insertOne(GPU_COLLECTION, gpu);
    }

    @Override
    public void createAll(List<Object> gpus) {
        insertMany(GPU_COLLECTION, gpus);
    }

    @Override
    public void remove(String edgeId) {
        deleteOne(GPU_COLLECTION, getByIdCondition(edgeId));
    }

    private Bson getByIdCondition(String edgeId) {
        return eq(PROJECT_NAME_FIELD, edgeId);
    }
}
