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

package com.epam.dlab.core;

import com.epam.dlab.exceptions.InitializationException;
import com.epam.dlab.mongo.MongoConstants;
import com.epam.dlab.mongo.MongoDbConnection;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.UpdateOptions;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;

/**
 * Provides loading and storing the working data of modules.
 */
public class ModuleData {

	private static final String ID_FIELD = "_id";
	private static final String MODIFICATION_DATE = "lastModificationDate";
	public static final String ENTRIES_FIELD = "entries";
	/**
	 * Date formatter.
	 */
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	private final MongoDbConnection connection;

	private String id;
	private Date modificationDate;

	/**
	 * Entries of data.
	 */
	private Map<String, String> entries = new HashMap<>();

	/**
	 * Flag modification of entries.
	 */
	private boolean modified;

	/**
	 * Return <b>true</b> if any entries was modify.
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * Instantiate module data.
	 *
	 * @param connection the name of data file.
	 * @throws InitializationException
	 */
	public ModuleData(MongoDbConnection connection) {
		this.connection = connection;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}

	/**
	 * Return the value for given key or <b>null</b> if value not found.
	 *
	 * @param key the key of entry.
	 */
	public String getString(String key) {
		return entries.get(key);
	}

	/**
	 * Return the date value for given key or <b>null</b> if value not found.
	 *
	 * @param key the key of entry.
	 * @throws ParseException
	 */
	public Date getDate(String key) throws ParseException {
		String value = entries.get(key);
		return (value == null ? null : dateFormat.parse(value));
	}

	/**
	 * Set value for given key or delete entry if the value is <b>null</b>.
	 *
	 * @param key   the key of entry.
	 * @param value the value.
	 */
	public void set(String key, String value) {
		if (StringUtils.equals(entries.get(key), value)) {
			return;
		} else if (value == null) {
			entries.remove(key);
		} else {
			entries.put(key, value);
		}
		modified = true;
	}

	/**
	 * Set value for given key or delete entry if the value is <b>null</b>.
	 *
	 * @param key   the key of entry.
	 * @param value the date.
	 */
	public void set(String key, Date value) {
		set(key, dateFormat.format(value));
	}

	public void store() {
		//TODO refactor this module move working with db to DAO layer
		final Document document = new Document().append(ID_FIELD, id).append(MODIFICATION_DATE,
				modificationDate).append(ENTRIES_FIELD, entries);
		connection.getCollection(MongoConstants.BILLING_DATA_COLLECTION)
				.updateOne(eq(ID_FIELD, id), new Document("$set", document), new UpdateOptions().upsert(true));
		modified = false;
	}

	public boolean wasProcessed(String fileName, Date modificationDate) {
		final FindIterable<Document> documents = connection.getCollection(MongoConstants.BILLING_DATA_COLLECTION)
				.find(and(eq(ID_FIELD, fileName), gte(MODIFICATION_DATE, modificationDate)))
				.limit(1);
		return documents.iterator().hasNext();
	}


	/**
	 * Returns a string representation of the object.
	 *
	 * @param self the object to generate the string for (typically this), used only for its class name.
	 */
	public ToStringHelper toStringHelper(Object self) {
		return MoreObjects.toStringHelper(self)
				.addValue(entries);
	}

	@Override
	public String toString() {
		return toStringHelper(this)
				.toString();
	}
}
