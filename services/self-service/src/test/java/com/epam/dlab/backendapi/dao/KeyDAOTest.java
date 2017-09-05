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

import static com.epam.dlab.backendapi.dao.MongoCollections.USER_EDGE;
import static junit.framework.TestCase.assertEquals;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

@Ignore
public class KeyDAOTest extends DAOTestBase {
	private ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	
    private KeyDAO dao;
    
    public KeyDAOTest() {
        super(USER_EDGE);
    }

    @Before
    public void setup() {
        cleanup();
        dao = new KeyDAO();
        testInjector.injectMembers(dao);

    }

    @BeforeClass
    public static void setupAll() {
        DAOTestBase.setupAll();
    }

    @AfterClass
    public static void teardownAll() {
        DAOTestBase.teardownAll();
    }

    @Test
    public void updateEdgeInfo() throws IOException {
    	String json = "{" +
    		"\"instance_id\": \"i-0ff4ad07223b46cae\"," +
    		"\"hostname\": \"ec2-52-32-129-15.us-west-2.compute.amazonaws.com\"," +
    		"\"public_ip\": \"52.32.129.15\"," +
    		"\"ip\": \"172.31.12.114\"," +
    		"\"key_name\": \"BDCC-DSS-POC\"," +
    		"\"user_own_bicket_name\": \"usein1120v10-usein-faradzhev-bucket\"," +
    		"\"tunnel_port\": 22," +
    		"\"socks_port\": 1080," +
    		"\"notebook_sg\": \"usein1120v10-usein_faradzhev-nb-SG\"," +
    		"\"notebook_profile\": \"usein1120v10-usein_faradzhev-nb-Profile\"," +
    		"\"notebook_subnet\": \"172.31.55.0/24\"," +
    		"\"edge_sg\": \"usein1120v10-usein_faradzhev-edge-SG\"," +
    		"\"edge_status\": \"running\"" +
        	"}";
    	EdgeInfoAws dto1 = MAPPER.readValue(json, EdgeInfoAws.class);
    	System.out.println(dto1);
    	
    	String user = "user1";
    	dao.updateEdgeInfo(user, dto1);
    	
    	EdgeInfoAws dto2 = dao.getEdgeInfo(user, EdgeInfoAws.class, new EdgeInfoAws());

        assertEquals(dto1.toString(), dto2.toString());
    }
}
