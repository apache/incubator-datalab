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

import static com.epam.dlab.backendapi.dao.MongoCollections.BILLING;
import static org.junit.Assert.assertEquals;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.epam.dlab.backendapi.resources.dto.BillingFilterFormDTO;
import com.google.common.collect.Lists;

//@Ignore
public class BillingDAOTest extends DAOTestBase {
    private BillingDAO dao;
    
    public BillingDAOTest() {
        super(BILLING);
    }

    @Before
    public void setup() {
    	dao = new BillingDAO();
        testInjector.injectMembers(dao);
    }

    @BeforeClass
    public static void setupAll() {
        DAOTestBase.setupAll();
    }

    @AfterClass
    public static void teardownAll() {
        //DAOTestBase.teardownAll();
    }

    @Test
    public void getReport() {
    	BillingFilterFormDTO filter = new BillingFilterFormDTO();
    	filter.setProduct(Lists.newArrayList("S3", "EC2"));
    	filter.setDateStart("2017-06-09");
    	Document report = dao.getReport("user", filter);
        assertEquals(report, report);
    }
}
