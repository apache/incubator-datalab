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
import com.epam.datalab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import com.epam.datalab.exceptions.DatalabException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.epam.datalab.backendapi.core.Constants.JSON_EXTENSION;

/**
 * Listen the directories for the files creation and runs the file processing by {@link AsyncFileHandler}.
 */
public class FolderListener implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FolderListener.class);
    /**
     * Timeout of the check the file creation in milliseconds.
     */
    public static final long LISTENER_TIMEOUT_MILLLIS = 1000;
    /**
     * Timeout of the idle for the folder listener in milliseconds.
     */
    public static final long LISTENER_IDLE_TIMEOUT_MILLLIS = 600L * 1000L;
    /**
     * Timeout of waiting for the directory creation in milliseconds.
     */
    private static final long WAIT_DIR_TIMEOUT_MILLIS = 500;

    /**
     * List of the folder listeners.
     */
    private static final List<FolderListener> listeners = new ArrayList<>();
    /**
     * Thread of the folder listener.
     */
    private Thread thread;
    /**
     * List of the file handles.
     */
    private WatchItemList itemList;
    /**
     * Flag of listening status.
     */
    private boolean isListen = false;
    /**
     * Time when expired of idle for folder listener in milliseconds.
     */
    private long expiredIdleMillis = 0;


    private FolderListener() {
    }

    /**
     * Creates thread of the folder listener
     *
     * @param directoryName Name of directory.
     * @param dao
     */
    private FolderListener(String directoryName, CallbackHandlerDao dao) {
        itemList = new WatchItemList(directoryName, dao);
    }

    /**
     * Appends the file handler for processing to the folder listener and returns instance of the file handler.
     *
     * @param directoryName        Name of directory for listen.
     * @param fileHandlerCallback  File handler for processing.
     * @param timeoutMillis        Timeout waiting for the file creation in milliseconds.
     * @param fileLengthCheckDelay Timeout waiting for the file writing in milliseconds.
     * @param callbackHandlerDao   callbackHandlerDao for handlers
     * @return Instance of the file handler.
     */
    public static WatchItem listen(String directoryName, FileHandlerCallback fileHandlerCallback,
                                   long timeoutMillis, long fileLengthCheckDelay,
                                   CallbackHandlerDao callbackHandlerDao) {
        return listen(directoryName, fileHandlerCallback, timeoutMillis, fileLengthCheckDelay, null,
                callbackHandlerDao);
    }

    /**
     * Appends the file handler for processing to the folder listener for the existing file and returns
     * instance of the file handler. If the file name is <b>null</b> this means that file does not exist
     * and equal to call method
     * {@link FolderListener#listen(String, FileHandlerCallback, long, long, CallbackHandlerDao)}.
     *
     * @param directoryName        Name of directory for listen.
     * @param fileHandlerCallback  File handler for processing.
     * @param timeoutMillis        Timeout waiting for the file creation in milliseconds.
     * @param fileLengthCheckDelay Timeout waiting for the file writing in milliseconds.
     * @param fileName             file name.
     * @param callbackHandlerDao   callbackHandlerDao for handlers
     * @return Instance of the file handler.
     */
    public static WatchItem listen(String directoryName, FileHandlerCallback fileHandlerCallback, long timeoutMillis,
                                   long fileLengthCheckDelay, String fileName, CallbackHandlerDao callbackHandlerDao) {
        FolderListener listener;
        WatchItem item;

        LOGGER.trace("Looking for folder listener to folder \"{}\" ...", directoryName);
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                listener = listeners.get(i);
                if (listener.itemList.getDirectoryName().equals(directoryName)) {
                    if (listener.isAlive()) {
                        LOGGER.debug("Folder listener \"{}\" found. Append file handler for UUID {}",
                                directoryName, fileHandlerCallback.getUUID());
                        item = listener.itemList.append(fileHandlerCallback, timeoutMillis, fileLengthCheckDelay,
                                fileName);
                        return item;
                    } else {
                        LOGGER.warn("Folder listener \"{}\" is dead and will be removed", directoryName);
                        listeners.remove(i);
                        break;
                    }
                }
            }
            LOGGER.debug("Folder listener \"{}\" not found. Create new listener and append file handler for UUID {}",
                    directoryName, fileHandlerCallback.getUUID());
            listener = new FolderListener(directoryName, callbackHandlerDao);
            item = listener.itemList.append(fileHandlerCallback, timeoutMillis, fileLengthCheckDelay, fileName);
            listeners.add(listener);
            listener.start();
        }
        return item;
    }

    /**
     * Terminates all the folder listeners.
     */
    public static void terminateAll() {
        FolderListener[] array;
        synchronized (listeners) {
            array = listeners.toArray(new FolderListener[listeners.size()]);
        }
        for (int i = 0; i < array.length; i++) {
            array[i].terminate();
        }
    }

    /**
     * Returns the list of folder listeners.
     */
    public static List<FolderListener> getListeners() {
        return listeners;
    }

    /**
     * Starts the thread of the folder listener.
     */
    protected void start() {
        thread = new Thread(this, getClass().getSimpleName() + "-" + listeners.size());
        thread.start();
    }

    /**
     * Terminates the thread of the folder listener.
     */
    protected void terminate() {
        if (thread != null) {
            LOGGER.debug("Folder listener \"{}\" will be terminate", getDirectoryName());
            thread.interrupt();
        }
    }

    /**
     * Returns <b>true</b> if the folder listener thread is running and is alive, otherwise <b>false</b>.
     */
    public boolean isAlive() {
        return (thread != null && thread.isAlive());
    }

    /**
     * Returns <b>true</b> if the folder listener is listening the folder.
     */
    public boolean isListen() {
        return isListen;
    }


    /**
     * Returns the list of the file handlers.
     */
    public WatchItemList getItemList() {
        return itemList;
    }

    /**
     * Returns the full name of directory.
     */
    public String getDirectoryName() {
        return itemList.getDirectoryFullName();
    }

    /**
     * Waiting for the directory creation and returns <b>true</b> if it exists or created.
     * If timeout has expired and directory was not created returns <b>false</b>
     */
    private boolean waitForDirectory() throws InterruptedException {
        File file = new File(getDirectoryName());
        if (file.exists()) {
            return true;
        } else {
            LOGGER.trace("Folder listener \"{}\" waiting for the directory creation", getDirectoryName());
        }

        long expiredTimeMillis = itemList.get(0).getExpiredTimeMillis();
        while (expiredTimeMillis >= System.currentTimeMillis()) {
            Thread.sleep(WAIT_DIR_TIMEOUT_MILLIS);
            if (file.exists()) {
                return true;
            }
        }
        LOGGER.error("Folder listener \"{}\" error. Timeout has expired and directory does not exist",
                getDirectoryName());
        return false;
    }

    /**
     * Initializes the thread of the folder listener. Returns <b>true</b> if the initialization
     * completed successfully. Returns <b>false</b> if all the file handlers has been processed
     * or initialization fails.
     */
    private boolean init() {
        LOGGER.trace("Folder listener initializing for \"{}\" ...", getDirectoryName());

        try {
            if (!waitForDirectory()) {
                return false;
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Folder listener \"{}\" has been interrupted", getDirectoryName());
            Thread.currentThread().interrupt();
            return false;
        }

        processStatusItems();
        if (itemList.size() == 0) {
            LOGGER.debug("Folder listener \"{}\" have no files and will be finished", getDirectoryName());
            return false;
        }

        LOGGER.trace("Folder listener has been initialized for \"{}\" ...", getDirectoryName());
        return true;
    }

    /**
     * Process all the file handlers if need and removes all expired, processed or interrupted
     * the file handlers from the list of the file handlers.
     */
    private void processStatusItems() {
        int i = 0;

        if (itemList.size() > 0) {
            expiredIdleMillis = 0;
        }
        itemList.processItemAll();

        synchronized (itemList) {
            while (i < itemList.size()) {
                final WatchItem item = itemList.get(i);
                final ItemStatus status = item.getStatus();
                final String uuid = item.getFileHandlerCallback().getUUID();

                switch (status) {
                    case WAIT_FOR_FILE:
                    case FILE_CAPTURED:
                    case INPROGRESS:
                        // Skip
                        i++;
                        continue;
                    case TIMEOUT_EXPIRED:
                        LOGGER.warn("Folder listener \"{}\" remove expired file handler for UUID {}", getDirectoryName
                                (), uuid);
                        try {
                            item.getFileHandlerCallback().handleError("Request timeout expired");
                        } catch (Exception e) {
                            LOGGER.error("Folder listener \"{}\" caused exception for UUID {}", getDirectoryName(),
                                    uuid, e);
                        }
                        break;
                    case IS_DONE:
                        if (item.getFutureResult()) {
                            LOGGER.trace("Folder listener \"{}\" remove processed file handler for UUID {}, handler " +
                                    "result is {}", getDirectoryName(), uuid, item.getFutureResult());
                        } else {
                            LOGGER.warn("Folder listener \"{}\" remove processed file handler for UUID {}, handler " +
                                    "result is {}", getDirectoryName(), uuid, item.getFutureResult());
                        }
                        break;
                    case IS_CANCELED:
                        LOGGER.debug("Folder listener \"{}\" remove canceled file handler for UUID {}",
                                getDirectoryName(), uuid);
                        break;
                    case IS_FAILED:
                        LOGGER.warn("Folder listener \"{}\" remove failed file handler for UUID {}", getDirectoryName
                                (), uuid);
                        break;
                    case IS_INTERRUPTED:
                        LOGGER.debug("Folder listener \"{}\" remove iterrupted file handler for UUID {}",
                                getDirectoryName(), uuid);
                        break;
                    default:
                        continue;
                }
                itemList.remove(i);
            }
        }

        if (expiredIdleMillis == 0 && itemList.size() == 0) {
            expiredIdleMillis = System.currentTimeMillis() + LISTENER_IDLE_TIMEOUT_MILLLIS;
        }
    }

    /**
     * Removes the listener from the list of folder listeners if the the file handler list is empty
     * and idle time has expired or if <b>force</b> flag has been set to <b>true</b>.
     *
     * @param force the flag of remove the folder listener immediately.
     * @return <b>true</b> if the folder listener has been removed otherwise <b>false</>.
     */
    private boolean removeListener(boolean force) {
        synchronized (listeners) {
            if (force || (expiredIdleMillis != 0 && expiredIdleMillis < System.currentTimeMillis())) {
                for (int i = 0; i < listeners.size(); i++) {
                    if (listeners.get(i) == this) {
                        isListen = false;
                        listeners.remove(i);
                        LOGGER.debug("Folder listener \"{}\" has been removed from pool", getDirectoryName());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find and return the list of files to process.
     */
    private String[] getNewFiles() {
        File dir = new File(getDirectoryName());
        return dir.list((File dir1, String name) -> {
            if (name.toLowerCase().endsWith(JSON_EXTENSION)) {
                WatchItem item = itemList.getItem(name);
                return (item != null && item.getStatus() == ItemStatus.WAIT_FOR_FILE);
            }
            return false;
        });
    }

    /**
     * Waiting for files and process it.
     */
    private void pollFile() {
        try {
            isListen = true;
            while (true) {
                String[] fileList = getNewFiles();
                if (fileList != null) {
                    for (String fileName : fileList) {
                        LOGGER.trace("Folder listener \"{}\" handes the file {}", getDirectoryName(), fileName);
                        processItem(fileName);
                    }
                }

                processStatusItems();
                if (removeListener(false)) {
                    LOGGER.debug("Folder listener \"{}\" have no files and will be finished", getDirectoryName());
                    break;
                }
                Thread.sleep(LISTENER_TIMEOUT_MILLLIS);
            }
        } catch (InterruptedException e) {
            removeListener(true);
            LOGGER.debug("Folder listener \"{}\" has been interrupted", getDirectoryName());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            removeListener(true);
            LOGGER.error("Folder listener for \"{}\" closed with error.", getDirectoryName(), e);
            throw new DatalabException("Folder listener for \"" + getDirectoryName() + "\" closed with error. " + e
                    .getLocalizedMessage(), e);
        }
    }

    private void processItem(String fileName) {
        try {
            WatchItem item = itemList.getItem(fileName);
            item.setFileName(fileName);
            if (itemList.processItem(item)) {
                LOGGER.debug("Folder listener \"{}\" processes the file {}", getDirectoryName(),
                        fileName);
            }
        } catch (Exception e) {
            LOGGER.warn("Folder listener \"{}\" has got exception for check or process the file {}",
                    getDirectoryName(), fileName, e);
        }
    }

    @Override
    public void run() {
        if (init()) {
            pollFile();
        } else {
            LOGGER.warn("Folder listener has not been initialized for \"{}\"", getDirectoryName());
            removeListener(true);
        }
    }
}