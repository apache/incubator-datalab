/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import io.dropwizard.lifecycle.Managed;

import static com.epam.dlab.backendapi.dao.InfrastructureProvisionDAO.EXPLORATORY_NAME;

public class IndexCreator extends BaseDAO implements Managed {
    @Override
    public void start() throws Exception {
        mongoService.getCollection(USER_INSTANCES).createIndex(new BasicDBObject(USER, 1).append(EXPLORATORY_NAME, 2),
                new IndexOptions().unique(true));
    }

    @Override
    public void stop() throws Exception {

    }
}
