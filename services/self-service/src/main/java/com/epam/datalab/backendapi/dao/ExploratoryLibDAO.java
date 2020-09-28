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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.backendapi.util.DateRemoverUtil;
import com.epam.datalab.dto.exploratory.LibInstallDTO;
import com.epam.datalab.dto.exploratory.LibInstallStatusDTO;
import com.epam.datalab.dto.exploratory.LibStatus;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.model.ResourceType;
import com.epam.datalab.model.library.Library;
import com.mongodb.client.model.Projections;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.datalab.backendapi.dao.ExploratoryDAO.COMPUTATIONAL_RESOURCES;
import static com.epam.datalab.backendapi.dao.ExploratoryDAO.exploratoryCondition;
import static com.epam.datalab.backendapi.dao.ExploratoryDAO.runningExploratoryAndComputationalCondition;
import static com.epam.datalab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.push;

/**
 * DAO for user libraries.
 */
public class ExploratoryLibDAO extends BaseDAO {
    public static final String EXPLORATORY_LIBS = "libs";
    public static final String COMPUTATIONAL_LIBS = "computational_libs";
    public static final String LIB_GROUP = "group";
    public static final String LIB_NAME = "name";
    public static final String LIB_VERSION = "version";
    public static final String LIB_AVAILABLE_VERSION = "available_versions";
    public static final String LIB_ADDED_PACKAGES = "add_pkgs";
    private static final String LIB_INSTALL_DATE = "install_date";
    private static final String LIB_ERROR_MESSAGE = "error_message";
    private static final String COMPUTATIONAL_NAME_FIELD = "computational_name";

    /**
     * Return condition for search library into exploratory data.
     *
     * @param libraryGroup the name of group.
     * @param libraryName  the name of library.
     */
    private static Bson libraryConditionExploratory(String libraryGroup, String libraryName) {
        return elemMatch(EXPLORATORY_LIBS,
                libCondition(libraryGroup, libraryName));
    }


    /**
     * Return condition for search library into computational data.
     *
     * @param computationalName computational name
     * @param libraryGroup      the name of group.
     * @param libraryName       the name of library.
     */
    private static Bson libraryConditionComputational(String computationalName, String libraryGroup,
                                                      String libraryName) {
        return elemMatch(COMPUTATIONAL_LIBS + "." + computationalName,
                and(eq(LIB_GROUP, libraryGroup), eq(LIB_NAME, libraryName)));
    }

    /**
     * Return field filter for libraries properties in exploratory data.
     *
     * @param fieldName
     * @return
     */
    private static String libraryFieldFilter(String fieldName) {
        return EXPLORATORY_LIBS + FIELD_SET_DELIMETER + fieldName;
    }


    private static String computationalLibraryFieldFilter(String computational, String fieldName) {
        return COMPUTATIONAL_LIBS + "." + computational + FIELD_SET_DELIMETER + fieldName;
    }

    private Document findLibraries(String user, String project, String exploratoryName, Bson include) {
        Optional<Document> opt = findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName, project),
                fields(excludeId(), include));

        return opt.orElseGet(Document::new);

    }

    public List<Library> getLibraries(String user, String project, String exploratoryName) {
        final Document libsDocument = findAllLibraries(user, project, exploratoryName);
        return Stream
                .concat(
                        libraryStream(libsDocument, exploratoryName, EXPLORATORY_LIBS, ResourceType.EXPLORATORY),
                        computationalLibStream(libsDocument))
                .collect(Collectors.toList());
    }

    public Document findAllLibraries(String user, String project, String exploratoryName) {
        return findLibraries(user, project, exploratoryName, include(EXPLORATORY_LIBS, COMPUTATIONAL_LIBS,
                COMPUTATIONAL_RESOURCES));
    }

    public Document findExploratoryLibraries(String user, String project, String exploratoryName) {
        return findLibraries(user, project, exploratoryName, include(EXPLORATORY_LIBS));
    }

    public Document findComputationalLibraries(String user, String project, String exploratoryName, String computationalName) {
        return findLibraries(user, project, exploratoryName, include(COMPUTATIONAL_LIBS + "." + computationalName));
    }

    @SuppressWarnings("unchecked")
    public Library getLibrary(String user, String project, String exploratoryName, String libraryGroup, String libraryName) {
        Optional<Document> userInstance = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName, project),
                        elemMatch(EXPLORATORY_LIBS,
                                and(eq(LIB_GROUP, libraryGroup), eq(LIB_NAME, libraryName))
                        )),
                Projections.fields(excludeId(), Projections.include(EXPLORATORY_LIBS)));

        if (userInstance.isPresent()) {
            final Object exloratoryLibs = userInstance.get().get(EXPLORATORY_LIBS);
            List<Document> libs = exloratoryLibs != null ? (List<Document>) exloratoryLibs : Collections.emptyList();
            return libs.stream()
                    .filter(libraryPredicate(libraryGroup, libraryName))
                    .map(d -> convertFromDocument(d, Library.class))
                    .findAny().orElse(null);

        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public Library getLibrary(String user, String project, String exploratoryName, String computationalName,
                              String libraryGroup, String libraryName) {
        Optional<Document> libraryStatus = findOne(USER_INSTANCES,
                and(runningExploratoryAndComputationalCondition(user, project, exploratoryName, computationalName),
                        libraryConditionComputational(computationalName, libraryGroup, libraryName)
                ),

                Projections.fields(excludeId(),
                        Projections.include(
                                COMPUTATIONAL_LIBS + "." + computationalName + "." + STATUS,
                                COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_GROUP,
                                COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_NAME)
                )
        );

        return libraryStatus.map(document -> ((List<Document>) (((Document) document.get(COMPUTATIONAL_LIBS)).get(computationalName)))
                .stream()
                .filter(libraryPredicate(libraryGroup, libraryName))
                .map(l -> convertFromDocument(l, Library.class))
                .findAny().orElse(null)).orElse(null);
    }

    private Predicate<Document> libraryPredicate(String libraryGroup, String libraryName) {
        return l -> libraryGroup.equals(l.getString(LIB_GROUP))
                && libraryName.equals(l.getString(LIB_NAME));
    }

    /**
     * Add the user's library for exploratory into database.
     *
     * @param user            user name.
     * @param project         project name
     * @param exploratoryName name of exploratory.
     * @param library         library.
     * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
     */
    public boolean addLibrary(String user, String project, String exploratoryName, LibInstallDTO library, boolean reinstall) {
        Optional<Document> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName, project),
                        elemMatch(EXPLORATORY_LIBS,
                                and(eq(LIB_GROUP, library.getGroup()), eq(LIB_NAME, library.getName())))));
        if (!opt.isPresent()) {
            updateOne(USER_INSTANCES,
                    exploratoryCondition(user, exploratoryName, project),
                    push(EXPLORATORY_LIBS, convertToBson(library)));
            return true;
        } else {
            Document values = addLibraryFields(library);
            if (reinstall) {
                values.append(libraryFieldFilter(LIB_INSTALL_DATE), null);
                values.append(libraryFieldFilter(LIB_ERROR_MESSAGE), null);
            }

            updateOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName, project),
                    elemMatch(EXPLORATORY_LIBS,
                            and(eq(LIB_GROUP, library.getGroup()), eq(LIB_NAME, library.getName())))),
                    new Document(SET, values));
            return false;
        }
    }

    /**
     * Add the user's library for exploratory into database.
     *
     * @param user              user name.
     * @param project           project name
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational.
     * @param library           library.
     * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
     */
    public boolean addLibrary(String user, String project, String exploratoryName, String computationalName,
                              LibInstallDTO library, boolean reinstall) {

        Optional<Document> opt = findOne(USER_INSTANCES,
                and(runningExploratoryAndComputationalCondition(user, project, exploratoryName, computationalName),
                        eq(COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_GROUP, library.getGroup()),
                        eq(COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_NAME, library.getName())));

        if (!opt.isPresent()) {
            updateOne(USER_INSTANCES,
                    runningExploratoryAndComputationalCondition(user, project, exploratoryName, computationalName),
                    push(COMPUTATIONAL_LIBS + "." + computationalName, convertToBson(library)));
            return true;
        } else {
            Document values = addComputationalLibraryFields(computationalName, library);
            if (reinstall) {
                values.append(computationalLibraryFieldFilter(computationalName, LIB_INSTALL_DATE), null);
                values.append(computationalLibraryFieldFilter(computationalName, LIB_ERROR_MESSAGE), null);
            }

            updateOne(USER_INSTANCES, and(
                    exploratoryCondition(user, exploratoryName, project),
                    eq(COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_GROUP, library.getGroup()),
                    eq(COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_NAME, library.getName())),

                    new Document(SET, values));

            return false;
        }
    }

    /**
     * Updates the info about libraries for exploratory/computational in Mongo database.
     *
     * @param dto object of computational resource status.
     */
    public void updateLibraryFields(LibInstallStatusDTO dto) {
        if (dto.getLibs() == null) {
            return;
        }

        if (StringUtils.isEmpty(dto.getComputationalName())) {
            updateExploratoryLibraryFields(dto);
        } else {
            updateComputationalLibraryFields(dto);
        }
    }

    private void updateExploratoryLibraryFields(LibInstallStatusDTO dto) {
        for (LibInstallDTO lib : dto.getLibs()) {
            try {
                Document values = updateLibraryFields(lib, dto.getUptime());

                updateOne(USER_INSTANCES,
                        and(exploratoryCondition(dto.getUser(), dto.getExploratoryName(), dto.getProject()),
                                libraryConditionExploratory(lib.getGroup(), lib.getName())),
                        new Document(SET, values));
            } catch (Exception e) {
                throw new DatalabException(String.format("Could not update library %s for %s",
                        lib, dto.getExploratoryName()), e);
            }
        }
    }

    private void updateComputationalLibraryFields(LibInstallStatusDTO dto) {
        for (LibInstallDTO lib : dto.getLibs()) {
            try {
                Document values = updateComputationalLibraryFields(dto.getComputationalName(), lib, dto.getUptime());

                updateOne(USER_INSTANCES,
                        and(exploratoryCondition(dto.getUser(), dto.getExploratoryName(), dto.getProject()),
                                elemMatch(COMPUTATIONAL_LIBS + "." + dto.getComputationalName(),
                                        libCondition(lib.getGroup(), lib.getName()))),
                        new Document(SET, values));
            } catch (Exception e) {
                throw new DatalabException(String.format("Could not update library %s for %s/%s",
                        lib, dto.getExploratoryName(), dto.getComputationalName()), e);
            }
        }
    }

    private static Bson libCondition(String group, String name) {
        return and(eq(LIB_GROUP, group), eq(LIB_NAME, name));
    }

    private Document addLibraryFields(LibInstallDTO lib) {
        Document values = new Document(libraryFieldFilter(STATUS), lib.getStatus());
        if (lib.getVersion() != null) {
            values.append(libraryFieldFilter(LIB_VERSION), lib.getVersion());
        }

        return values;
    }

    private Document updateLibraryFields(LibInstallDTO lib, Date uptime) {
        Document values = new Document(libraryFieldFilter(STATUS), lib.getStatus());
        if (lib.getVersion() != null) {
            values.append(libraryFieldFilter(LIB_VERSION), lib.getVersion());
        }
        if (uptime != null) {
            values.append(libraryFieldFilter(LIB_INSTALL_DATE), uptime);
        }
        if (lib.getAvailableVersions() != null) {
            values.append(libraryFieldFilter(LIB_AVAILABLE_VERSION), lib.getAvailableVersions());
        }
        if (lib.getAddedPackages() != null) {
            values.append(libraryFieldFilter(LIB_ADDED_PACKAGES), lib.getAddedPackages());
        }
        if (lib.getErrorMessage() != null) {
            values.append(libraryFieldFilter(LIB_ERROR_MESSAGE),
                    DateRemoverUtil.removeDateFormErrorMessage(lib.getErrorMessage()));
        }

        return values;
    }

    private Document addComputationalLibraryFields(String computational, LibInstallDTO lib) {
        Document values = new Document(computationalLibraryFieldFilter(computational, STATUS), lib.getStatus());
        if (lib.getVersion() != null) {
            values.append(computationalLibraryFieldFilter(computational, LIB_VERSION), lib.getVersion());
        }

        return values;
    }

    private Document updateComputationalLibraryFields(String computational, LibInstallDTO lib, Date uptime) {
        Document values = new Document(computationalLibraryFieldFilter(computational, STATUS), lib.getStatus());
        if (lib.getVersion() != null) {
            values.append(computationalLibraryFieldFilter(computational, LIB_VERSION), lib.getVersion());
        }
        if (uptime != null) {
            values.append(computationalLibraryFieldFilter(computational, LIB_INSTALL_DATE), uptime);
        }
        if (lib.getAvailableVersions() != null) {
            values.append(computationalLibraryFieldFilter(computational, LIB_AVAILABLE_VERSION), lib.getAvailableVersions());
        }
        if (lib.getAddedPackages() != null) {
            values.append(computationalLibraryFieldFilter(computational, LIB_ADDED_PACKAGES), lib.getAddedPackages());
        }
        if (lib.getErrorMessage() != null) {
            values.append(computationalLibraryFieldFilter(computational, LIB_ERROR_MESSAGE),
                    DateRemoverUtil.removeDateFormErrorMessage(lib.getErrorMessage()));
        }

        return values;
    }

    @SuppressWarnings("unchecked")
    private Stream<Library> computationalLibStream(Document libsDocument) {
        return ((List<Document>) libsDocument.getOrDefault(COMPUTATIONAL_RESOURCES, Collections.emptyList()))
                .stream()
                .map(d -> d.getString(COMPUTATIONAL_NAME_FIELD))
                .flatMap(compName -> libraryStream(
                        (Document) libsDocument.getOrDefault(COMPUTATIONAL_LIBS, new Document()),
                        compName,
                        compName, ResourceType.COMPUTATIONAL));
    }

    @SuppressWarnings("unchecked")
    private Stream<Library> libraryStream(Document libsDocument, String resourceName, String libFieldName,
                                          ResourceType libType) {
        return ((List<Document>) libsDocument.getOrDefault(libFieldName, Collections.emptyList()))
                .stream()
                .map(d -> convertFromDocument(d, Library.class))
                .filter(library -> !Arrays.asList(LibStatus.INVALID_VERSION, LibStatus.INVALID_NAME).contains(library.getStatus()))
                .peek(l -> l.withType(libType).withResourceName(resourceName));
    }
}