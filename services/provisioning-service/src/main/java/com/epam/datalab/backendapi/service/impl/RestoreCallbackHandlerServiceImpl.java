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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.datalab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import com.epam.datalab.backendapi.service.RestoreCallbackHandlerService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class RestoreCallbackHandlerServiceImpl implements Managed, RestoreCallbackHandlerService {

    @Inject
    private CallbackHandlerDao callbackHandlerDao;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;

    @Override
    public void start() {
        restore();
    }

    @Override
    public void stop() {
        log.info("RestoreCallbackHandlerServiceImpl stopped");
    }

    public void restore() {
        log.info("Restoring callback handlers");
        callbackHandlerDao.findAll().forEach(persistentFileHandler ->
                folderListenerExecutor.start(persistentFileHandler.getDirectory(),
                        Duration.milliseconds(persistentFileHandler.getTimeout()),
                        persistentFileHandler.getHandler()));
        log.info("Successfully restored file handlers");
    }
}
