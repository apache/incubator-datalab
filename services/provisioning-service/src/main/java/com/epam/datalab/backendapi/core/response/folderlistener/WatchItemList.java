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
import com.epam.datalab.backendapi.core.commands.DockerCommands;
import com.epam.datalab.backendapi.core.response.folderlistener.WatchItem.ItemStatus;
import com.epam.datalab.backendapi.core.response.handlers.PersistentFileHandler;
import com.epam.datalab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * List of the file handlers for processing.
 */
public class WatchItemList {
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchItemList.class);

    /**
     * Directory name.
     */
    private final String directoryName;
    /**
     * Directory full name.
     */
    private final String directoryFullName;
    private final CallbackHandlerDao handlerDao;

    /**
     * List of the file handlers.
     */
    private final Vector<WatchItem> list = new Vector<>();

    /**
     * UUID of the file handler for search.
     */
    private String uuidSearch;

    /**
     * File handler for search.
     */
    private final FileHandlerCallback handlerSearch = new FileHandlerCallback() {
        @Override
        public String getUUID() {
            return uuidSearch;
        }

        @Override
        public boolean checkUUID(String uuid) {
            return uuidSearch.equals(uuid);
        }

        @Override
        public void handleError(String errorMessage) {
        }

        @Override
        public String getUser() {
            return "DATALAB";
        }

        @Override
        public boolean handle(String fileName, byte[] content) throws Exception {
            return false;
        }
    };

    /**
     * Creates instance of the file handlers for processing.
     *
     * @param directoryName listener directory name.
     * @param handlerDao    data access object for callback handler
     */
    public WatchItemList(String directoryName, CallbackHandlerDao handlerDao) {
        this.directoryName = directoryName;
        this.directoryFullName = Paths.get(directoryName).toAbsolutePath().toString();
        this.handlerDao = handlerDao;
    }

    /**
     * Returns directory name.
     */
    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * Returns directory full name.
     */
    public String getDirectoryFullName() {
        return directoryFullName;
    }


    /**
     * Appends the file handler to the list and returns it.
     *
     * @param fileHandlerCallback  File handler for processing.
     * @param timeoutMillis        Timeout waiting for the file creation in milliseconds.
     * @param fileLengthCheckDelay Timeout waiting for the file writing in milliseconds.
     * @return Instance of the file handler.
     */
    public WatchItem append(FileHandlerCallback fileHandlerCallback, long timeoutMillis, long fileLengthCheckDelay) {
        if (Objects.nonNull(handlerDao)) {
            handlerDao.upsert(new PersistentFileHandler(fileHandlerCallback, timeoutMillis, directoryName));
        }
        WatchItem item = new WatchItem(fileHandlerCallback, timeoutMillis, fileLengthCheckDelay);
        synchronized (this) {
            int index = Collections.binarySearch(list, item);
            if (index < 0) {
                index = -index;
                if (index > list.size()) {
                    list.add(item);
                } else {
                    list.add(index - 1, item);
                }
            } else {
                LOGGER.warn("Handler for UUID {} for folder {} will be replaced. Old item is: {}",
                        fileHandlerCallback.getUUID(), directoryFullName, get(index));
                list.set(index, item);
            }
        }
        return item;
    }

    /**
     * Appends the file handler to the list for the existing file and returns it. If the file name
     * is <b>null</b> this means that file does not exist and equal to call method
     * {@link WatchItemList#append(FileHandlerCallback, long, long)}.
     *
     * @param fileHandlerCallback  File handler for processing.
     * @param timeoutMillis        Timeout waiting for the file creation in milliseconds.
     * @param fileLengthCheckDelay Timeout waiting for the file writing in milliseconds.
     * @param fileName             file name.
     * @return Instance of file handler.
     */
    public WatchItem append(FileHandlerCallback fileHandlerCallback, long timeoutMillis, long fileLengthCheckDelay,
                            String fileName) {
        WatchItem item = append(fileHandlerCallback, timeoutMillis, fileLengthCheckDelay);
        if (fileName != null) {
            item.setFileName(fileName);
        }
        return item;
    }

    /**
     * Removes the file handler from list.
     *
     * @param index index of the file handler.
     */
    public void remove(int index) {

        final WatchItem watchItem = list.remove(index);
        if (Objects.nonNull(handlerDao) && watchItem.getStatus() != ItemStatus.IS_FAILED) {
            handlerDao.remove(watchItem.getFileHandlerCallback().getId());
        }
    }

    /**
     * Returns the number of the file handlers in list.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the file handler.
     *
     * @param index index of the file handler.
     */
    public WatchItem get(int index) {
        return list.get(index);
    }

    /**
     * Returns the index of the file handler in the list if it is contained in the list,
     * otherwise returns (-(insertion point) - 1).
     *
     * @param uuid UUID of the file handler.
     */
    public int getIndex(String uuid) {
        uuidSearch = uuid;
        return Collections.binarySearch(list, new WatchItem(handlerSearch, 0, 0));
    }

    /**
     * Returns the instance of the file handler if it contained in the list,
     * otherwise returns <b>null</b>.
     */
    public WatchItem getItem(String fileName) {
        String uuid = DockerCommands.extractUUID(fileName);
        int index = getIndex(uuid);
        if (index < 0) {
            return null;
        }
        return get(index);
    }

    /**
     * Runs asynchronously the file handler in the {@link ForkJoinPool#commonPool()}.
     *
     * @param item the file handler.
     */
    private void runAsync(WatchItem item) {
        LOGGER.trace("Process file {} for folder {}", item.getFileName(), directoryFullName);
        item.setFuture(CompletableFuture.supplyAsync(
                new AsyncFileHandler(item.getFileName(), getDirectoryName(),
                        item.getFileHandlerCallback(), Duration.milliseconds(item.getFileLengthCheckDelay()))));
    }

    /**
     * Runs the file processing asynchronously if it have status {@link ItemStatus#FILE_CAPTURED} and returns
     * <b>true</b>,
     * otherwise <b>false</b>.
     *
     * @param item the file handler.
     */
    public boolean processItem(WatchItem item) {
        if (item.getStatus() == ItemStatus.FILE_CAPTURED) {
            runAsync(item);
            return true;
        }

        if (item.isExpired()) {
            LOGGER.warn("Watch time has expired for UUID {} in folder {}", item.getFileHandlerCallback().getUUID(),
                    directoryFullName);
        }
        return false;
    }

    /**
     * Checks all the file handlers and runs the file processing for it if have status
     * {@link ItemStatus#FILE_CAPTURED}.
     */
    public int processItemAll() {
        int count = 0;
        synchronized (list) {
            for (int i = 0; i < size(); i++) {
                WatchItem item = list.get(i);
                if (item.getStatus() == ItemStatus.FILE_CAPTURED && processItem(item)) {
                    count++;
                }
            }
        }
        if (count > 0) {
            LOGGER.trace("Starts processing {} files for folder {}", count, directoryName);
        }
        return count;
    }

}
