package org.apache.dlab.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.dlab.util.PropertyHelper;

public class MongoDBHelper {
	private static final MongoClient client = MongoClients
			.create(PropertyHelper.read("mongo.connection.string"));

	public static void cleanCollection(String collection) {
		client.getDatabase(PropertyHelper.read("mongo.db.name")).getCollection(collection).drop();
	}
}
