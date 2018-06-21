/*
 * **************************************************************************
 *
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ***************************************************************************
 */

package com.epam.dlab.backendapi.core.response.handlers.dao;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.response.handlers.PersistentFileHandler;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Singleton
@Slf4j
public class FileSystemCallbackHandlerDao implements CallbackHandlerDao {

	@Inject
	private ProvisioningServiceApplicationConfiguration configuration;
	@Inject
	private ObjectMapper mapper;

	@Override
	public void save(PersistentFileHandler handlerCallback) {

		final String fileName = fileName(handlerCallback.getHandler().getId());
		final String absolutePath = getAbsolutePath(fileName);
		saveToFile(handlerCallback, fileName, absolutePath);

	}

	@Override
	public List<PersistentFileHandler> findAll() {
		try (final Stream<Path> pathStream = Files.list(Paths.get(configuration.getHandlerDirectory()))) {
			return pathStream.map(this::toPersistentFileHandler)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(toList());
		} catch (IOException e) {
			log.error("Can not restore handlers due to: {}", e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	@Override
	public void remove(String handlerId) {
		try {
			Files.delete(Paths.get(getAbsolutePath(fileName(handlerId))));
		} catch (Exception e) {
			log.error("Can not remove handler {} due to: {}", handlerId, e.getMessage(), e);
			throw new DlabException("Can not remove handler " + handlerId + " due to: " + e.getMessage());
		}
	}

	private String getAbsolutePath(String fileName) {
		return configuration.getHandlerDirectory() + File.separator + fileName;
	}

	private void saveToFile(PersistentFileHandler handlerCallback, String fileName, String absolutePath) {
		try {
			log.trace("Persisting callback handler to file {}", absolutePath);
			Files.write(Paths.get(absolutePath), mapper.writeValueAsBytes(handlerCallback), StandardOpenOption.CREATE);
		} catch (Exception e) {
			log.warn("Can not persist file handler {} due to {}", fileName, e.getMessage());
		}
	}

	private String fileName(String handlerId) {
		return handlerId + ".json";
	}

	private Optional<PersistentFileHandler> toPersistentFileHandler(Path path) {
		try {
			return Optional.of(mapper.readValue(path.toFile(), PersistentFileHandler.class));
		} catch (Exception e) {
			log.warn("Can not deserialize file handler from file: {}", path.toString());
		}
		return Optional.empty();
	}
}
