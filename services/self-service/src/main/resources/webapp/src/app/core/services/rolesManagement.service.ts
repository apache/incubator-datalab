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

import { ApplicationServiceFacade } from './';
import { ErrorUtils } from '../util';

@Injectable()
export class RolesGroupsService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getGroupsData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetGroupsData()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public getRolesData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetRolesData()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }
  
  public setupNewGroup(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildSetupNewGroup(data)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public setupRolesForGroup(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildSetupRolesForGroup(data)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }
  public setupUsersForGroup(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildSetupUsersForGroup(data)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public removeUsersForGroup(data): Observable<{}> {
    const url = `?user=${data.user}&group=${data.group}`;

    return this.applicationServiceFacade
      .buildRemoveUsersForGroup(JSON.stringify(url))
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }
}