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

package com.epam.dlab.backendapi.core.response.handlers.dao;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.DockerWarmuper;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.response.handlers.LibListCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.PersistentFileHandler;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemCallbackHandlerDaoTest {

	@Mock
	private ObjectMapper mapper;
	@Mock
	private ProvisioningServiceApplicationConfiguration configuration;
	@Mock
	private CallbackHandlerDao dao;
	@InjectMocks
	private FileSystemCallbackHandlerDao fileSystemCallbackHandlerDao;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void createHandlersFolder() throws IOException {
		folder.newFolder("opt", "handlers");
	}


	@Test
	public void upsert() throws IOException {
		final String handlersFolders = getHandlersFolder();
		when(configuration.getHandlerDirectory()).thenReturn(handlersFolders);
		when(mapper.writeValueAsBytes(any())).thenReturn("{'test': 'test'}".getBytes());
		final PersistentFileHandler persistentFileHandler =
				new PersistentFileHandler(new LibListCallbackHandler(null,
						DockerAction.LIB_LIST, "uuid", "test", "das"), 1L, "/opt/test");

		fileSystemCallbackHandlerDao.upsert(persistentFileHandler);

		verify(configuration, times(2)).getHandlerDirectory();
		verify(mapper).writeValueAsBytes(refEq(persistentFileHandler));
		assertTrue(new File(handlersFolders + File.separator + "LibListCallbackHandler_uuid.json").exists());
		verifyNoMoreInteractions(mapper, configuration, dao);
	}

	@Test
	public void upsertTwoSimilarHandlers() throws IOException {
		final String handlersFolders = getHandlersFolder();
		when(configuration.getHandlerDirectory()).thenReturn(handlersFolders);
		when(mapper.writeValueAsBytes(any())).thenReturn("{'test': 'test'}".getBytes());
		final PersistentFileHandler persistentFileHandler1 = new PersistentFileHandler(new DockerWarmuper()
				.new DockerFileHandlerCallback("sameUUID"), 1L, "/opt/test");
		final PersistentFileHandler persistentFileHandler2 =
				new PersistentFileHandler(new LibListCallbackHandler(null,
						DockerAction.LIB_LIST, "sameUUID", "test", "das1"), 1L, "/opt/test");
		final PersistentFileHandler persistentFileHandler3 =
				new PersistentFileHandler(new LibListCallbackHandler(null,
						DockerAction.LIB_LIST, "anotherUUID", "test", "das2"), 1L, "/opt/test");


		fileSystemCallbackHandlerDao.upsert(persistentFileHandler1);
		fileSystemCallbackHandlerDao.upsert(persistentFileHandler2);
		fileSystemCallbackHandlerDao.upsert(persistentFileHandler3);

		verify(configuration, times(6)).getHandlerDirectory();
		verify(mapper).writeValueAsBytes(refEq(persistentFileHandler1));
		verify(mapper).writeValueAsBytes(refEq(persistentFileHandler2));
		verify(mapper).writeValueAsBytes(refEq(persistentFileHandler3));
		assertTrue(new File(handlersFolders + File.separator + "LibListCallbackHandler_sameUUID.json").exists());
		assertTrue(new File(handlersFolders + File.separator + "LibListCallbackHandler_anotherUUID.json").exists());
		assertFalse(new File(handlersFolders + File.separator + "DockerFileHandlerCallback_sameUUID.json").exists());
		verifyNoMoreInteractions(mapper, configuration, dao);
	}

	@Test
	public void findAll() throws IOException {
		final File handler1 = getHandlerFile("test1.json");
		final File handler2 = getHandlerFile("test2.json");
		final String handlersFolder = getHandlersFolder();

		when(configuration.getHandlerDirectory()).thenReturn(handlersFolder);
		final PersistentFileHandler persistentFileHandler1 = new PersistentFileHandler(null, 1L, "/opt");
		final PersistentFileHandler persistentFileHandler2 = new PersistentFileHandler(null, 2L, "/opt");
		when(mapper.readValue(any(File.class), Matchers.<Class<PersistentFileHandler>>any())).thenReturn
				(persistentFileHandler1).thenReturn(persistentFileHandler2);
		final List<PersistentFileHandler> handlers = fileSystemCallbackHandlerDao.findAll();

		assertEquals(2, handlers.size());

		verify(configuration).getHandlerDirectory();
		verify(mapper).readValue(handler1, PersistentFileHandler.class);
		verify(mapper).readValue(handler2, PersistentFileHandler.class);
		verifyNoMoreInteractions(mapper, dao, configuration);
	}

	@Test
	public void findAllWithException() throws IOException {
		new File(getHandlersFolder()).delete();
		when(configuration.getHandlerDirectory()).thenReturn(getHandlersFolder());
		when(mapper.readValue(any(File.class), Matchers.<Class<PersistentFileHandler>>any())).thenThrow(new
				RuntimeException("Exception"));
		final List<PersistentFileHandler> handlers = fileSystemCallbackHandlerDao.findAll();

		assertEquals(0, handlers.size());

		verify(configuration).getHandlerDirectory();
		verifyNoMoreInteractions(mapper, dao, configuration);
	}

	@Test
	public void findAllWithOneWrongHandlerFile() throws IOException {
		final File handler1 = getHandlerFile("test1.json");
		final File handler2 = getHandlerFile("test2.json");
		final String handlersFolder = getHandlersFolder();

		final PersistentFileHandler persistentFileHandler1 = new PersistentFileHandler(null, 1L, "/opt");

		when(configuration.getHandlerDirectory()).thenReturn(handlersFolder);
		when(mapper.readValue(any(File.class), Matchers.<Class<PersistentFileHandler>>any())).thenReturn
				(persistentFileHandler1).thenThrow(new RuntimeException("Exception"));

		final List<PersistentFileHandler> handlers = fileSystemCallbackHandlerDao.findAll();

		assertEquals(1, handlers.size());

		verify(configuration).getHandlerDirectory();
		verify(mapper).readValue(handler1, PersistentFileHandler.class);
		verify(mapper).readValue(handler2, PersistentFileHandler.class);
		verifyNoMoreInteractions(mapper, dao, configuration);
	}

	private String getHandlersFolder() {
		return folder.getRoot().getAbsolutePath() +
				File.separator + "opt" + File.separator + "handlers";
	}

	private File getHandlerFile(String handlerFileName) throws IOException {
		return folder.newFile(File.separator + "opt" + File.separator + "handlers" + File.separator +
				handlerFileName);
	}

	@Test
	public void remove() throws IOException {
		final File handler = getHandlerFile("test1.json");
		handler.createNewFile();
		final String handlersFolder = getHandlersFolder();

		when(configuration.getHandlerDirectory()).thenReturn(handlersFolder);
		fileSystemCallbackHandlerDao.remove("test1");

		assertFalse(handler.exists());

		verify(configuration).getHandlerDirectory();
		verifyNoMoreInteractions(configuration, dao, mapper);
	}

	@Test
	public void removeWithException() throws IOException {
		final String handlersFolder = getHandlersFolder();

		when(configuration.getHandlerDirectory()).thenReturn(handlersFolder);
		expectedException.expect(DlabException.class);
		fileSystemCallbackHandlerDao.remove("test1.json");
	}
}