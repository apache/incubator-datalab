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

import { GeneralEnvironmentStatus } from '../../health-status/environment-status.model';
import { ApplicationServiceFacade, AppRoutingService } from '.';
import { HTTP_STATUS_CODES, ErrorUtils } from '../util';

@Injectable()
export class HealthStatusService {
  constructor(
    private applicationServiceFacade: ApplicationServiceFacade,
    private appRoutingService: AppRoutingService
    ) { }

   public isHealthStatusOk(): Observable<boolean> {
      return this.applicationServiceFacade
        .buildGetEnvironmentHealthStatus()
        .map(response => {
          if (response.status === HTTP_STATUS_CODES.OK)
            if (response.json().status === 'ok')
              return true;

          return false;
        }, this);
  }

  public getEnvironmentHealthStatus(): Observable<GeneralEnvironmentStatus> {
    return this.applicationServiceFacade
    .buildGetEnvironmentHealthStatus()
    .map(response => response.json())
    .catch(ErrorUtils.handleServiceError);
  }

  public getEnvironmentStatuses(): Observable<GeneralEnvironmentStatus> {
    const body = '?full=1';
    return this.applicationServiceFacade
    .buildGetEnvironmentStatuses(body)
    .map(response => response.json())
    .catch(ErrorUtils.handleServiceError);
  }

  public runEdgeNode(): Observable<{}> {
    return this.applicationServiceFacade
      .buildRunEdgeNodeRequest()
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public suspendEdgeNode(): Observable<{}> {
    return this.applicationServiceFacade
      .buildSuspendEdgeNodeRequest()
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public recreateEdgeNode(): Observable<{}> {
    return this.applicationServiceFacade
      .buildRecreateEdgeNodeRequest()
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }
  
  public isBillingEnabled(): Observable<boolean> {
    return this.applicationServiceFacade
    .buildGetEnvironmentHealthStatus()
    .map(response => {
      if (response.status === HTTP_STATUS_CODES.OK) {
        const data = response.json();
        if (!data.billingEnabled) {
          this.appRoutingService.redirectToHomePage();
          return false;
        }
      }
      return true;
    });
  }

  public getActiveUsers(): Observable<Array<string>> {
    return this.applicationServiceFacade
      .buildGetActiveUsers()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public manageEnvironment(act, data): Observable<Response | {}> {
    const action = `/${act}`;
    return this.applicationServiceFacade
      .buildManageEnvironment(action, data)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public getSsnMonitorData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetSsnMonitorData()
      .map(response => response.json())
      .catch(error => error);
  }
}
