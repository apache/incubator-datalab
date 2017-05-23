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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.UserComputationalResourceDTO;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.util.DateRemoverUtil;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;
import static org.apache.commons.lang3.StringUtils.EMPTY;


import static com.epam.dlab.backendapi.dao.ExploratoryDAO.exploratoryCondition;
import static com.epam.dlab.UserInstanceStatus.TERMINATED;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.COMPUTATIONAL_RESOURCES;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_NAME;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.UPTIME;

/** DAO for user libraries.
 */
public class ExploratoryLibDAO extends BaseDAO {
    private static final String EXPLORATORY_LIBS = "libs";
    private static final String LIB_GROUP = "group";
    public static final String LIB_NAME = "name";
    private static final String LIB_VERSION = "version";
    private static final String LIB_INSTALL_DATE = "install_date";
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
    public UserInstanceDTO fetchLibraryFields(String user, String exploratoryName, String libraryGroup, String libraryName) throws DlabException {
        Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
        		exploratoryCondition(user, exploratoryName),
        		and(libraryCondition(libraryGroup, libraryName),
        			LIB_FIELDS),
                UserInstanceDTO.class);
        if( opt.isPresent() ) {
            return opt.get();
        }
        throw new DlabException("Library " + libraryGroup + "." + libraryName + " not found.");
    }

    /** Add the user's library for notebook into database.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     * @param library library.
     * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
     * @exception DlabException
     */
    public boolean addLibrary(String user, String exploratoryName, UserInstanceDTO library) throws DlabException {
        Optional<Document> opt = findOne(USER_INSTANCES,
        								exploratoryCondition(user, exploratoryName),
        								libraryCondition(libraryGroup, libraryName));
        if (!opt.isPresent()) {
            updateOne(USER_INSTANCES,
            		exploratoryCondition(user, exploratoryName),
                    push(EXPLORATORY_LIBS, convertToBson(library)));
            return true;
        } else {
            return false;
        }
    }

    /** Updates the info of computational resource in Mongo database.
     * @param dto object of computational resource status.
     * @return The result of an update operation.
     * @exception DlabException
     */
    public UpdateResult updateLibraryFields(ComputationalStatusDTO dto) throws DlabException {
        try {
            Document values = new Document(libraryFieldFilter(STATUS), dto.getStatus());
        	if (dto.getUptime() != null) {
        		values.append(libraryFieldFilter(LIB_VERSION), dto.getUptime());
        	}
        	if (dto.getInstanceId() != null) {
        		values.append(libraryFieldFilter(LIB_INSTALL_DATE), dto.getInstanceId());
        	}
            return updateOne(USER_INSTANCES,
            				and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
            					libraryCondition(libraryGroup, libraryName)),
                    new Document(SET, values));
        } catch (Throwable t) {
            throw new DlabException("Could not update librariy " + libraryGroup + "." + libraryName + " for exploratory " + exploratoryName, t);
        }
    }
}