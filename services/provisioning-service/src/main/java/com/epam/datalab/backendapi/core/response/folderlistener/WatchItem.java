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
import com.google.common.base.MoreObjects;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Class to store the file handler for processing.
 */
@Slf4j
public class WatchItem implements Comparable<WatchItem> {

    /**
     * Status of file processing.
     * <pre>
     * WAIT_FOR_FILE waiting for the file creation.
     * TIMEOUT_EXPIRED the timeout expired for the file creation.
     * FILE_CAPTURED the file created and handled.
     * INPROGRESS the file processing is running.
     * IS_DONE the file processing is done. You can check the result of processing {@link WatchItem#getFutureResult()}
     * IS_CANCELED the file processing has been canceled.
     * IS_INTERRUPTED the file processing has been interrupted.
     * IS_FAILED the file processing is failed.
     * </pre>
     */
    public enum ItemStatus {
        WAIT_FOR_FILE,
        TIMEOUT_EXPIRED,
        FILE_CAPTURED,
        INPROGRESS,
        IS_DONE,
        IS_CANCELED,
        IS_INTERRUPTED,
        IS_FAILED
    }

    /**
     * File handler for processing.
     */
    private final FileHandlerCallback fileHandlerCallback;
    /**
     * Timeout waiting for the file creation in milliseconds.
     */
    private final long timeoutMillis;
    /**
     * Timeout waiting for the file writing in milliseconds.
     */
    private final long fileLengthCheckDelay;

    /**
     * Time when expired for the file creation in milliseconds.
     */
    private long expiredTimeMillis;
    /**
     * File name.
     */
    private String fileName;
    /**
     * Future for asynchronously the file processing.
     */
    private CompletableFuture<Boolean> future;
    /**
     * Result of the file processing.
     */
    private Boolean futureResult = null;

    /**
     * Creates instance of the file handler.
     *
     * @param fileHandlerCallback  File handler for processing.
     * @param timeoutMillis        Timeout waiting for the file creation in milliseconds.
     * @param fileLengthCheckDelay Timeout waiting for the file writing in milliseconds.
     */
    public WatchItem(FileHandlerCallback fileHandlerCallback, long timeoutMillis, long fileLengthCheckDelay) {
        this.fileHandlerCallback = fileHandlerCallback;
        this.timeoutMillis = timeoutMillis;
        this.fileLengthCheckDelay = fileLengthCheckDelay;
        setExpiredTimeMillis(timeoutMillis);
    }

    @Override
    public int compareTo(WatchItem o) {
        if (o == null) {
            return -1;
        }
        return (fileHandlerCallback.checkUUID(o.fileHandlerCallback.getUUID()) ?
                0 : fileHandlerCallback.getUUID().compareTo(o.fileHandlerCallback.getUUID()));
    }

    /**
     * Returns the file handler for processing.
     */
    public FileHandlerCallback getFileHandlerCallback() {
        return fileHandlerCallback;
    }

    /**
     * Returns the timeout waiting for the file creation in milliseconds.
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Returns the timeout waiting for the file writing in milliseconds.
     */
    public long getFileLengthCheckDelay() {
        return fileLengthCheckDelay;
    }


    /**
     * Returns the time when expired for the file creation in milliseconds.
     */
    public long getExpiredTimeMillis() {
        return expiredTimeMillis;
    }

    /**
     * Sets time when expired for file creation in milliseconds.
     *
     * @param expiredTimeMillis time expired for file creation in milliseconds.
     */
    private void setExpiredTimeMillis(long expiredTimeMillis) {
        this.expiredTimeMillis = System.currentTimeMillis() + expiredTimeMillis;
    }

    /**
     * Returns the file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the file name.
     *
     * @param fileName file name.
     */
    protected void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the status of the file processing.
     * See {@link ItemStatus} for details.
     */
    public ItemStatus getStatus() {
        if (fileName == null) {
            return (expiredTimeMillis < System.currentTimeMillis() ? ItemStatus.TIMEOUT_EXPIRED : ItemStatus.WAIT_FOR_FILE);
        } else if (future == null) {
            return ItemStatus.FILE_CAPTURED;
        } else if (future.isCancelled()) {
            return ItemStatus.IS_CANCELED;
        }

        if (future.isDone()) {
            try {
                futureResult = future.get();
                return ItemStatus.IS_DONE;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ItemStatus.IS_INTERRUPTED;
            } catch (ExecutionException e) {
                log.error("Execution exception occurred", e);
                return ItemStatus.IS_FAILED;
            }
        }

        return ItemStatus.INPROGRESS;
    }

    /**
     * Returns <b>true</> if the time has expired for the file creation.
     */
    public boolean isExpired() {
        return (fileName == null && expiredTimeMillis < System.currentTimeMillis());
    }


    /**
     * Returns the future for asynchronously the file processing.
     */
    public CompletableFuture<Boolean> getFuture() {
        return future;
    }

    /**
     * Sets the future for the file processing.
     *
     * @param future completable future for file processing.
     */
    protected void setFuture(CompletableFuture<Boolean> future) {
        this.future = future;
    }

    /**
     * Returns the result of the file processing. This method is non-blocking and returns <b>true</b>
     * or <b>false</b> if the file processing has done, otherwise returns <b>null</b>.
     */
    public Boolean getFutureResult() {
        if (futureResult == null && future != null && future.isDone()) {
            try {
                futureResult = future.get();
            } catch (Exception e) {
                log.error("Exception occurred during getting result: {}", e.getMessage(), e);
            }
        }
        return futureResult;
    }

    /**
     * Returns the result of the file processing. This method is blocking and returns <b>true</b> or
     * <b>false</b> when the file processing has done.
     */
    public Boolean getFutureResultSync() throws InterruptedException, ExecutionException {
        if (futureResult == null && future != null) {
            futureResult = future.get();
        }
        return futureResult;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fileHandlerCallback", fileHandlerCallback)
                .add("timeoutMillis", timeoutMillis)
                .add("fileLengthCheckDelay", fileLengthCheckDelay)
                .add("expiredTimeMillis", expiredTimeMillis)
                .add("fileName", fileName)
                .add("future", future)
                .add("futureResult", futureResult)
                .toString();
    }
}
