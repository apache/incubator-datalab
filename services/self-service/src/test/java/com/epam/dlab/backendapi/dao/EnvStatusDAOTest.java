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

import com.epam.dlab.backendapi.dao.aws.AwsKeyDao;
import com.epam.dlab.backendapi.resources.dto.HealthStatusEnum;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusResource;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.aws.computational.AwsComputationalResource;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.dto.status.EnvResourceList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import org.junit.*;

import java.util.Date;

import static com.epam.dlab.backendapi.dao.BaseDAO.USER;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_NAME;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_EDGE;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class EnvStatusDAOTest extends DAOTestBase {
    private ExploratoryDAO expDAO;
    private ComputationalDAO compDAO;
    private EnvStatusDAO envDAO;
    private KeyDAO keyDAO;
    
    public EnvStatusDAOTest() {
        super(USER_EDGE, USER_INSTANCES);
    }

    @Before
    public void setup() {
        cleanup();

        mongoService.createCollection(USER_INSTANCES);
        mongoService.getCollection(USER_INSTANCES)
        		.createIndex(new BasicDBObject(USER, 1).append(EXPLORATORY_NAME, 2),
                new IndexOptions().unique(true));

        expDAO = new ExploratoryDAO();
        testInjector.injectMembers(expDAO);
        compDAO = new ComputationalDAO();
        testInjector.injectMembers(compDAO);
        envDAO = new EnvStatusDAO();
        testInjector.injectMembers(envDAO);
        keyDAO = new AwsKeyDao();
        testInjector.injectMembers(keyDAO);
    }

    @BeforeClass
    public static void setupAll() {
        DAOTestBase.setupAll();
    }

    @AfterClass
    public static void teardownAll() {
        DAOTestBase.teardownAll();
    }
    
    private static class EdgeInfo {
        @JsonProperty("instance_id")
        private String instanceId;
        @JsonProperty("edge_status")
        private String edgeStatus;
        @JsonProperty("public_ip")
        private String publicIp;
    }

    @Test
    public void testEnvStatus() {
    	final String user = "user1";
    	final String exploratoryName = "exp1";
    	final String computationalName = "comp1";
    	
    	// Add EDGE
    	EdgeInfo edge = new EdgeInfo();
    	edge.instanceId = "instance0";
    	edge.publicIp = "35.23.78.35";
    	edge.edgeStatus = "stopped";
    	expDAO.insertOne(USER_EDGE, edge, user);
    	
    	// Add exploratory
        UserInstanceDTO exp1 = new UserInstanceDTO()
                .withUser(user)
                .withExploratoryName(exploratoryName)
                .withUptime(new Date(100));
        expDAO.insertOne(USER_INSTANCES, exp1);
        // Set status and instance_id for exploratory
        ExploratoryStatusDTO expStatus = new ExploratoryStatusDTO()
        		.withUser(user)
        		.withExploratoryName(exploratoryName)
        		.withInstanceId("instance1")
        		.withStatus("running");
        expDAO.updateExploratoryFields(expStatus);

        // Add computational
        AwsComputationalResource comp1 = AwsComputationalResource.builder()
                .computationalName(computationalName)
                .instanceId("instance11")
                .status("creating")
                .uptime(new Date(200)).build();
        boolean inserted = compDAO.addComputational(exp1.getUser(), exploratoryName, comp1);
        assertTrue(inserted);

        // Check selected resources
        EnvResourceList resList = envDAO.findEnvResources(user);

        assertEquals(2, resList.getHostList().size());
        assertEquals(1, resList.getClusterList().size());

        assertEquals(edge.instanceId, resList.getHostList().get(0).getId());
        assertEquals(expStatus.getInstanceId(), resList.getHostList().get(1).getId());
        assertEquals(comp1.getInstanceId(), resList.getClusterList().get(0).getId());

        // Change status
        resList.getHostList().get(0).setStatus("running");
        resList.getHostList().get(1).setStatus("stopped");
        resList.getClusterList().get(0).setStatus("terminating");
        
        envDAO.updateEnvStatus(user, resList);

        // Check new status
        EdgeInfoAws userCred = keyDAO.getEdgeInfo(user, EdgeInfoAws.class, new EdgeInfoAws());
        assertEquals("running", userCred.getEdgeStatus());
        assertEquals("stopped", expDAO.fetchExploratoryStatus(user, exploratoryName).toString());
        assertEquals("terminating", compDAO.fetchComputationalFields(user, exploratoryName, computationalName).getStatus());
        
        // Health status
        HealthStatusPageDTO hStatus = envDAO.getHealthStatusPageDTO(user, true);
		assertEquals(HealthStatusEnum.OK.toString(), hStatus.getStatus());
        HealthStatusResource rStatus = hStatus.getListResources().get(0);
		assertEquals("running", rStatus.getStatus());
        assertEquals(rStatus.getResourceId(), edge.publicIp);
    }

}
