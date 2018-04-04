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

import { ApplicationServiceFacade, AppRoutingService } from './';
import { HTTP_STATUS_CODES } from '../util';

@Injectable()
export class HealthStatusService {
  constructor(
    private applicationServiceFacade: ApplicationServiceFacade,
    private appRoutingService: AppRoutingService
    ) { }

   public isHealthStatusOk(): Observable<boolean> {
      return this.applicationServiceFacade
        .buildGetEnvironmentHealthStatus()
        .map((response: Response) => {
          if (response.status === HTTP_STATUS_CODES.OK)
            if (response.json().status === 'ok')
              return true;

          return false;
        }, this);
  }

  public getEnvironmentHealthStatus(): Observable<Response> {
    return this.applicationServiceFacade
    .buildGetEnvironmentHealthStatus()
    .map((response: Response) => response.json())
    .catch((error: any) => error);
  }

  public getEnvironmentStatuses(): Observable<Response> {
    const body = '?full=1';
    return this.applicationServiceFacade
    .buildGetEnvironmentStatuses(body)
    .map((response: Response) => response.json())
    .catch((error: any) => error);
  }

  public runEdgeNode(): Observable<Response> {
    return this.applicationServiceFacade
      .buildRunEdgeNodeRequest()
      .map((response: Response) => response);
  }

  public suspendEdgeNode(): Observable<Response> {
    return this.applicationServiceFacade
      .buildSuspendEdgeNodeRequest()
      .map((response: Response) => response);
  }

  public recreateEdgeNode(): Observable<Response> {
    return this.applicationServiceFacade
      .buildRecreateEdgeNodeRequest()
      .map((response: Response) => response);
  }
  
  public isBillingEnabled(): Observable<boolean> {
    return this.applicationServiceFacade
    .buildGetEnvironmentHealthStatus()
    .map((response: Response) => {
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

  public getActiveUsers(): Observable<Response> {
    return this.applicationServiceFacade
      .buildGetActiveUsers()
      .map((response: Response) => response.json());
  }

  public manageEnvironment(act, data): Observable<Response> {
    const action = `/${act}`;
    return this.applicationServiceFacade
      .buildManageEnvironment(action, data)
      .map((response: Response) => response);
  }
}
