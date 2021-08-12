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
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static com.epam.datalab.backendapi.core.Constants.JSON_EXTENSION;
import static com.epam.datalab.backendapi.core.Constants.LOG_EXTENSION;

/* Handler for the file processing.
 */
public final class AsyncFileHandler implements Supplier<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncFileHandler.class);

    /**
     * File name.
     */
    private final String fileName;
    /**
     * Directory name.
     */
    private final String directory;
    /**
     * Implement of the file handler.
     */
    private final FileHandlerCallback fileHandlerCallback;
    /**
     * Timeout waiting for the file writing.
     */
    private final Duration fileLengthCheckDelay;

    /**
     * Create instance of the file handler.
     *
     * @param fileName             file name.
     * @param directory            directory name.
     * @param fileHandlerCallback  file handler for processing
     * @param fileLengthCheckDelay timeout waiting for the file writing.
     */
    public AsyncFileHandler(String fileName, String directory, FileHandlerCallback fileHandlerCallback,
                            Duration fileLengthCheckDelay) {
        this.fileName = fileName;
        this.directory = directory;
        this.fileHandlerCallback = fileHandlerCallback;
        this.fileLengthCheckDelay = fileLengthCheckDelay;
    }

    @Override
    public Boolean get() {
        Path path = Paths.get(directory, fileName);
        try {
            boolean result = fileHandlerCallback.handle(fileName, readBytes(path));
            if (result) {
                try {
                    Files.deleteIfExists(path);
                    Files.deleteIfExists(getLogFile());
                    LOGGER.trace("Response {} and log files has been deleted", path.toAbsolutePath());
                } catch (IOException e) {
                    LOGGER.warn("Can't delete file {}", path.toAbsolutePath(), e);
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("Could not handle file {} async", path.toAbsolutePath(), e);
            fileHandlerCallback.handleError(e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * Returns the name of log file.
     */
    private Path getLogFile() {
        return Paths.get(directory, fileName.replaceAll(JSON_EXTENSION, LOG_EXTENSION));
    }

    /**
     * Returns the content of file.
     *
     * @param path source file.
     * @return File content.
     * @throws IOException
     * @throws InterruptedException
     */
    private byte[] readBytes(Path path) throws IOException, InterruptedException {
        File file = path.toFile();
        waitFileCompletelyWritten(file);
        return Files.readAllBytes(path);
    }

    /**
     * Waiting for the file writing. This method is blocking and return control when
     * the file will no longer resize.
     *
     * @param file source file.
     */
    private void waitFileCompletelyWritten(File file) throws InterruptedException {
        long before;
        long after = file.length();
        do {
            before = after;
            Thread.sleep(fileLengthCheckDelay.toMilliseconds());
            after = file.length();
        } while (before != after);
    }
}
