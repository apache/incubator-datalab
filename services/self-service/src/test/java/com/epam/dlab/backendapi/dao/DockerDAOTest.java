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

import org.junit.*;

import static com.epam.dlab.backendapi.dao.MongoCollections.DOCKER_ATTEMPTS;
import static junit.framework.TestCase.assertEquals;

@Ignore
public class DockerDAOTest extends DAOTestBase {
    private DockerDAO dockerDAO;

    public DockerDAOTest() {
        super(DOCKER_ATTEMPTS);
    }

    @Before
    public void setup() {
        cleanup();
        dockerDAO = new DockerDAO();
        testInjector.injectMembers(dockerDAO);
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
    public void singleWriteSuccess() {
        dockerDAO.writeDockerAttempt("testUser", "action1");
        assertEquals(mongoService.getCollection(DOCKER_ATTEMPTS).count(), 1L);
    }

    @Test
    public void multipleWriteSuccess() {
        dockerDAO.writeDockerAttempt("testUser", "action1");
        dockerDAO.writeDockerAttempt("testUser", "action2");
        assertEquals(mongoService.getCollection(DOCKER_ATTEMPTS).count(), 2L);
    }
}
