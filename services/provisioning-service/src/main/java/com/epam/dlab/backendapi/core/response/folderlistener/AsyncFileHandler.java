/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/


package com.epam.dlab.backendapi.core.response.folderlistener;

import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static com.epam.dlab.backendapi.core.Constants.JSON_EXTENSION;
import static com.epam.dlab.backendapi.core.Constants.LOG_EXTENSION;

public final class AsyncFileHandler implements Supplier<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderListener.class);


    private final String fileName;
    private final String directory;
    private final FileHandlerCallback fileHandlerCallback;
    private final Duration fileLengthCheckDelay;

    public AsyncFileHandler(String fileName, String directory, FileHandlerCallback fileHandlerCallback, Duration fileLengthCheckDelay) {
        this.fileName = fileName;
        this.directory = directory;
        this.fileHandlerCallback = fileHandlerCallback;
        this.fileLengthCheckDelay = fileLengthCheckDelay;
    }

    @Override
    public Boolean get() {
        Path path = Paths.get(directory, fileName);
        try {
            if (fileHandlerCallback.handle(fileName, readBytes(path))) {
                Files.deleteIfExists(path);
                Files.deleteIfExists(getLogFile());
            }
            return true;
        } catch (Exception e) {
            LOGGER.debug("handle file async", e);
        }
        return false;
    }

    private Path getLogFile() {
        return Paths.get(directory, fileName.replaceAll(JSON_EXTENSION, LOG_EXTENSION));
    }

    private byte[] readBytes(Path path) throws IOException, InterruptedException {
        File file = path.toFile();
        waitFileCompliteWrited(file, file.length());
        return Files.readAllBytes(path);
    }

    private void waitFileCompliteWrited(File file, long before) throws InterruptedException {
        Thread.sleep(fileLengthCheckDelay.toMilliseconds());
        long after = file.length();
        if (before != after) {
            waitFileCompliteWrited(file, after);
        }
    }
}
