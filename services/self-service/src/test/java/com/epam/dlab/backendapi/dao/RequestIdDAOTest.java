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

import com.epam.dlab.backendapi.domain.RequestIdDTO;
import org.junit.*;

import java.util.Date;
import java.util.UUID;

import static com.epam.dlab.backendapi.dao.MongoCollections.REQUEST_ID;
import static org.junit.Assert.assertEquals;

@Ignore
public class RequestIdDAOTest extends DAOTestBase {
    private RequestIdDAO dao;
    
    public RequestIdDAOTest() {
        super(REQUEST_ID);
    }

    @Before
    public void setup() {
    	dao = new RequestIdDAO();
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
	public void test() {
    	RequestIdDTO dto = new RequestIdDTO()
    			.withId(UUID.randomUUID().toString())
    			.withUser("user1")
    			.withRequestTime(new Date())
    			.withExpirationTime(new Date(System.currentTimeMillis() + 1000));
    	dao.put(dto);
    	System.out.println(dto);
    	
    	RequestIdDTO dto1 = dao.get(dto.getId());
    	System.out.println(dto1);
        assertEquals(dto.toString(), dto1.toString());
        
        Date expirationTime = new Date();
        dao.resetExpirationTime();

    	RequestIdDTO dto2 = dao.get(dto.getId());
        assertEquals(expirationTime, dto2.getExpirationTime());
        
        long deleteCount = dao.removeExpired();
        assertEquals(1, deleteCount);
    }
}
