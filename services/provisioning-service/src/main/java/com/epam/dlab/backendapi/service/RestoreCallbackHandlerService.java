/*
 * **************************************************************************
 *
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ***************************************************************************
 */

package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class RestoreCallbackHandlerService implements Managed {

	@Inject
	private CallbackHandlerDao callbackHandlerDao;
	@Inject
	private FolderListenerExecutor folderListenerExecutor;

	@Override
	public void start() {
		log.info("Restoring callback handlers");
		callbackHandlerDao.findAll().forEach(persistentFileHandler ->
				folderListenerExecutor.start(persistentFileHandler.getDirectory(),
						Duration.milliseconds(persistentFileHandler.getTimeout()),
						persistentFileHandler.getHandler()));
		log.info("Successfully restored file handlers");
	}

	@Override
	public void stop() {
		log.info("RestoreCallbackHandlerService stopped");
	}
}
