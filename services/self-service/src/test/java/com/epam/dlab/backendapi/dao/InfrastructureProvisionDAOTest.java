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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.UserComputationalResourceDTO;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.result.UpdateResult;
import org.junit.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.epam.dlab.backendapi.dao.InfrastructureProvisionDAO.exploratoryCondition;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static junit.framework.TestCase.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Ignore
public class InfrastructureProvisionDAOTest extends DAOTestBase {
    private InfrastructureProvisionDAO dao;

    public InfrastructureProvisionDAOTest() {
        super(Collections.singletonList(USER_INSTANCES));
    }

    @Before
    public void setup() {
        cleanup();
        dao = new InfrastructureProvisionDAO();
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
    public void fetchExploratoryIdSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1");

        UserInstanceDTO instance2 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_2")
                .withExploratoryId("exp2");

        UserInstanceDTO instance3 = new UserInstanceDTO()
                .withUser("user2")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp21");

        dao.insertOne(USER_INSTANCES, instance1);
        dao.insertOne(USER_INSTANCES, instance2);
        dao.insertOne(USER_INSTANCES, instance3);

        String testId = dao.fetchExploratoryId("user1", "exp_name_1");
        assertEquals(testId, "exp1");
    }

    @Test
    public void fetchExploratoryIdNotFound() {
        String testId = dao.fetchExploratoryId("user1", "exp_name_1");
        assertEquals(testId, EMPTY);
    }

    @Test(expected = DlabException.class)
    public void fetchExploratoryIdFailWhenMany() throws DlabException {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1");

        UserInstanceDTO instance2 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp2");

        dao.insertOne(USER_INSTANCES, instance1);
        dao.insertOne(USER_INSTANCES, instance2);

        dao.fetchExploratoryId("user1", "exp_name_1");
    }

    @Test
    public void fetchExploratoryStatusSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withStatus("created");

        UserInstanceDTO instance2 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_2")
                .withStatus("failed");

        dao.insertOne(USER_INSTANCES, instance1);
        dao.insertOne(USER_INSTANCES, instance2);

        UserInstanceStatus status = dao.fetchExploratoryStatus("user1", "exp_name_2");
        assertEquals(status, UserInstanceStatus.FAILED);
    }

    @Test
    public void fetchExploratoryStatusBadValue() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withStatus("creat");

        dao.insertOne(USER_INSTANCES, instance1);
        UserInstanceStatus status = dao.fetchExploratoryStatus("user1", "exp_name_1");
        assertEquals(status, null);
    }

    @Test
    public void fetchExploratoryStatusNotFound() {
        UserInstanceStatus status = dao.fetchExploratoryStatus("user1", "exp_name_1");
        assertEquals(status, null);
    }

    @Test(expected = DlabException.class)
    public void fetchExploratoryStatusFailWhenMany() throws DlabException {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withStatus("created");

        UserInstanceDTO instance2 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withStatus("created");

        dao.insertOne(USER_INSTANCES, instance1);
        dao.insertOne(USER_INSTANCES, instance2);

        dao.fetchExploratoryStatus("user1", "exp_name_1");
    }

    @Test
    public void fetchExploratoryFieldsSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withImageName("jupyter")
                .withImageVersion("jupyter-2");

        Boolean isInserted = dao.insertExploratory(instance1);
        assertTrue(isInserted);

        UserInstanceDTO instance2 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_2")
                .withExploratoryId("exp2")
                .withStatus("running")
                .withImageName("rstudio")
                .withImageVersion("r-3");

        Boolean isInserted2 = dao.insertExploratory(instance2);
        assertTrue(isInserted2);

        Optional<UserInstanceDTO> testInstance = dao.fetchExploratoryFields("user1", "exp_name_2");
        assertTrue(testInstance.isPresent());
        assertEquals(instance2.getExploratoryId(), testInstance.get().getExploratoryId());
        assertEquals(instance2.getStatus(), testInstance.get().getStatus());
        assertEquals(instance2.getImageName(), testInstance.get().getImageName());
        assertEquals(instance2.getImageVersion(), testInstance.get().getImageVersion());
    }

    @Test
    public void insertExploratorySuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withImageName("jupyter")
                .withImageVersion("jupyter-2");

        Boolean isInserted = dao.insertExploratory(instance1);
        assertTrue(isInserted);

        long insertedCount = mongoService.getCollection(USER_INSTANCES).count();
        assertEquals(insertedCount, 1L);

        Optional<UserInstanceDTO> testInstance = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
                UserInstanceDTO.class);
        assertTrue(testInstance.isPresent());
        assertEquals(instance1.getExploratoryId(), testInstance.get().getExploratoryId());
        assertEquals(instance1.getStatus(), testInstance.get().getStatus());
        assertEquals(instance1.getImageName(), testInstance.get().getImageName());
        assertEquals(instance1.getImageVersion(), testInstance.get().getImageVersion());
    }

    @Test
    public void updateExploratoryStatusSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created");

        UserInstanceDTO instance2 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_2")
                .withExploratoryId("exp2")
                .withStatus("created");

        dao.insertOne(USER_INSTANCES, instance1);
        dao.insertOne(USER_INSTANCES, instance2);

        StatusBaseDTO newStatus = new StatusBaseDTO();
        newStatus.setUser("user1");
        newStatus.setExploratoryName("exp_name_1");
        newStatus.setStatus("running");
        UpdateResult result = dao.updateExploratoryStatus(newStatus);

        assertEquals(result.getModifiedCount(), 1);

        Optional<UserInstanceDTO> testInstance = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
                UserInstanceDTO.class);
        assertTrue(testInstance.isPresent());
        assertEquals(UserInstanceStatus.of(testInstance.get().getStatus()), UserInstanceStatus.RUNNING);

        Optional<UserInstanceDTO> testInstance2 = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance2.getUser(), instance2.getExploratoryName()),
                UserInstanceDTO.class);
        assertTrue(testInstance2.isPresent());
        assertEquals(UserInstanceStatus.of(testInstance2.get().getStatus()), UserInstanceStatus.CREATED);
    }

    @Test
    public void updateExploratoryFieldsSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created");

        UserInstanceDTO instance2 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_2")
                .withExploratoryId("exp2")
                .withStatus("created");

        dao.insertOne(USER_INSTANCES, instance1);
        dao.insertOne(USER_INSTANCES, instance2);

        ExploratoryStatusDTO status = new ExploratoryStatusDTO();
        status.setUser("user1");
        status.setExploratoryName("exp_name_1");
        status.setExploratoryId("exp2");
        status.setExploratoryUrl("www.exp2.com");
        status.setStatus("running");
        status.setUptime(new Date(100));

        UpdateResult result = dao.updateExploratoryFields(status);
        assertEquals(result.getModifiedCount(), 1);

        Optional<UserInstanceDTO> testInstance1 = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
                UserInstanceDTO.class);
        assertTrue(testInstance1.isPresent());

        UserInstanceDTO instance = testInstance1.get();
        assertEquals(instance.getExploratoryId(), status.getExploratoryId());
        assertEquals(instance.getUrl(), status.getExploratoryUrl());
        assertEquals(instance.getStatus(), status.getStatus());
        assertEquals(instance.getUptime(), status.getUptime());
    }

    @Test
    public void addComputationalSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withUptime(new Date(100));

        dao.insertOne(USER_INSTANCES, instance1);

        UserComputationalResourceDTO comp1 = new UserComputationalResourceDTO()
                .withComputationalName("comp1")
                .withComputationalId("c1")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp1);
        assertTrue(inserted);

        UserInstanceDTO testInstance = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
                UserInstanceDTO.class).get();
        assertTrue(testInstance.getResources() != null && testInstance.getResources().size() == 1);

        UserComputationalResourceDTO testComp = testInstance.getResources().get(0);
        assertEquals(comp1.getComputationalName(), testComp.getComputationalName());
        assertEquals(comp1.getComputationalId(), testComp.getComputationalId());
        assertEquals(comp1.getStatus(), testComp.getStatus());
        assertEquals(comp1.getUptime(), testComp.getUptime());
    }

    @Test
    public void addComputationalAlreadyExists() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withUptime(new Date(100));

        dao.insertOne(USER_INSTANCES, instance1);

        UserComputationalResourceDTO comp1 = new UserComputationalResourceDTO()
                .withComputationalName("comp1")
                .withComputationalId("c1")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp1);
        assertTrue(inserted);

        boolean insertFail = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp1);
        assertFalse(insertFail);
    }

    @Test
    public void fetchComputationalIdSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withUptime(new Date(100));

        dao.insertOne(USER_INSTANCES, instance1);

        UserComputationalResourceDTO comp1 = new UserComputationalResourceDTO()
                .withComputationalName("comp1")
                .withComputationalId("c1")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp1);
        assertTrue(inserted);

        UserComputationalResourceDTO comp2 = new UserComputationalResourceDTO()
                .withComputationalName("comp2")
                .withComputationalId("c2")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted2 = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp2);
        assertTrue(inserted2);

        String testId = dao.fetchComputationalId(instance1.getUser(),
                instance1.getExploratoryName(), comp2.getComputationalName());
        assertEquals(comp2.getComputationalId(), testId);
    }

    @Test
    public void updateComputationalStatusSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withUptime(new Date(100));

        dao.insertOne(USER_INSTANCES, instance1);

        UserComputationalResourceDTO comp1 = new UserComputationalResourceDTO()
                .withComputationalName("comp1")
                .withComputationalId("c1")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp1);
        assertTrue(inserted);

        UserComputationalResourceDTO comp2 = new UserComputationalResourceDTO()
                .withComputationalName("comp2")
                .withComputationalId("c2")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted2 = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp2);
        assertTrue(inserted2);

        UpdateResult testResult = dao.updateComputationalStatus(
                new ComputationalStatusDTO()
                        .withUser(instance1.getUser())
                        .withExploratoryName(instance1.getExploratoryName())
                        .withComputationalName(comp2.getComputationalName())
                        .withStatus(UserInstanceStatus.RUNNING));
        assertEquals(testResult.getModifiedCount(), 1);

        Optional<UserInstanceDTO> testInstance = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
                UserInstanceDTO.class);
        assertTrue(testInstance.isPresent());

        List<UserComputationalResourceDTO> list = testInstance.get().getResources();
        UserComputationalResourceDTO testComp1 = list.stream()
                .filter(r -> r.getComputationalName().equals(comp1.getComputationalName()))
                .findFirst()
                .orElse(null);
        assertNotNull(testComp1);

        UserComputationalResourceDTO testComp2 = list.stream()
                .filter(r -> r.getComputationalName().equals(comp2.getComputationalName()))
                .findFirst()
                .orElse(null);
        assertNotNull(testComp2);

        assertEquals(testComp1.getStatus(), comp1.getStatus());
        assertEquals(UserInstanceStatus.of(testComp2.getStatus()), UserInstanceStatus.RUNNING);
    }

    @Test
    public void updateComputationalStatusesForExploratorySuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withUptime(new Date(100));

        dao.insertOne(USER_INSTANCES, instance1);

        UserComputationalResourceDTO comp1 = new UserComputationalResourceDTO()
                .withComputationalName("comp1")
                .withComputationalId("c1")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp1);
        assertTrue(inserted);

        UserComputationalResourceDTO comp2 = new UserComputationalResourceDTO()
                .withComputationalName("comp2")
                .withComputationalId("c2")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted2 = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp2);
        assertTrue(inserted2);

        UserComputationalResourceDTO comp3 = new UserComputationalResourceDTO()
                .withComputationalName("comp3")
                .withComputationalId("c3")
                .withStatus("terminated")
                .withUptime(new Date(100));
        boolean inserted3 = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp3);
        assertTrue(inserted3);

        UpdateResult testResult = dao.updateComputationalStatusesForExploratory(new StatusBaseDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withStatus(UserInstanceStatus.STOPPED));
        assertEquals(1, testResult.getModifiedCount());

        Optional<UserInstanceDTO> testInstance = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
                UserInstanceDTO.class);
        assertTrue(testInstance.isPresent());

        List<UserComputationalResourceDTO> list = testInstance.get().getResources();
        UserComputationalResourceDTO testComp1 = list.stream()
                .filter(r -> r.getComputationalName().equals(comp1.getComputationalName()))
                .findFirst()
                .orElse(null);
        assertNotNull(testComp1);

        UserComputationalResourceDTO testComp2 = list.stream()
                .filter(r -> r.getComputationalName().equals(comp2.getComputationalName()))
                .findFirst()
                .orElse(null);
        assertNotNull(testComp2);

        UserComputationalResourceDTO testComp3 = list.stream()
                .filter(r -> r.getComputationalName().equals(comp3.getComputationalName()))
                .findFirst()
                .orElse(null);
        assertNotNull(testComp3);

        assertEquals(UserInstanceStatus.STOPPED, UserInstanceStatus.of(testComp1.getStatus()));
        assertEquals(UserInstanceStatus.STOPPED, UserInstanceStatus.of(testComp2.getStatus()));
        assertEquals(testComp3.getStatus(), comp3.getStatus());
    }

    @Test
    public void updateComputationalFieldsSuccess() {
        UserInstanceDTO instance1 = new UserInstanceDTO()
                .withUser("user1")
                .withExploratoryName("exp_name_1")
                .withExploratoryId("exp1")
                .withStatus("created")
                .withUptime(new Date(100));

        dao.insertOne(USER_INSTANCES, instance1);

        UserComputationalResourceDTO comp1 = new UserComputationalResourceDTO()
                .withComputationalName("comp1")
                .withComputationalId("c1")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp1);
        assertTrue(inserted);

        UserComputationalResourceDTO comp2 = new UserComputationalResourceDTO()
                .withComputationalName("comp2")
                .withComputationalId("c2")
                .withStatus("created")
                .withUptime(new Date(100));
        boolean inserted2 = dao.addComputational(instance1.getUser(), instance1.getExploratoryName(), comp2);
        assertTrue(inserted2);

        ComputationalStatusDTO status = new ComputationalStatusDTO()
                .withUser(instance1.getUser())
                .withExploratoryName(instance1.getExploratoryName())
                .withComputationalName(comp2.getComputationalName())
                .withComputationalId("c3")
                .withStatus("running")
                .withUptime(new Date(200));
        UpdateResult result = dao.updateComputationalFields(status);
        assertEquals(1, result.getModifiedCount());

        Optional<UserInstanceDTO> testInstance = dao.findOne(USER_INSTANCES,
                exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
                UserInstanceDTO.class);
        assertTrue(testInstance.isPresent());

        List<UserComputationalResourceDTO> list = testInstance.get().getResources();
        UserComputationalResourceDTO testComp2 = list.stream()
                .filter(r -> r.getComputationalName().equals(comp2.getComputationalName()))
                .findFirst()
                .orElse(null);
        assertNotNull(testComp2);

        assertEquals(status.getComputationalId(), testComp2.getComputationalId());
        assertEquals(status.getStatus(), testComp2.getStatus());
        assertEquals(status.getUptime(), testComp2.getUptime());
    }
}
