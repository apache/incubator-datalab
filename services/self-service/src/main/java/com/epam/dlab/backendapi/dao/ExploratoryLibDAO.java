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

import java.util.Optional;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.epam.dlab.dto.exploratory.ExploratoryLibInstallStatusDTO;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.exceptions.DlabException;

/** DAO for user libraries.
 */
public class ExploratoryLibDAO extends BaseDAO {
    private static final String EXPLORATORY_LIBS = "libs";
    private static final String LIB_GROUP = "group";
    public static final String LIB_NAME = "name";
    private static final String LIB_VERSION = "version";
    private static final String LIB_INSTALL_DATE = "install_date";
    private static final String LIB_ERROR_MESSAGE = "error_message";
	private static final Bson LIB_FIELDS = fields(excludeId(), include(EXPLORATORY_LIBS + ".$"));

    private static Bson libraryCondition(String libraryGroup, String libraryName) {
        return elemMatch(EXPLORATORY_LIBS,
        				and(eq(LIB_GROUP, libraryGroup),
        					eq(LIB_NAME, libraryName)));
    }
    
    private static String libraryFieldFilter(String fieldName) {
        return EXPLORATORY_LIBS + FIELD_SET_DELIMETER + fieldName;
    }


    /** Finds and returns the list of user libraries. 
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @return
     */
    public Iterable<Document> findLibraries(String user, String exploratoryName) {
    	return find(USER_INSTANCES,
    			exploratoryCondition(user, exploratoryName),
    			LIB_FIELDS);
    }

    /** Finds and returns the info about library.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @param libraryGroup the group name of library.
     * @param libraryName the name of library.
     * @exception DlabException
     */
    public LibInstallDTO fetchLibraryFields(String user, String exploratoryName, String libraryGroup, String libraryName) throws DlabException {
        Optional<LibInstallDTO> opt = findOne(USER_INSTANCES,
        		and(exploratoryCondition(user, exploratoryName),
        			libraryCondition(libraryGroup, libraryName)),
        			LIB_FIELDS,
        		LibInstallDTO.class);
        if( opt.isPresent() ) {
            return opt.get();
        }
        throw new DlabException("Library " + libraryGroup + "." + libraryName + " not found.");
    }

    /** Finds and returns the status of library.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @param libraryGroup the group name of library.
     * @param libraryName the name of library.
     * @exception DlabException
     */
    public LibStatus fetchLibraryStatus(String user, String exploratoryName, String libraryGroup, String libraryName) throws DlabException {
        return LibStatus.of(
        		findOne(USER_INSTANCES,
        				and(exploratoryCondition(user, exploratoryName),
        					libraryCondition(libraryGroup, libraryName)),
        				fields(include(STATUS), excludeId()))
        			.orElse(new Document())
        			.getOrDefault(STATUS, EMPTY).toString());
    }

    /** Add the user's library for exploratory into database.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     * @param library library.
     * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
     * @exception DlabException
     */
    public boolean addLibrary(String user, String exploratoryName, LibInstallDTO library) throws DlabException {
    	Optional<Document> opt = findOne(USER_INSTANCES,
        								and(exploratoryCondition(user, exploratoryName),
        									libraryCondition(library.getGroup(), library.getName())));
	if (!opt.isPresent()) {
            updateOne(USER_INSTANCES,
            		exploratoryCondition(user, exploratoryName),
                    push(EXPLORATORY_LIBS, convertToBson(library)));
            return true;
        } else {
            return false;
        }
    }

    /** Updates the info about libraries for exploratory in Mongo database.
     * @param dto object of computational resource status.
     * @exception DlabException
     */
    public void updateLibraryFields(ExploratoryLibInstallStatusDTO dto) throws DlabException {
    	for (LibInstallDTO lib : dto.getLibs()) {
	        try {
	            Document values = new Document(libraryFieldFilter(STATUS), lib.getStatus());
	        	if (lib.getVersion() != null) {
	        		values.append(libraryFieldFilter(LIB_VERSION), lib.getVersion());
	        	}
	        	if (dto.getUptime() != null) {
	        		values.append(libraryFieldFilter(LIB_INSTALL_DATE), dto.getUptime());
	        	}
	        	if (lib.getErrorMessage() != null) {
	        		values.append(libraryFieldFilter(LIB_ERROR_MESSAGE), lib.getErrorMessage());
	        	}
	            updateOne(USER_INSTANCES,
	            		and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
	            			libraryCondition(lib.getGroup(), lib.getName())),
	                    new Document(SET, values));
	        } catch (Throwable t) {
	            throw new DlabException("Could not update librariy " + lib.getGroup() + "." + lib.getName() +
	            		" for exploratory " + dto.getExploratoryName(), t);
	        }
    	}
    }
}