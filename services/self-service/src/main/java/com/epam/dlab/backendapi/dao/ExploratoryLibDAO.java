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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.util.DateRemoverUtil;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibInstallStatusDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.library.Library;
import com.mongodb.client.model.Projections;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.*;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.push;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * DAO for user libraries.
 */
public class ExploratoryLibDAO extends BaseDAO {
	public static final String EXPLORATORY_LIBS = "libs";
	public static final String COMPUTATIONAL_LIBS = "computational_libs";
	public static final String LIB_GROUP = "group";
	public static final String LIB_NAME = "name";
	public static final String LIB_VERSION = "version";
	private static final String LIB_INSTALL_DATE = "install_date";
	private static final String LIB_ERROR_MESSAGE = "error_message";
	private static final String COMPUTATIONAL_NAME_FIELD = "computational_name";

	/**
	 * Return condition for search library into exploratory data.
	 *
	 * @param libraryGroup the name of group.
	 * @param libraryName  the name of library.
	 */
	private static Bson libraryCondition(String libraryGroup, String libraryName) {
		return elemMatch(EXPLORATORY_LIBS,
				and(eq(LIB_GROUP, libraryGroup),
						eq(LIB_NAME, libraryName)));
	}

	/**
	 * Return condition for search library into exploratory data.
	 *
	 * @param libraryGroup   the name of group.
	 * @param libraryName    the name of library.
	 * @param libraryVersion the name of library.
	 */
	private static Bson libraryCondition(String libraryGroup, String libraryName, String libraryVersion) {
		return elemMatch(EXPLORATORY_LIBS,
				and(eq(LIB_GROUP, libraryGroup), eq(LIB_NAME, libraryName), eq(LIB_VERSION, libraryVersion)));
	}


	/**
	 * Return condition for search library into computational data.
	 *
	 * @param computationalName computational name
	 * @param libraryGroup      the name of group.
	 * @param libraryName       the name of library.
	 * @param libraryVersion    the name of library.
	 */
	private static Bson libraryConditionComputational(String computationalName, String libraryGroup,
													  String libraryName, String libraryVersion) {
		return elemMatch(COMPUTATIONAL_LIBS + "." + computationalName,
				and(eq(LIB_GROUP, libraryGroup), eq(LIB_NAME, libraryName), eq(LIB_VERSION, libraryVersion)));
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

	private Document findLibraries(String user, String exploratoryName, Bson include) {
		Optional<Document> opt = findOne(USER_INSTANCES,
				exploratoryCondition(user, exploratoryName),
				fields(excludeId(), include));

		return opt.orElseGet(Document::new);

	}

	public List<Library> getLibraries(String user, String exploratoryName) {
		final Document libsDocument = findAllLibraries(user, exploratoryName);
		return Stream
				.concat(
						libraryStream(libsDocument, exploratoryName, EXPLORATORY_LIBS, ResourceType.EXPLORATORY),
						computationalLibStream(libsDocument))
				.collect(Collectors.toList());
	}

	public Document findAllLibraries(String user, String exploratoryName) {
		return findLibraries(user, exploratoryName, include(EXPLORATORY_LIBS, COMPUTATIONAL_LIBS,
				COMPUTATIONAL_RESOURCES));
	}

	public Document findExploratoryLibraries(String user, String exploratoryName) {
		return findLibraries(user, exploratoryName, include(EXPLORATORY_LIBS));
	}

	public Document findComputationalLibraries(String user, String exploratoryName, String computationalName) {
		return findLibraries(user, exploratoryName, include(COMPUTATIONAL_LIBS + "." + computationalName));
	}

	/**
	 * Finds and returns the status of library.
	 *
	 * @param user            user name.
	 * @param exploratoryName the name of exploratory.
	 * @param libraryGroup    the group name of library.
	 * @param libraryName     the name of library.
	 */
	public LibStatus fetchLibraryStatus(String user, String exploratoryName,
										String libraryGroup, String libraryName, String version) {
		Optional<Document> libraryStatus = findOne(USER_INSTANCES,
				and(exploratoryCondition(user, exploratoryName), libraryCondition(libraryGroup, libraryName, version)),
				Projections.fields(excludeId(), Projections.include("libs.status")));

		if (libraryStatus.isPresent()) {
			Object lib = libraryStatus.get().get(EXPLORATORY_LIBS);
			if (lib != null && lib instanceof List && !((List) lib).isEmpty()) {
				return LibStatus.of(((List<Document>) lib).get(0).getOrDefault(STATUS, EMPTY).toString());
			}
		}

		return LibStatus.of(EMPTY);
	}

	/**
	 * Finds and returns the status of library.
	 *
	 * @param user              user name.
	 * @param exploratoryName   the name of exploratory.
	 * @param computationalName the name of computational.
	 * @param libraryGroup      the group name of library.
	 * @param libraryName       the name of library.
	 */
	public LibStatus fetchLibraryStatus(String user, String exploratoryName, String computationalName,
										String libraryGroup, String libraryName, String version) {
		Optional<Document> libraryStatus = findOne(USER_INSTANCES,
				and(runningExploratoryAndComputationalCondition(user, exploratoryName, computationalName),
						libraryConditionComputational(computationalName, libraryGroup, libraryName, version)
				),

				Projections.fields(excludeId(),
						Projections.include(
								COMPUTATIONAL_LIBS + "." + computationalName + "." + STATUS,
								COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_GROUP,
								COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_NAME)
				)
		);

		if (libraryStatus.isPresent()) {
			Object lib = ((Document) libraryStatus.get().get(COMPUTATIONAL_LIBS)).get(computationalName);
			if (lib != null && lib instanceof List && !((List) lib).isEmpty()) {
				return LibStatus.of(((List<Document>) lib).stream()
						.filter(e -> libraryGroup.equals(e.getString(LIB_GROUP))
								&& libraryName.equals(e.getString(LIB_NAME))).findFirst()
						.orElseGet(Document::new).getOrDefault(STATUS, EMPTY).toString());
			}
		}

		return LibStatus.of(EMPTY);
	}

	/**
	 * Add the user's library for exploratory into database.
	 *
	 * @param user            user name.
	 * @param exploratoryName name of exploratory.
	 * @param library         library.
	 * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
	 */
	public boolean addLibrary(String user, String exploratoryName, LibInstallDTO library, boolean reinstall) {
		Optional<Document> opt = findOne(USER_INSTANCES,
				and(exploratoryCondition(user, exploratoryName),
						libraryCondition(library.getGroup(), library.getName())));
		if (!opt.isPresent()) {
			updateOne(USER_INSTANCES,
					exploratoryCondition(user, exploratoryName),
					push(EXPLORATORY_LIBS, convertToBson(library)));
			return true;
		} else {
			Document values = updateLibraryFields(library, null);
			if (reinstall) {
				values.append(libraryFieldFilter(LIB_INSTALL_DATE), null);
				values.append(libraryFieldFilter(LIB_ERROR_MESSAGE), null);
			}

			updateOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName),
					libraryCondition(library.getGroup(), library.getName())), new Document(SET, values));

			return false;
		}
	}

	/**
	 * Add the user's library for exploratory into database.
	 *
	 * @param user              user name.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational.
	 * @param library           library.
	 * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
	 */
	public boolean addLibrary(String user, String exploratoryName, String computationalName,
							  LibInstallDTO library, boolean reinstall) {

		Optional<Document> opt = findOne(USER_INSTANCES,
				and(runningExploratoryAndComputationalCondition(user, exploratoryName, computationalName),
						eq(COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_GROUP, library.getGroup()),
						eq(COMPUTATIONAL_LIBS + "." + computationalName + "." + LIB_NAME, library.getName())));

		if (!opt.isPresent()) {
			updateOne(USER_INSTANCES,
					runningExploratoryAndComputationalCondition(user, exploratoryName, computationalName),
					push(COMPUTATIONAL_LIBS + "." + computationalName, convertToBson(library)));
			return true;
		} else {
			Document values = updateComputationalLibraryFields(computationalName, library, null);
			if (reinstall) {
				values.append(computationalLibraryFieldFilter(computationalName, LIB_INSTALL_DATE), null);
				values.append(computationalLibraryFieldFilter(computationalName, LIB_ERROR_MESSAGE), null);
			}

			updateOne(USER_INSTANCES, and(
					exploratoryCondition(user, exploratoryName),
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
						and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
								libraryCondition(lib.getGroup(), lib.getName())),
						new Document(SET, values));
			} catch (Exception e) {
				throw new DlabException(String.format("Could not update library %s for %s",
						lib, dto.getExploratoryName()), e);
			}
		}
	}

	private void updateComputationalLibraryFields(LibInstallStatusDTO dto) {
		for (LibInstallDTO lib : dto.getLibs()) {
			try {
				Document values = updateComputationalLibraryFields(dto.getComputationalName(), lib, dto.getUptime());

				updateOne(USER_INSTANCES,
						and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
								elemMatch(COMPUTATIONAL_LIBS + "." + dto.getComputationalName(),
										and(eq(LIB_GROUP, lib.getGroup()), eq(LIB_NAME, lib.getName())))),
						new Document(SET, values));
			} catch (Exception e) {
				throw new DlabException(String.format("Could not update library %s for %s/%s",
						lib, dto.getExploratoryName(), dto.getComputationalName()), e);
			}
		}
	}

	private Document updateLibraryFields(LibInstallDTO lib, Date uptime) {
		Document values = new Document(libraryFieldFilter(STATUS), lib.getStatus());
		if (lib.getVersion() != null) {
			values.append(libraryFieldFilter(LIB_VERSION), lib.getVersion());
		}
		if (uptime != null) {
			values.append(libraryFieldFilter(LIB_INSTALL_DATE), uptime);
		}

		if (lib.getErrorMessage() != null) {
			values.append(libraryFieldFilter(LIB_ERROR_MESSAGE),
					DateRemoverUtil.removeDateFormErrorMessage(lib.getErrorMessage()));
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
				.peek(l -> l.withType(libType).withResourceName(resourceName));
	}
}