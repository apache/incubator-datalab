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

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.exploratoryCondition;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.push;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.*;

import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.epam.dlab.backendapi.util.DateRemoverUtil;
import com.epam.dlab.dto.exploratory.ExploratoryLibInstallStatusDTO;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.exceptions.DlabException;

/** DAO for user libraries.
 */
public class ExploratoryLibDAO extends BaseDAO {
    private static final String EXPLORATORY_LIBS = "libs";
    private static final String COMPUTATIONAL_LIBS = "computational_libs";
    private static final String LIB_GROUP = "group";
    private static final String LIB_NAME = "name";
    private static final String LIB_VERSION = "version";
    private static final String LIB_INSTALL_DATE = "install_date";
    private static final String LIB_ERROR_MESSAGE = "error_message";

	/** Return condition for search library into exploratory data.
	 * @param libraryGroup the name of group.
	 * @param libraryName the name of library.
	 */
    private static Bson libraryCondition(String libraryGroup, String libraryName) {
        return elemMatch(EXPLORATORY_LIBS,
        				and(eq(LIB_GROUP, libraryGroup),
        					eq(LIB_NAME, libraryName)));
    }

    /** Return condition for search library into exploratory data.
     * @param libraryGroup the name of group.
     * @param libraryName the name of library.
     * @param libraryVersion the name of library.
     */
    private static Bson libraryCondition(String libraryGroup, String libraryName, String libraryVersion) {
        return elemMatch(EXPLORATORY_LIBS,
                and(eq(LIB_GROUP, libraryGroup), eq(LIB_NAME, libraryName), eq(LIB_VERSION, libraryVersion)));
    }

    /** Return field filter for libraries properties in exploratory data.
     * @param fieldName
     * @return
     */
    private static String libraryFieldFilter(String fieldName) {
        return EXPLORATORY_LIBS + FIELD_SET_DELIMETER + fieldName;
    }


    /** Finds and returns the list of user libraries.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     */
    @SuppressWarnings("unchecked")
	public Document findLibraries(String user, String exploratoryName) {
    	return findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName),
                fields(include(EXPLORATORY_LIBS, COMPUTATIONAL_LIBS))).orElse(new Document());
    }

    /** Finds and returns the status of library.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @param libraryGroup the group name of library.
     * @param libraryName the name of library.
     */
    public LibStatus fetchLibraryStatus(String user, String exploratoryName,
                                        String libraryGroup, String libraryName, String version) {
    	Optional<Document> libraryStatus = findOne(USER_INSTANCES,
				and(exploratoryCondition(user, exploratoryName), libraryCondition(libraryGroup, libraryName, version)),
				Projections.fields(excludeId(), Projections.include("libs.status")));

    	if (libraryStatus.isPresent()) {
    	    Object lib = libraryStatus.get().get(EXPLORATORY_LIBS);
            if (lib != null && lib instanceof List && !((List) lib).isEmpty()) {
                return  LibStatus.of(((List<Document>)lib).get(0).getOrDefault(STATUS, EMPTY).toString());
            }
        }

    	return LibStatus.of(EMPTY);
    }

    /** Add the user's library for exploratory into database.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     * @param library library.
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

    /** Updates the info about libraries for exploratory in Mongo database.
     * @param dto object of computational resource status.
     * @exception DlabException
     */
    public void updateLibraryFields(ExploratoryLibInstallStatusDTO dto) throws DlabException {
    	if (dto.getLibs() == null) {
    		return;
    	}
    	for (LibInstallDTO lib : dto.getLibs()) {
	        try {
	            Document values = updateLibraryFields(lib, dto.getUptime());

	            updateOne(USER_INSTANCES,
	            		and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
	            			libraryCondition(lib.getGroup(), lib.getName())),
	                    new Document(SET, values));
	        } catch (Exception t) {
	            throw new DlabException("Could not update librariy " + lib.getGroup() + "." + lib.getName() +
	            		" for exploratory " + dto.getExploratoryName(), t);
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
}