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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import java.util.Optional;

import org.bson.Document;

import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.exceptions.DlabException;

/** DAO for user exploratory.
 */
public class GitCredsDAO extends BaseDAO {
	private static final String FIELD_GIT_CREDS = "git_creds";

    /** Find and return the list of GIT credentials for user. 
     * @param user name
     * @return
     */
    public ExploratoryGitCredsDTO findGitCreds(String user) {
    	Optional<ExploratoryGitCredsDTO> opt = findOne(GIT_CREDS,
    													eq(ID, user),
    													fields(include(FIELD_GIT_CREDS), excludeId()),
    													ExploratoryGitCredsDTO.class);
    	return (opt.isPresent() ? opt.get() : new ExploratoryGitCredsDTO());
    }

    /** Update the GIT credentials for user.
     * @param user name
     * @param dto GIT credentials
     * @exception DlabException
     */
    public void updateGitCreds(String user, ExploratoryGitCredsDTO dto) throws DlabException {
    	Document d = new Document(SET,
							convertToBson(dto)
								.append(ID, user));
    	updateOne(GIT_CREDS,
    				eq(ID, user),
    				d,
    				true);
    }
}