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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.mongo.IsoDateModule;
import com.epam.dlab.mongo.MongoService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Aggregates.unwind;

/**
 * Implements the base API for Mongo database.
 */
public class BaseDAO implements MongoCollections {
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseDAO.class);

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
			.registerModule(new IsoDateModule());

	static final String FIELD_SET_DELIMETER = ".$.";
	private static final String FIELD_PROJECTION_DELIMITER = "$";
	public static final String ID = "_id";
	static final String SET = "$set";
	public static final String USER = "user";
	protected static final String INSTANCE_ID = "instance_id";
	public static final String STATUS = "status";
	public static final String ERROR_MESSAGE = "error_message";
	static final String TIMESTAMP = "timestamp";
	static final String REUPLOAD_KEY_REQUIRED = "reupload_key_required";

	@Inject
	protected MongoService mongoService;

	/**
	 * Return <b>true</b> if collection exists.
	 *
	 * @param name collection name.
	 */
	boolean collectionExists(String name) {
		return mongoService.collectionExists(name);
	}

	/**
	 * Return Mongo collection.
	 *
	 * @param collection collection name.
	 */
	public MongoCollection<Document> getCollection(String collection) {
		return mongoService.getCollection(collection);
	}

	/**
	 * Inserts the document into the collection.
	 *
	 * @param collection collection name.
	 * @param supplier   document.
	 */
	protected void insertOne(String collection, Supplier<Document> supplier) {
		insertOne(collection, supplier, generateUUID());
	}

	/**
	 * Inserts the document into the collection with given the unique id.
	 *
	 * @param collection collection name.
	 * @param document   document.
	 * @param uuid       unique id.
	 */
	protected void insertOne(String collection, Supplier<Document> document, String uuid) {
		try {
			mongoService.getCollection(collection)
					.insertOne(document.get()
							.append(ID, uuid)
							.append(TIMESTAMP, new Date()));
		} catch (MongoException e) {
			LOGGER.warn("Insert to Mongo DB fails: {}", e.getLocalizedMessage(), e);
			throw new DlabException("Insert to Mongo DB failed: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Serializes the object and inserts into the collection.
	 *
	 * @param collection collection name.
	 * @param object     for inserting to collection.
	 */
	protected void insertOne(String collection, Object object) {
		insertOne(collection, object, generateUUID());
	}

	/**
	 * Serializes the object and inserts into the collection.
	 *
	 * @param collection collection name.
	 * @param object     for inserting to collection.
	 * @param uuid       unique id.
	 */
	protected void insertOne(String collection, Object object, String uuid) {
		try {
			mongoService.getCollection(collection)
					.insertOne(convertToBson(object)
							.append(ID, uuid)
							.append(TIMESTAMP, new Date()));
		} catch (MongoException e) {
			LOGGER.warn("Insert to Mongo DB fails: {}", e.getLocalizedMessage(), e);
			throw new DlabException("Insert to Mongo DB fails: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Updates single document in the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @param document   document.
	 */
	protected UpdateResult updateOne(String collection, Bson condition, Bson document) {
		try {
			return mongoService.getCollection(collection)
					.updateOne(condition, document);
		} catch (MongoException e) {
			LOGGER.warn("Update Mongo DB fails: {}", e.getLocalizedMessage(), e);
			throw new DlabException("Update to Mongo DB fails: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Update or insert single document in the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @param document   document.
	 * @param isUpsert   if <b>true</b> document will be updated or inserted.
	 */
	protected void updateOne(String collection, Bson condition, Bson document, boolean isUpsert) {
		try {
			if (isUpsert) {
				mongoService.getCollection(collection).updateOne(condition, document,
						new UpdateOptions().upsert(true));
			} else {
				mongoService.getCollection(collection).updateOne(condition, document);
			}
		} catch (MongoException e) {
			LOGGER.warn("Upsert Mongo DB fails: {}", e.getLocalizedMessage(), e);
			throw new DlabException("Upsert to Mongo DB fails: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Updates all documents in the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @param document   document.
	 */
	protected UpdateResult updateMany(String collection, Bson condition, Bson document) {
		try {
			return mongoService.getCollection(collection)
					.updateMany(condition, document);
		} catch (MongoException e) {
			LOGGER.warn("Update Mongo DB fails: {}", e.getLocalizedMessage(), e);
			throw new DlabException("Insert to Mongo DB fails: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Finds and returns documents from the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 */
	protected FindIterable<Document> find(String collection, Bson condition) {
		return mongoService.getCollection(collection)
				.find(condition);
	}

	/**
	 * Finds and returns documents from the collection by condition.
	 *
	 * @param collection    collection name.
	 * @param condition     condition for search documents in collection.
	 * @param resultedClass type of class for deserialization.
	 */
	protected <T> List<T> find(String collection, Bson condition, Class<T> resultedClass) {
		return mongoService.getCollection(collection)
				.find(condition)
				.into(new ArrayList<>())
				.stream()
				.map(d -> convertFromDocument(d, resultedClass))
				.collect(Collectors.toList());
	}

	/**
	 * Finds and returns documents with the specified fields from the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @param projection document describing the fields in the collection to return.
	 */
	protected FindIterable<Document> find(String collection, Bson condition, Bson projection) {
		return mongoService.getCollection(collection)
				.find(condition)
				.projection(projection);
	}

	/**
	 * Aggregates and returns documents according to the specified aggregation pipeline.
	 *
	 * @param collection collection name.
	 * @param pipeline   the aggregate pipeline.
	 */
	private AggregateIterable<Document> aggregate(String collection,
												  List<? extends Bson> pipeline) {
		return mongoService.getCollection(collection)
				.aggregate(pipeline);
	}

	/**
	 * Checks that the documents iterator have one document only.
	 *
	 * @param documents documents
	 */
	private Optional<Document> limitOne(MongoIterable<Document> documents) {
		Document first = documents.first();
		try (MongoCursor<Document> iterator = documents.iterator()) {
			if (iterator.hasNext()) {
				iterator.next();
				if (iterator.hasNext()) {
					throw new DlabException("too many items found while one is expected");
				}
			}
		}
		return Optional.ofNullable(first);
	}

	/**
	 * Finds and returns one document from the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @throws DlabException if documents iterator have more than one document.
	 */
	protected Optional<Document> findOne(String collection, Bson condition) {
		FindIterable<Document> found = find(collection, condition);
		return limitOne(found);
	}

	/**
	 * Finds and returns one document with the specified fields from the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @param projection document describing the fields in the collection to return.
	 * @throws DlabException if documents iterator have more than one document.
	 */
	protected Optional<Document> findOne(String collection,
										 Bson condition,
										 Bson projection) {
		FindIterable<Document> found = find(collection, condition, projection);
		return limitOne(found);
	}

	/**
	 * Serializes given object to document and returns it.
	 *
	 * @param object object
	 */
	Document convertToBson(Object object) {
		try {
			return Document.parse(MAPPER.writeValueAsString(object));
		} catch (IOException e) {
			throw new DlabException("error converting to bson", e);
		}
	}

	/**
	 * Finds and returns one object as given class from the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @param clazz      type of class for deserialization.
	 */
	protected <T> Optional<T> findOne(String collection, Bson condition, Class<T> clazz) {
		Optional<Document> doc = findOne(collection, condition);
		return doc.isPresent() ? Optional.ofNullable(convertFromDocument(doc.get(), clazz)) : Optional.empty();
	}

	/**
	 * Finds and returns one object as given class and with the specified fields from the collection by condition.
	 *
	 * @param collection collection name.
	 * @param condition  condition for search documents in collection.
	 * @param projection document describing the fields in the collection to return.
	 * @param clazz      type of class for deserialization.
	 */
	protected <T> Optional<T> findOne(String collection, Bson condition, Bson projection, Class<T> clazz) {
		Optional<Document> doc = findOne(collection, condition, projection);
		return doc.isPresent() ? Optional.ofNullable(convertFromDocument(doc.get(), clazz)) : Optional.empty();
	}

	/**
	 * Aggregates and returns one document according to the specified aggregation pipeline.
	 *
	 * @param collection collection name.
	 * @param pipeline   the aggregate pipeline.
	 * @throws DlabException if have more than one aggregated documents.
	 */
	Optional<Document> aggregateOne(String collection,
									List<? extends Bson> pipeline) {
		MongoIterable<Document> found = aggregate(collection, pipeline);
		return limitOne(found);
	}

	/**
	 * Deserializes given document to object and returns it.
	 *
	 * @param document element from database
	 */
	<T> T convertFromDocument(Document document, Class<T> clazz) {
		try {
			String json = document.toJson();
			return MAPPER.readValue(json, clazz);
		} catch (IOException e) {
			throw new DlabException("error converting from document with id " + document.get(ID), e);
		}
	}

	protected Document getGroupingFields(String... fieldNames) {
		Document d = new Document();
		for (String name : fieldNames) {
			d.put(name, "$" + name);
		}
		return d;
	}

	protected Stream<Document> stream(Iterable<Document> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	/**
	 * Returns a unique id.
	 */
	private String generateUUID() {
		return UUID.randomUUID().toString();
	}

	static Bson unwindField(String fieldName) {
		return unwind(FIELD_PROJECTION_DELIMITER + fieldName);
	}

	private static Object getDotted(Document d, String fieldName) {
		if (fieldName.isEmpty()) {
			return null;
		}
		final String[] fieldParts = StringUtils.split(fieldName, '.');
		Object val = d.get(fieldParts[0]);
		for (int i = 1; i < fieldParts.length; ++i) {
			if (fieldParts[i].equals("$")
					&& val instanceof ArrayList) {
				ArrayList<?> array = (ArrayList<?>) val;
				if (array.isEmpty()) {
					break;
				} else {
					val = array.get(0);
				}
			} else if (val instanceof Document) {
				val = ((Document) val).get(fieldParts[i]);
			} else {
				break;
			}
		}
		return val;
	}

	static Object getDottedOrDefault(Document d, String fieldName, Object defaultValue) {
		Object result = getDotted(d, fieldName);
		return result == null ? defaultValue : result;
	}
}
