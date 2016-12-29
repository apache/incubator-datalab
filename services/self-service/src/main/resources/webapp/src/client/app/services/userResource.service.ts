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
import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { Observable } from 'rxjs';

@Injectable()
export class UserResourceService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {
  }

  public getExploratoryEnvironmentTemplates(): Observable<any> {
    return this.applicationServiceFacade
      .buildGetExploratoryEnvironmentTemplatesRequest()
      .map((res: Response) => res.json())
      .catch((error: any) => error);
  }

  public getComputationalResourcesTemplates(): Observable<any> {
    return this.applicationServiceFacade
      .buildGetComputationalResourcesTemplatesRequest()
      .map((res: Response) => res.json())
      .catch((error: any) => error);
  }

  public getUserProvisionedResources(): Observable<any> {
    return this.applicationServiceFacade
      .buildGetUserProvisionedResourcesRequest()
      .map((response: Response) => response.json())
      .catch((error: any) => error);
  }

  public createExploratoryEnvironment(data): Observable<Response> {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateExploratoryEnvironmentRequest(body)
      .map((response: Response) => response);
  }

  public runExploratoryEnvironment(data): Observable<Response> {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildRunExploratoryEnvironmentRequest(body)
      .map((response: Response) => response);
  }

  public suspendExploratoryEnvironment(notebook: any, action): Observable<Response> {
    let url = '/' + notebook.name + '/' + action;

    return this.applicationServiceFacade
      .buildSuspendExploratoryEnvironmentRequest(JSON.stringify(url))
      .map((response: Response) => response);
  }

  public createComputationalResource(data): Observable<Response> {
    let body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResourcesRequest(body)
      .map((response: Response) => response);
  }

  public suspendComputationalResource(notebookName: string, computationalResourceName: string): Observable<Response> {
    let body = JSON.stringify('/' + notebookName + '/' + computationalResourceName + '/terminate');
    return this.applicationServiceFacade
      .buildDeleteComputationalResourcesRequest(body)
      .map((response: Response) => response);
  }
}
