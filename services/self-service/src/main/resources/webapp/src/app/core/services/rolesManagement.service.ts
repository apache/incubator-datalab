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
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ApplicationServiceFacade } from './';
import { ErrorUtils } from '../util';

@Injectable()
export class RolesGroupsService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getGroupsData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetGroupsData()
      .pipe(
        map(response => response.json()),
        catchError(ErrorUtils.handleServiceError));
  }

  public getRolesData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetRolesData()
      .pipe(
        map(response => response.json()),
        catchError(ErrorUtils.handleServiceError));
  }

  public setupNewGroup(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildSetupNewGroup(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public updateGroup(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildUpdateGroupData(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public setupRolesForGroup(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildSetupRolesForGroup(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public setupUsersForGroup(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildSetupUsersForGroup(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public removeUsersForGroup(data): Observable<{}> {
    const url = `?user=${data.user}&group=${data.group}`;

    return this.applicationServiceFacade
      .buildRemoveUsersForGroup(JSON.stringify(url))
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public removeGroupById(data): Observable<{}> {
    const url = `/${data}`;

    return this.applicationServiceFacade
      .buildRemoveGroupById(JSON.stringify(url))
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
