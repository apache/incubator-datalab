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

package com.epam.dlab.backendapi.domain.contracts;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.aws.keyload.UploadFileResultAws;
import com.epam.dlab.exceptions.DlabException;

/** Interface for upload process the user key to notebook.
 */
public interface IKeyUploader {

	/** Returns the status of user key.
	 * @param userInfo user info.
	 * @exception DlabException When the check the status of user key fails.
	 */
    KeyLoadStatus checkKey(UserInfo userInfo) throws DlabException ;

	/** Starts upload of user key to notebook and returns UUID of request.
	 * @param userInfo user info.
	 * @param content the user key content.
	 * @exception DlabException When the upload of user key fails.
	 */
    String startKeyUpload(UserInfo userInfo, String content) throws DlabException;

	/** Event called when the user key has been uploaded to notebook.
	 * @param result info about an upload operation.
	 */
    void onKeyUploadComplete(UploadFileResultAws result);
}
