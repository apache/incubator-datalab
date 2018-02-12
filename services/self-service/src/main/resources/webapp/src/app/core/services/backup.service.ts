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
import { Response } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import { ApplicationServiceFacade } from './';

@Injectable()
export class BackupService {
  inProgress: boolean;

  constructor(private applicationServiceFacade: ApplicationServiceFacade) {}

  set creatingBackup(data) {
    this.inProgress = data.status !== 'CREATED';
  }

  public createBackup(data): Observable<Response> {
    return this.applicationServiceFacade
      .buildCreateBackupRequest(data)
      .map((response: Response) => response);
  }

  public getBackupStatus(uuid): Observable<Response> {
    const body = `/${uuid}`;
    return this.applicationServiceFacade
      .buildGetBackupStatusRequest(body)
      .map((response: Response) => response.json())
      .map(data => (this.creatingBackup = data));
  }
}
