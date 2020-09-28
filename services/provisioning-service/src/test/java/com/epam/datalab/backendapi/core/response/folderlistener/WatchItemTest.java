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
import com.epam.datalab.backendapi.core.response.folderlistener.WatchItem.ItemStatus;
import io.dropwizard.util.Duration;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class WatchItemTest {

    private static final String UUID = "123";

    private final FileHandlerCallback fHandler = new FileHandler(UUID);

    private final long timeoutMillis = 1000;

    private final long fileLengthCheckDelay = 10000;

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
            return true;
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


    private WatchItem getWatchItem() {
        return new WatchItem(fHandler, timeoutMillis, fileLengthCheckDelay);
    }

    private String getFileName() {
        return UUID + ".json";
    }

    private String getDirectory() {
        return "./";
    }

    private AsyncFileHandler getSupplier(WatchItem item) {
        return new AsyncFileHandler(item.getFileName(), getDirectory(), item.getFileHandlerCallback(),
                Duration.milliseconds(item.getFileLengthCheckDelay()));
    }

    @Test
    public void isExpired() {
        WatchItem item;

        item = new WatchItem(fHandler, -1, fileLengthCheckDelay);
        assertEquals(true, item.isExpired());

        item = getWatchItem();
        assertEquals(false, item.isExpired());
    }

    @Test
    public void status() throws InterruptedException, ExecutionException {
        WatchItem item;

        item = new WatchItem(fHandler, -1, fileLengthCheckDelay);
        assertEquals(ItemStatus.TIMEOUT_EXPIRED, item.getStatus());

        item.setFileName(getFileName());
        assertEquals(ItemStatus.FILE_CAPTURED, item.getStatus());

        item = getWatchItem();
        assertEquals(ItemStatus.WAIT_FOR_FILE, item.getStatus());

        item.setFileName(getFileName());
        assertEquals(ItemStatus.FILE_CAPTURED, item.getStatus());

        item.setFuture(CompletableFuture.supplyAsync(getSupplier(item)));
        assertEquals(ItemStatus.INPROGRESS, item.getStatus());

        assertEquals(null, item.getFutureResult());
        item.getFutureResultSync();

        assertEquals(ItemStatus.IS_DONE, item.getStatus());

        item.setFuture(CompletableFuture.supplyAsync(getSupplier(item)));
        item.getFuture().cancel(false);
        assertEquals(ItemStatus.IS_CANCELED, item.getStatus());

        //IS_INTERRUPTED, IS_FAILED
    }

    @Test
    public void futureResult() throws InterruptedException, ExecutionException {
        WatchItem item = getWatchItem();

        item.setFileName(getFileName());
        item.setFuture(CompletableFuture.supplyAsync(getSupplier(item)));

        assertEquals(false, item.getFutureResultSync());
        assertEquals(false, item.getFutureResult());
    }
}
