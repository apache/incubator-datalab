/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/


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
