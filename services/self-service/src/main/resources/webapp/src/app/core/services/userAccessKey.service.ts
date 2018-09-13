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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { ApplicationServiceFacade } from '.';
import { ErrorUtils } from '../util';

@Injectable()
export class UserAccessKeyService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public checkUserAccessKey(): Observable<{}> {
    return this.applicationServiceFacade
      .buildCheckUserAccessKeyRequest()
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public generateAccessKey(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGenerateAccessKey()
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public regenerateAccessKey(): Observable<{}> {
    const param = '?is_primary_uploading=false';
    return this.applicationServiceFacade
      .buildRegenerateAccessKey(param)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public uploadUserAccessKey(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildUploadUserAccessKeyRequest(data)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public reuploadUserAccessKey(data): Observable<{}> {
    const param = '?is_primary_uploading=false';
    return this.applicationServiceFacade
      .buildReuploadUserAccessKeyRequest(data, param)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }
}
