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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.dto.base.DataEngineType;
import com.google.inject.Inject;
import com.mongodb.client.FindIterable;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.dlab.backendapi.dao.ComputationalDAO.COMPUTATIONAL_ID;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.COMPUTATIONAL_RESOURCES;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_ID;
import static com.mongodb.client.model.Projections.*;

@Slf4j
public abstract class BillingDAO extends BaseDAO {

    public static final String SHAPE = "shape";
    public static final String SERVICE_BASE_NAME = "service_base_name";
    public static final String ITEMS = "lines";
    public static final String COST_TOTAL = "cost_total";
    public static final String FULL_REPORT = "full_report";

    private static final String MASTER_NODE_SHAPE = "master_node_shape";
    private static final String SLAVE_NODE_SHAPE = "slave_node_shape";
    private static final String TOTAL_INSTANCE_NUMBER = "total_instance_number";

    private static final String DATAENGINE_SLAVE = "dataengine_slave";
    private static final String DATAENGINE_MASTER = "dataengine_master";
    private static final String DATAENGINE_INSTANCE_COUNT = "dataengine_instance_count";

    private static final String DATAENGINE_DOCKER_IMAGE = "image";

    @Inject
    protected SettingsDAO settings;

    protected Map<String, BillingDAO.ShapeInfo> getShapes(List<String> shapeNames) {
        FindIterable<Document> docs = getCollection(USER_INSTANCES)
                .find()
                .projection(
                        fields(excludeId(),
                                include(SHAPE, EXPLORATORY_ID,
                                        COMPUTATIONAL_RESOURCES + "." + COMPUTATIONAL_ID,
                                        COMPUTATIONAL_RESOURCES + "." + MASTER_NODE_SHAPE,
                                        COMPUTATIONAL_RESOURCES + "." + SLAVE_NODE_SHAPE,
                                        COMPUTATIONAL_RESOURCES + "." + TOTAL_INSTANCE_NUMBER,
                                        COMPUTATIONAL_RESOURCES + "." + DATAENGINE_MASTER,
                                        COMPUTATIONAL_RESOURCES + "." + DATAENGINE_SLAVE,
                                        COMPUTATIONAL_RESOURCES + "." + DATAENGINE_INSTANCE_COUNT,
                                        COMPUTATIONAL_RESOURCES + "." + DATAENGINE_DOCKER_IMAGE
                                )));
        Map<String, BillingDAO.ShapeInfo> shapes = new HashMap<>();
        for (Document d : docs) {
            String name = d.getString(SHAPE);
            if (shapeNames == null || shapeNames.isEmpty() || shapeNames.contains(name)) {
                shapes.put(d.getString(EXPLORATORY_ID), new BillingDAO.ShapeInfo(d.getString(SHAPE)));
            }

            @SuppressWarnings("unchecked")
            List<Document> comp = (List<Document>) d.get(COMPUTATIONAL_RESOURCES);
            for (Document c : comp) {
                if (DataEngineType.fromDockerImageName(c.getString(DATAENGINE_DOCKER_IMAGE)) == DataEngineType.SPARK_STANDALONE) {
                    name = c.getString(DATAENGINE_MASTER);
                    String slaveName = c.getString(DATAENGINE_SLAVE);
                    if (shapeNames == null || shapeNames.isEmpty() ||
                            shapeNames.contains(name) || shapeNames.contains(slaveName)) {
                        shapes.put(c.getString(COMPUTATIONAL_ID),
                                new BillingDAO.ShapeInfo(name, slaveName, c.getString(DATAENGINE_INSTANCE_COUNT)));
                    }
                } else {
                    name = c.getString(MASTER_NODE_SHAPE);
                    String slaveName = c.getString(SLAVE_NODE_SHAPE);
                    if (shapeNames == null || shapeNames.isEmpty() ||
                            shapeNames.contains(name) || shapeNames.contains(slaveName)) {
                        shapes.put(c.getString(COMPUTATIONAL_ID),
                                new BillingDAO.ShapeInfo(name, slaveName, c.getString(TOTAL_INSTANCE_NUMBER)));
                    }
                }
            }
        }

        appendSsnAndEdgeNodeType(shapeNames, shapes);

        log.trace("Loaded shapes is {}", shapes);
        return shapes;
    }

    protected abstract void appendSsnAndEdgeNodeType(List<String> shapeNames, Map<String, BillingDAO.ShapeInfo> shapes);

    protected String generateShapeName(ShapeInfo shape) {

        String shapeName = "";

        if (shape != null) {
            if (shape.isExploratory()) {
                return shape.getShape();
            } else {
                try {
                    // Total number of instances. Slave instances equals total minus one
                    int count = Integer.parseInt(shape.getSlaveCount());
                    return String.format("Master: %s%sSlave:  %dx%s", shape.getShape(), System.lineSeparator(),
                            count - 1, shape.getSlaveShape());
                } catch (NumberFormatException e) {
                    log.error("Cannot parse string {} to integer", shape.getSlaveCount());
                    return shapeName;
                }
            }
        }

        return shapeName;
    }

    /**
     * Store shape info
     */
    @ToString
    @Getter
    protected class ShapeInfo {
        private String shape;
        private String slaveShape;
        private String slaveCount;
        private boolean isExploratory;

        public ShapeInfo(String shape) {
            this.shape = shape;
            this.isExploratory = true;
        }

        public ShapeInfo(String shape, String slaveShape, String slaveCount) {
            this.shape = shape;
            this.slaveShape = slaveShape;
            this.slaveCount = slaveCount;
            this.isExploratory = false;
        }
    }
}
