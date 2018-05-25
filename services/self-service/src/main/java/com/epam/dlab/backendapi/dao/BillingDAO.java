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
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.util.*;

import static com.epam.dlab.backendapi.dao.ComputationalDAO.COMPUTATIONAL_ID;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.COMPUTATIONAL_RESOURCES;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_ID;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
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

	private static final String DATAENGINE_SHAPE = "dataengine_instance_shape";
	private static final String DATAENGINE_INSTANCE_COUNT = "dataengine_instance_count";

	private static final String DATAENGINE_DOCKER_IMAGE = "image";

	@Inject
	protected SettingsDAO settings;

	protected Map<String, BillingDAO.ShapeInfo> getShapes(List<String> shapeNames) {
		FindIterable<Document> userInstances = getUserInstances();
		final Map<String, BillingDAO.ShapeInfo> shapes = new HashMap<>();

		for (Document d : userInstances) {
			getExploratoryShape(shapeNames, d)
					.ifPresent(shapeInfo -> shapes.put(d.getString(EXPLORATORY_ID), shapeInfo));
			@SuppressWarnings("unchecked")
			List<Document> comp = (List<Document>) d.get(COMPUTATIONAL_RESOURCES);
			comp.forEach(computational ->
					getComputationalShape(shapeNames, computational)
							.ifPresent(shapeInfo -> shapes.put(computational.getString(COMPUTATIONAL_ID), shapeInfo)));
		}

		appendSsnAndEdgeNodeType(shapeNames, shapes);

		log.trace("Loaded shapes is {}", shapes);
		return shapes;
	}

	private Optional<ShapeInfo> getComputationalShape(List<String> shapeNames, Document c) {
		return isDataEngine(c.getString(DATAENGINE_DOCKER_IMAGE)) ? getDataEngineShape(shapeNames, c) :
				getDataEngineServiceShape(shapeNames, c);
	}

	private FindIterable<Document> getUserInstances() {
		return getCollection(USER_INSTANCES)
				.find()
				.projection(
						fields(excludeId(),
								include(SHAPE, EXPLORATORY_ID,
										COMPUTATIONAL_RESOURCES + "." + COMPUTATIONAL_ID,
										COMPUTATIONAL_RESOURCES + "." + MASTER_NODE_SHAPE,
										COMPUTATIONAL_RESOURCES + "." + SLAVE_NODE_SHAPE,
										COMPUTATIONAL_RESOURCES + "." + TOTAL_INSTANCE_NUMBER,
										COMPUTATIONAL_RESOURCES + "." + DATAENGINE_SHAPE,
										COMPUTATIONAL_RESOURCES + "." + DATAENGINE_INSTANCE_COUNT,
										COMPUTATIONAL_RESOURCES + "." + DATAENGINE_DOCKER_IMAGE
								)));
	}

	private Optional<ShapeInfo> getExploratoryShape(List<String> shapeNames, Document d) {
		final String shape = d.getString(SHAPE);
		if (isShapeAcceptable(shapeNames, shape)) {
			return Optional.of(new ShapeInfo(shape));
		}
		return Optional.empty();
	}

	private boolean isDataEngine(String dockerImage) {
		return DataEngineType.fromDockerImageName(dockerImage) == DataEngineType.SPARK_STANDALONE;
	}

	private Optional<ShapeInfo> getDataEngineServiceShape(List<String> shapeNames,
														  Document c) {
		final String desMasterShape = c.getString(MASTER_NODE_SHAPE);
		final String desSlaveShape = c.getString(SLAVE_NODE_SHAPE);
		if (isShapeAcceptable(shapeNames, desMasterShape, desSlaveShape)) {
			return Optional.of(new ShapeInfo(desMasterShape, desSlaveShape, c.getString(TOTAL_INSTANCE_NUMBER)));
		}
		return Optional.empty();
	}

	private Optional<ShapeInfo> getDataEngineShape(List<String> shapeNames, Document c) {
		final String dataEngineShape = c.getString(DATAENGINE_SHAPE);
		if ((isShapeAcceptable(shapeNames, dataEngineShape))
				&& StringUtils.isNotEmpty(c.getString(COMPUTATIONAL_ID))) {

			return Optional.of(new ShapeInfo(dataEngineShape, c.getString(DATAENGINE_INSTANCE_COUNT)));
		}
		return Optional.empty();
	}

	private boolean isShapeAcceptable(List<String> shapeNames, String... shapes) {
		return shapeNames == null || shapeNames.isEmpty() || Arrays.stream(shapes).anyMatch(shapeNames::contains);
	}

	protected abstract void appendSsnAndEdgeNodeType(List<String> shapeNames, Map<String, BillingDAO.ShapeInfo>
			shapes);

	protected String generateShapeName(ShapeInfo shape) {
		return Optional.ofNullable(shape).map(ShapeInfo::getName).orElse(StringUtils.EMPTY);
	}

	/**
	 * Store shape info
	 */
	@Getter
	@ToString
	protected class ShapeInfo {
		private static final String DES_NAME_FORMAT = "Master: %s%sSlave:  %d x %s";
		private static final String DE_NAME_FORMAT = "%d x %s";
		private final boolean isDataEngine;
		private final String shape;
		private final String slaveShape;
		private final String slaveCount;
		private final boolean isExploratory;

		private ShapeInfo(boolean isDataEngine, String shape, String slaveShape, String slaveCount, boolean
				isExploratory) {
			this.isDataEngine = isDataEngine;
			this.shape = shape;
			this.slaveShape = slaveShape;
			this.slaveCount = slaveCount;
			this.isExploratory = isExploratory;
		}

		public ShapeInfo(String shape) {
			this(false, shape, null, null, true);
		}

		ShapeInfo(String shape, String slaveShape, String slaveCount) {
			this(false, shape, slaveShape, slaveCount, false);
		}


		ShapeInfo(String shape, String slaveCount) {
			this(true, shape, null, slaveCount, false);
		}

		public String getName() {
			if (isExploratory) {
				return shape;
			} else {
				return clusterName();
			}
		}

		private String clusterName() {
			try {
				final Integer count = Integer.valueOf(slaveCount);
				return isDataEngine ? String.format(DE_NAME_FORMAT, count, shape) :
						String.format(DES_NAME_FORMAT, shape, System.lineSeparator(), count - 1, slaveShape);
			} catch (NumberFormatException e) {
				log.error("Cannot parse string {} to integer", slaveCount);
				return StringUtils.EMPTY;
			}
		}
	}
}
