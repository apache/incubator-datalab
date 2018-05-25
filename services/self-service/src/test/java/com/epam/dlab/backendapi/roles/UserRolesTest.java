package com.epam.dlab.backendapi.roles;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SecurityDAO;
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

		assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "shape_1"));
		assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "shape_2"));
		assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "shape_3"));
		assertTrue(UserRoles.checkAccess(userInfoDev, RoleType.EXPLORATORY_SHAPES, "someShape"));

		assertFalse(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "shape_1"));
		assertFalse(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "shape_2"));
		assertFalse(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "shape_3"));
		assertTrue(UserRoles.checkAccess(userInfoTest, RoleType.EXPLORATORY_SHAPES, "someShape"));
	}
}
