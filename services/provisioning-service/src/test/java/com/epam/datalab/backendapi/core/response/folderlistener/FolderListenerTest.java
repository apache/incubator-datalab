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

package com.epam.datalab.backendapi.core.response.folderlistener;

import com.epam.datalab.backendapi.core.FileHandlerCallback;
import com.epam.datalab.util.ServiceUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class FolderListenerTest {
    private final long maxWaitTimeoutMillis = 30000;

    private final long timeoutMillis = 2000;

    private final long fileLengthCheckDelay = 1000;

    private boolean handleResult = true;

    public class FileHandler implements FileHandlerCallback {
        private final String uuid;

        public FileHandler(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public String getUUID() {
            return uuid;
        }

        @Override
        public boolean checkUUID(String uuid) {
            return this.uuid.equals(uuid);
        }

        @Override
        public boolean handle(String fileName, byte[] content) throws Exception {
            if (!handleResult) {
                throw new Exception("Test exception");
            }
            return handleResult;
        }

        @Override
        public void handleError(String errorMessage) {
            System.out.println("handleError called for UUID " + getUUID());
        }

        @Override
        public String getUser() {
            return null;
        }
    }

    private String getFileName(String uuid) {
        return "FolderListenerTest_" + uuid + ".json";
    }

    private String getDirectory() {
        return ServiceUtils.getUserDir();
    }

    private void removeFile(String uuid) throws IOException {
        File file = new File(getDirectory(), getFileName(uuid));
        if (file.exists()) {
            file.delete();
        }
    }

    private void createFile(String uuid) throws IOException {
        File file = new File(getDirectory(), getFileName(uuid));
        removeFile(uuid);
        FileWriter f = new FileWriter(file);

        f.write("test");
        f.flush();
        f.close();
    }


    private void processFile(WatchItem item) throws InterruptedException, IOException {
        long expiredTime = System.currentTimeMillis() + maxWaitTimeoutMillis;
        while (!FolderListener.getListeners().isEmpty() &&
                !FolderListener.getListeners().get(0).isListen()) {
            if (expiredTime < System.currentTimeMillis()) {
                throw new InterruptedException("Timeout has been expired");
            }
            Thread.sleep(100);
        }

        expiredTime = System.currentTimeMillis() + maxWaitTimeoutMillis;
        while (item.getFuture() == null) {
            if (expiredTime < System.currentTimeMillis()) {
                throw new InterruptedException("Timeout has been expired");
            }
            Thread.sleep(100);
        }
    }

    @Test
    public void listen() throws InterruptedException, ExecutionException, IOException {
        Integer uuid = 1;
        FileHandlerCallback fHandler;
        WatchItem item;

        handleResult = false;
        fHandler = new FileHandler(uuid.toString());
        item = FolderListener.listen(getDirectory(), fHandler, timeoutMillis, fileLengthCheckDelay, null);
        FolderListener listener = FolderListener.getListeners().get(0);
        assertEquals(false, listener.isListen());
        assertEquals(true, listener.isAlive());
        System.out.println("TEST process FALSE");

        uuid = 2;
        createFile(uuid.toString());
        fHandler = new FileHandler(uuid.toString());
        item = FolderListener.listen(getDirectory(), fHandler, timeoutMillis, fileLengthCheckDelay, null);
        processFile(item);
        assertEquals(true, listener.isListen());
        assertEquals(false, item.getFutureResultSync());
        assertEquals(false, item.getFutureResult());

        System.out.println("TEST process TRUE");
        uuid = 3;
        handleResult = true;
        createFile(uuid.toString());
        fHandler = new FileHandler(uuid.toString());
        item = FolderListener.listen(getDirectory(), fHandler, timeoutMillis, fileLengthCheckDelay, null);
        processFile(item);
        assertEquals(true, item.getFutureResultSync());
        assertEquals(true, item.getFutureResult());

        System.out.println("TEST process with out listen");
        uuid = 4;
        createFile(uuid.toString());
        fHandler = new FileHandler(uuid.toString());
        item = FolderListener.listen(getDirectory(), fHandler, timeoutMillis, fileLengthCheckDelay, getFileName(uuid
                .toString()), null);

        long expiredTime = System.currentTimeMillis() + maxWaitTimeoutMillis;
        while (item.getFuture() == null) {
            if (expiredTime < System.currentTimeMillis()) {
                throw new InterruptedException("Timeout has been expired");
            }
            Thread.sleep(100);
        }
        assertEquals(true, item.getFutureResultSync());
        assertEquals(true, item.getFutureResult());

        FolderListener.terminateAll();
        expiredTime = System.currentTimeMillis() + maxWaitTimeoutMillis;
        while (FolderListener.getListeners().size() > 0) {
            if (expiredTime < System.currentTimeMillis()) {
                throw new InterruptedException("Timeout has been expired");
            }
            Thread.sleep(100);
        }

        System.out.println("All listen tests passed");


        for (int i = 1; i <= uuid; i++) {
            removeFile(String.valueOf(i));
        }
    }

}
