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

package com.epam.dlab.mongo;

import com.epam.dlab.core.DBAdapterBase;
import com.epam.dlab.core.aggregate.UsageDataList;
import com.epam.dlab.exceptions.AdapterException;
import com.epam.dlab.exceptions.InitializationException;
import com.epam.dlab.exceptions.ParseException;
import com.epam.dlab.model.aws.ReportLine;
import com.epam.dlab.module.ModuleName;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * The adapter for file system.
 */
@JsonTypeName(ModuleName.ADAPTER_MONGO_DLAB)
@JsonClassDescription(
		"Mongo DB adapter.\n" +
				"Write converted data to the Mongo database. Can be used for AdapterOut only.\n" +
				"  - type: " + ModuleName.ADAPTER_MONGO_DLAB + "\n" +
				"    host: <host>             - the host name or IP address.\n" +
				"    port: <port>             - the port number.\n" +
				"    database: <database>     - the name of database.\n" +
				"    username: <username>     - the name of user.\n" +
				"    password: <password>     - the password of user.\n" +
				"    [bufferSize: <number>]   - the size of buffer, default is 10000 records.\n" +
				"    [upsert: <false | true>] - if true then upsert is enabled."
)
public class AdapterMongoDb extends DBAdapterBase {

	/**
	 * The size of buffer for bulk insert. Not applicable for upsert mode.
	 */
	@JsonProperty
	private int bufferSize = 10000;

	/**
	 * The upsert mode if set to <b>true</b>.
	 */
	@JsonProperty
	private boolean upsert = false;

	/**
	 * Return the size of buffer for bulk insert.
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * Set the size of buffer for bulk insert.
	 *
	 * @throws InitializationException
	 */
	public void setBufferSize(int bufferSize) throws InitializationException {
		if (upsert && bufferSize <= 0) {
			throw new InitializationException("The bufferSize must be greater than zero when upsert mode is switched" +
					" " +
					"on");
		}
		this.bufferSize = bufferSize;
	}

	/**
	 * Return the <b>true</b> if upsert mode switched on.
	 */
	public boolean isUpsert() {
		return upsert;
	}

	/**
	 * Set the upsert mode.
	 *
	 * @throws InitializationException
	 */
	public void setUpsert(boolean upsert) throws InitializationException {
		if (upsert && bufferSize <= 0) {
			throw new InitializationException("Upsert mode cannot be enabled if the bufferSize is zero or less than " +
					"zero");
		}
		this.upsert = upsert;
	}


	/**
	 * Custom connection to Mongo database.
	 */
	private MongoDbConnection connection;

	/**
	 * Mongo collection.
	 */
	private MongoCollection<Document> collection;

	/**
	 * DAO of DLab's resource type.
	 */
	private DlabResourceTypeDAO resourceTypeDAO;

	/**
	 * Buffer for insert operations.
	 */
	private List<Document> buffer;

	/**
	 * List of dates for delete from MongoDB.
	 */
	private UsageDataList usageDateList;


	@Override
	public void open() throws AdapterException {
		if (connection == null) {
			if (getMode() != Mode.WRITE) {
				throw new AdapterException("Mode of " + getType() + " adapter may be " + Mode.WRITE + " only.");
			}
			connection = new MongoDbConnection(getHost(), getPort(), getDatabase(), getUsername(), getPassword());
			collection = connection.getCollection(MongoConstants.COLLECTION_BILLING);
			try {
				resourceTypeDAO = new DlabResourceTypeDAO(connection);
			} catch (InitializationException e) {
				throw new AdapterException("Cannot initialize billing transformer to DLab format. " + e
						.getLocalizedMessage(), e);
			}

			connection.createBillingIndexes();
			usageDateList = new UsageDataList();
			buffer = (upsert || bufferSize > 0 ? new ArrayList<>(bufferSize) : null);
		} else {
			throw new AdapterException("Connection is already opened");
		}
	}

	@Override
	public void close() throws AdapterException {
		if (connection != null) {
			if (upsert) {
				connection.upsertRows(collection, buffer, usageDateList);
			} else if (bufferSize > 0) {
				connection.insertRows(collection, buffer);
			}
			buffer = null;
			updateTotal();

			try {
				connection.close();
			} catch (Exception e) {
				throw new AdapterException("Cannot close connection to database " +
						getDatabase() + ". " + e.getLocalizedMessage(), e);
			} finally {
				connection = null;
			}
		}
	}

	@Override
	public String getEntryName() {
		return MongoConstants.COLLECTION_BILLING;
	}

	@Override
	public String readLine() throws AdapterException {
		throw new AdapterException("Unimplemented method called.");
	}

	@Override
	public void writeHeader(List<String> header) throws AdapterException {
		// Nothing to do
	}

	@Override
	public void writeRow(ReportLine row) throws AdapterException {
		Document document;
		try {
			document = resourceTypeDAO.transform(row);
		} catch (ParseException e) {
			throw new AdapterException("Cannot transform report line. " + e.getLocalizedMessage(), e);
		}

		usageDateList.append(row.getUsageDate());
		if (upsert) {
			buffer.add(document);
			if (buffer.size() >= bufferSize) {
				connection.upsertRows(collection, buffer, usageDateList);
			}
		} else if (bufferSize > 0) {
			buffer.add(document);
			if (buffer.size() >= bufferSize) {
				connection.insertRows(collection, buffer);
			}
		} else {
			connection.insertOne(collection, document);
		}
	}

	/**
	 * Update total cost of resources.
	 */
	private void updateTotal() throws AdapterException {
		TreeSet<String> months = new TreeSet<>();
		try {
			for (String date : usageDateList) {
				months.add(date.substring(0, 7));
			}
			for (String month : months) {
				resourceTypeDAO.updateMonthTotalCost(month);
			}
		} catch (Exception e) {
			throw new AdapterException("Cannot update total monthly cost. " + e.getLocalizedMessage(), e);
		}
		try {
			if (months.size() > 0) {
				resourceTypeDAO.updateExploratoryCost();
			}
		} catch (Exception e) {
			throw new AdapterException("Cannot update total cost of exploratory. " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("bufferSize", bufferSize)
				.add("upsert", upsert);
	}
}
