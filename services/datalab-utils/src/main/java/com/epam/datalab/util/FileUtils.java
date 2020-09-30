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

package com.epam.datalab.util;

import com.epam.datalab.exceptions.DatalabException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class FileUtils {

    private FileUtils() {
    }

    public static void saveToFile(String filename, String directory, String content) throws IOException {
        java.nio.file.Path filePath = Paths.get(directory, filename).toAbsolutePath();
        log.debug("Saving content to {}", filePath.toString());
        try {
            com.google.common.io.Files.createParentDirs(new File(filePath.toString()));
        } catch (IOException e) {
            throw new DatalabException("Can't create folder " + filePath + ": " + e.getLocalizedMessage(), e);
        }
        Files.write(filePath, content.getBytes());
    }

    public static void deleteFile(String filename, String directory) throws IOException {
        java.nio.file.Path filePath = Paths.get(directory, filename).toAbsolutePath();
        log.debug("Deleting file from {}", filePath.toString());
        Files.deleteIfExists(filePath);
    }

    public static void deleteFile(String absolutePath) {
        log.debug("Deleting file from {}", absolutePath);
        try {
            Files.deleteIfExists(Paths.get(absolutePath));
        } catch (IOException e) {
            log.error("Problems occured with deleting file {} due to: {}", absolutePath, e.getLocalizedMessage(), e);
        }
    }
}
