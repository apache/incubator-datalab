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

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import io.dropwizard.lifecycle.Managed;

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_NAME;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;

/** Creates the indexes for mongo collections. */
public class IndexCreator extends BaseDAO implements Managed {
    @Override
	public void start() {
        mongoService.getCollection(USER_INSTANCES)
        		.createIndex(new BasicDBObject(USER, 1)
        		.append(EXPLORATORY_NAME, 2),
                new IndexOptions().unique(true));
        // TODO: Make refactoring and append indexes for other mongo collections
    }

    @Override
	public void stop() {
		//Add some functionality if necessary
    }
}
