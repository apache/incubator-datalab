/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.backendapi.roles;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.SecurityDAO;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserRolesTest {

    @Mock
    private SecurityDAO dao;

    @Test
    @SuppressWarnings("unchecked")
    public void checkAccess() {
        UserInfo userInfoDev = new UserInfo("developer", "token123");
        userInfoDev.addRole("dev");
        List<String> shapes1 = new ArrayList<>();
        shapes1.add("shape_1");
        shapes1.add("shape_2");
        shapes1.add("shape_3");
        ArrayList<String> devGroup = new ArrayList<>();
        devGroup.add("dev");
        Document doc1 = new Document().append("exploratory_shapes", shapes1).append("groups", devGroup);

        UserInfo userInfoTest = new UserInfo("tester", "token321");
        userInfoTest.addRole("test");
        List<String> shapes2 = new ArrayList<>();
        shapes2.add("shape_2");
        shapes2.add("shape_3");
        ArrayList<String> testGroup = new ArrayList<>();
        testGroup.add("test");
        Document doc2 = new Document().append("exploratory_shapes", shapes2).append("groups", testGroup);

        MongoCursor cursor = mock(MongoCursor.class);

        FindIterable mockIterable = mock(FindIterable.class);

        when(dao.getRoles()).thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(doc1).thenReturn(doc2);
        UserRoles.initialize(dao, true);

        assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "shape_1", userInfoDev.getRoles()));
        assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "shape_2", userInfoDev.getRoles()));
        assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "shape_3", userInfoDev.getRoles()));
        assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "someShape", userInfoDev.getRoles()));

        assertFalse(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "shape_1", userInfoTest.getRoles()));
        assertFalse(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "shape_2", userInfoTest.getRoles()));
        assertFalse(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "shape_3", userInfoTest.getRoles()));
        assertTrue(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "someShape", userInfoTest.getRoles()));
    }
}
