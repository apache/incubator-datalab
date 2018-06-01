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

import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.dto.exploratory.ExploratoryURL;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.UpdateResult;
import org.junit.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.epam.dlab.backendapi.dao.BaseDAO.USER;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_NAME;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.exploratoryCondition;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Ignore
public class ExploratoryDAOTest extends DAOTestBase {
	private ExploratoryDAO infExpDAO;

	public ExploratoryDAOTest() {
		super(USER_INSTANCES);
	}

	@Before
	public void setup() {
		cleanup();

		mongoService.createCollection(USER_INSTANCES);
		mongoService.getCollection(USER_INSTANCES).createIndex(new BasicDBObject(USER, 1).append(EXPLORATORY_NAME, 2),
				new IndexOptions().unique(true));


		infExpDAO = new ExploratoryDAO();
		testInjector.injectMembers(infExpDAO);

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

		infExpDAO.insertOne(USER_INSTANCES, instance1);
		infExpDAO.insertOne(USER_INSTANCES, instance2);
		infExpDAO.insertOne(USER_INSTANCES, instance3);

		String testId = infExpDAO.fetchExploratoryId("user1", "exp_name_1");
		assertEquals(testId, "exp1");
	}

	@Test
	public void fetchExploratoryIdNotFound() {
		String testId = infExpDAO.fetchExploratoryId("user1", "exp_name_1");
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

		infExpDAO.insertOne(USER_INSTANCES, instance1);
		infExpDAO.insertOne(USER_INSTANCES, instance2);

		infExpDAO.fetchExploratoryId("user1", "exp_name_1");
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

		infExpDAO.insertOne(USER_INSTANCES, instance1);
		infExpDAO.insertOne(USER_INSTANCES, instance2);

		UserInstanceStatus status = infExpDAO.fetchExploratoryStatus("user1", "exp_name_2");
		assertEquals(status, UserInstanceStatus.FAILED);
	}

	@Test
	public void fetchExploratoryStatusBadValue() {
		UserInstanceDTO instance1 = new UserInstanceDTO()
				.withUser("user1")
				.withExploratoryName("exp_name_1")
				.withStatus("creat");

		infExpDAO.insertOne(USER_INSTANCES, instance1);
		UserInstanceStatus status = infExpDAO.fetchExploratoryStatus("user1", "exp_name_1");
		assertEquals(status, null);
	}

	@Test
	public void fetchExploratoryStatusNotFound() {
		UserInstanceStatus status = infExpDAO.fetchExploratoryStatus("user1", "exp_name_1");
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

		infExpDAO.insertOne(USER_INSTANCES, instance1);
		infExpDAO.insertOne(USER_INSTANCES, instance2);

		infExpDAO.fetchExploratoryStatus("user1", "exp_name_1");
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

		infExpDAO.insertExploratory(instance1);

		UserInstanceDTO instance2 = new UserInstanceDTO()
				.withUser("user1")
				.withExploratoryName("exp_name_2")
				.withExploratoryId("exp2")
				.withStatus("running")
				.withImageName("rstudio")
				.withImageVersion("r-3");

		infExpDAO.insertExploratory(instance2);

		UserInstanceDTO testInstance = infExpDAO.fetchExploratoryFields("user1", "exp_name_2");

		assertEquals(instance2.getExploratoryId(), testInstance.getExploratoryId());
		assertEquals(instance2.getStatus(), testInstance.getStatus());
		assertEquals(instance2.getImageName(), testInstance.getImageName());
		assertEquals(instance2.getImageVersion(), testInstance.getImageVersion());
	}

	@Test
	public void insertExploratorySuccess() {
		UserInstanceDTO instance1 = new UserInstanceDTO()
				.withUser("user1")
				.withExploratoryName("exp_name_1")
				.withExploratoryId("exp1")
				.withStatus("created")
				.withImageName("jupyter")
				.withImageVersion("jupyter-2")
				.withPrivateIp("192.168.1.1");

		infExpDAO.insertExploratory(instance1);

		long insertedCount = mongoService.getCollection(USER_INSTANCES).count();
		assertEquals(1, insertedCount);

		Optional<UserInstanceDTO> testInstance = infExpDAO.findOne(USER_INSTANCES,
				exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
				UserInstanceDTO.class);
		assertTrue(testInstance.isPresent());
		assertEquals(instance1.getExploratoryId(), testInstance.get().getExploratoryId());
		assertEquals(instance1.getStatus(), testInstance.get().getStatus());
		assertEquals(instance1.getImageName(), testInstance.get().getImageName());
		assertEquals(instance1.getImageVersion(), testInstance.get().getImageVersion());
		assertEquals(instance1.getPrivateIp(), testInstance.get().getPrivateIp());
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

		infExpDAO.insertOne(USER_INSTANCES, instance1);
		infExpDAO.insertOne(USER_INSTANCES, instance2);

		ExploratoryStatusDTO newStatus = new ExploratoryStatusDTO();
		newStatus.setUser("user1");
		newStatus.setExploratoryName("exp_name_1");
		newStatus.setStatus("running");
		UpdateResult result = infExpDAO.updateExploratoryStatus(newStatus);

		assertEquals(result.getModifiedCount(), 1);

		Optional<UserInstanceDTO> testInstance = infExpDAO.findOne(USER_INSTANCES,
				exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
				UserInstanceDTO.class);
		assertTrue(testInstance.isPresent());
		assertEquals(UserInstanceStatus.of(testInstance.get().getStatus()), UserInstanceStatus.RUNNING);

		Optional<UserInstanceDTO> testInstance2 = infExpDAO.findOne(USER_INSTANCES,
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

		infExpDAO.insertOne(USER_INSTANCES, instance1);
		infExpDAO.insertOne(USER_INSTANCES, instance2);

		ExploratoryStatusDTO status = new ExploratoryStatusDTO();
		status.setUser("user1");
		status.setExploratoryName("exp_name_1");
		status.setExploratoryId("exp2");
		List<ExploratoryURL> urls = new ArrayList<ExploratoryURL>();
		urls.add(new ExploratoryURL("www.exp1.com", "desc1"));
		urls.add(new ExploratoryURL("www.exp2.com", "desc2"));
		status.setExploratoryUrl(urls);
		status.setStatus("running");
		status.setUptime(new Date(100));

		UpdateResult result = infExpDAO.updateExploratoryFields(status);
		assertEquals(result.getModifiedCount(), 1);

		Optional<UserInstanceDTO> testInstance1 = infExpDAO.findOne(USER_INSTANCES,
				exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
				UserInstanceDTO.class);
		assertTrue(testInstance1.isPresent());

		UserInstanceDTO instance = testInstance1.get();
		assertEquals(instance.getExploratoryId(), status.getExploratoryId());
		assertEquals(instance.getExploratoryUrl().size(), status.getExploratoryUrl().size());
		assertEquals(instance.getExploratoryUrl().get(0), status.getExploratoryUrl().get(0));
		assertEquals(instance.getExploratoryUrl().get(1), status.getExploratoryUrl().get(1));
		assertEquals(instance.getStatus(), status.getStatus());
		assertEquals(instance.getUptime(), status.getUptime());
	}

	@Test
	public void updateExploratoryUrlByIpSuccess() {

		List<ExploratoryURL> urls = new ArrayList<>();
		urls.add(new ExploratoryURL("www.192.168.100.1.com", "desc1"));
		urls.add(new ExploratoryURL("www.192.168.100.1.com", "desc2"));

		UserInstanceDTO instance1 = new UserInstanceDTO()
				.withUser("user1")
				.withExploratoryName("exp_name_1")
				.withExploratoryId("exp1")
				.withStatus("created")
				.withPrivateIp("192.168.100.1")
				.withExploratoryUrl(urls);

		infExpDAO.insertOne(USER_INSTANCES, instance1);

		ExploratoryStatusDTO status = new ExploratoryStatusDTO();
		status.setUser("user1");
		status.setExploratoryName("exp_name_1");
		status.setExploratoryId("exp2");

		status.setPrivateIp("8.8.8.8");

		UpdateResult result = infExpDAO.updateExploratoryFields(status);
		assertEquals(result.getModifiedCount(), 1);

		Optional<UserInstanceDTO> testInstance1 = infExpDAO.findOne(USER_INSTANCES,
				exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
				UserInstanceDTO.class);
		assertTrue(testInstance1.isPresent());

		UserInstanceDTO instance = testInstance1.get();

		// these urls will be final, cause they depends from PrivateIp
		List<ExploratoryURL> urlsNew = new ArrayList<>();
		urlsNew.add(new ExploratoryURL("www.192.168.100.1.com", "desc1"));
		urlsNew.add(new ExploratoryURL("www.192.168.100.1.com", "desc2"));

		assertEquals(instance.getExploratoryId(), status.getExploratoryId());
		assertEquals(instance.getExploratoryUrl().size(), urlsNew.size());
		assertEquals(instance.getExploratoryUrl().get(0), urlsNew.get(0));
		assertEquals(instance.getExploratoryUrl().get(1), urlsNew.get(1));
		assertEquals(instance.getPrivateIp(), status.getPrivateIp());
	}

	@Test
	public void updateExploratoryUrlByUrlSuccess() {

		List<ExploratoryURL> urls = new ArrayList<>();
		urls.add(new ExploratoryURL("www.192.168.100.1.com", "desc1"));
		urls.add(new ExploratoryURL("www.192.168.100.1.com", "desc2"));

		UserInstanceDTO instance1 = new UserInstanceDTO()
				.withUser("user1")
				.withExploratoryName("exp_name_1")
				.withExploratoryId("exp1")
				.withStatus("created")
				.withPrivateIp("192.168.100.1");

		infExpDAO.insertOne(USER_INSTANCES, instance1);

		ExploratoryStatusDTO status = new ExploratoryStatusDTO();
		status.setUser("user1");
		status.setExploratoryName("exp_name_1");
		status.setExploratoryId("exp2");

		status.setExploratoryUrl(urls);

		UpdateResult result = infExpDAO.updateExploratoryFields(status);
		assertEquals(result.getModifiedCount(), 1);

		Optional<UserInstanceDTO> testInstance1 = infExpDAO.findOne(USER_INSTANCES,
				exploratoryCondition(instance1.getUser(), instance1.getExploratoryName()),
				UserInstanceDTO.class);
		assertTrue(testInstance1.isPresent());

		UserInstanceDTO instance = testInstance1.get();

		assertEquals(instance.getExploratoryId(), status.getExploratoryId());
		assertEquals(instance.getExploratoryUrl().size(), urls.size());
		assertEquals(instance.getExploratoryUrl().get(0), urls.get(0));
		assertEquals(instance.getExploratoryUrl().get(1), urls.get(1));

	}
}
