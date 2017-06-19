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

import org.bson.Document;

import com.epam.dlab.backendapi.resources.dto.ExploratoryGitCredsFormDTO;
import com.epam.dlab.exceptions.DlabException;

/** DAO for user exploratory.
 */
public class GitCredsDAO extends BaseDAO {

    /** Find and return the list of GIT credentials for user. 
     * @param user name
     * @return
     */
    public Iterable<Document> findGitCreds(String user) {
        return find(GIT_CREDS, eq(ID, user));
    }

    /** Update the GIT credentials for user.
     * @param user name
     * @param dto GIT credentials
     * @exception DlabException
     */
    public void updateGitCreds(String user, ExploratoryGitCredsFormDTO dto) throws DlabException {
    	Document d = new Document(SET,
							convertToBson(dto)
								.append(ID, user));
    	updateOne(GIT_CREDS,
    				eq(ID, user),
    				d,
    				true);
    }
}