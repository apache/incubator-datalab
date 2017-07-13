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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;

import java.util.Date;
import java.util.Optional;

import org.bson.Document;

import com.epam.dlab.backendapi.domain.RequestIdDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

/** DAO for request id.
 */
public class RequestIdDAO extends BaseDAO {
    private static final String EXPIRATION_TIME = "expirationTime";
	
	public RequestIdDTO get(String id) throws DlabException {
		Optional<RequestIdDTO> opt = findOne(REQUEST_ID, eq(ID, id), RequestIdDTO.class);
		if (!opt.isPresent()) {
			throw new DlabException("Request id " + id + " not found.");
		}
		return opt.get();
    }
    
	public void put(RequestIdDTO requestId) throws DlabException {
		getCollection(REQUEST_ID)
			.insertOne(convertToBson(requestId));
    }
    
	public void delete(String id) throws DlabException {
		getCollection(REQUEST_ID).deleteOne(eq(ID, id));
    }
	
	public void resetExpirationTime() throws DlabException {
		Date time = new Date();
		getCollection(REQUEST_ID).updateMany(new Document(), Updates.set(EXPIRATION_TIME, time));
    }
	
	public long removeExpired() throws DlabException {
		DeleteResult result = getCollection(REQUEST_ID)
								.deleteMany(lt(EXPIRATION_TIME, new Date()));
		return result.getDeletedCount();
    }
}