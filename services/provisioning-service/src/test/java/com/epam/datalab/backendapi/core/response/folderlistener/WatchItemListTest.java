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
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WatchItemListTest {

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


    private String getDirectory() {
        return "./";
    }

    private String getFileName() {
        return UUID + ".json";
    }

    @Test
    public void checkGetters() {
        WatchItemList items = new WatchItemList(getDirectory(), null);

        assertEquals(0, items.size());

        items.append(fHandler, timeoutMillis, fileLengthCheckDelay);
        assertEquals(1, items.size());

        WatchItem item = items.get(0);
        assertNotNull(item);
        assertEquals(0, items.getIndex(UUID));
        assertEquals(item, items.getItem(getFileName()));
        items.remove(0);
        assertEquals(0, items.size());
    }


    @Test
    public void processItem() throws InterruptedException, ExecutionException {
        WatchItemList items = new WatchItemList(getDirectory(), null);
        items.append(fHandler, timeoutMillis, fileLengthCheckDelay);

        WatchItem item = items.get(0);

        assertEquals(false, items.processItem(item));

        item.setFileName(getFileName());
        assertEquals(true, items.processItem(item));

        assertEquals(ItemStatus.INPROGRESS, item.getStatus());
        item.getFutureResultSync();
    }

    @Test
    public void processItemAll() throws InterruptedException, ExecutionException {
        WatchItemList items = new WatchItemList(getDirectory(), null);
        items.append(fHandler, timeoutMillis, fileLengthCheckDelay);

        WatchItem item = items.get(0);

        assertEquals(0, items.processItemAll());

        item.setFileName(getFileName());
        assertEquals(1, items.processItemAll());

        assertEquals(ItemStatus.INPROGRESS, item.getStatus());
        item.getFutureResultSync();
    }

}
