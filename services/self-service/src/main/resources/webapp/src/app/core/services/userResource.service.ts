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
export class UserResourceService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

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
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateExploratoryEnvironmentRequest(body)
      .map((response: Response) => response);
  }

  public runExploratoryEnvironment(data): Observable<Response> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildRunExploratoryEnvironmentRequest(body)
      .map((response: Response) => response);
  }

  public suspendExploratoryEnvironment(notebook: any, action): Observable<Response> {
    const url = '/' + notebook.name + '/' + action;

    return this.applicationServiceFacade
      .buildSuspendExploratoryEnvironmentRequest(JSON.stringify(url))
      .map((response: Response) => response);
  }

  public createComputationalResource_DataengineService(data): Observable<Response> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineServiceRequest(body)
      .map((response: Response) => response);
  }

  public createComputationalResource_Dataengine(data): Observable<Response> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineRequest(body)
      .map((response: Response) => response);
  }

  public suspendComputationalResource(notebookName: string, computationalResourceName: string): Observable<Response> {
    const body = JSON.stringify('/' + notebookName + '/' + computationalResourceName + '/terminate');
    return this.applicationServiceFacade
      .buildDeleteComputationalResourcesRequest(body)
      .map((response: Response) => response);
  }

  public toggleStopStartAction(notebook: string, resource: string, action): Observable<Response> {
    const url = `/${notebook}/${resource}/${action}`;
    if (action === 'stop') {
      return this.applicationServiceFacade
        .buildStopSparkClusterAction(JSON.stringify(url))
        .map((response: Response) => response);
    } else if (action === 'start') {
      return this.applicationServiceFacade
        .buildStartSparkClusterAction(url)
        .map((response: Response) => response);
    }
  }

  public getUserPreferences(): Observable<Response> {
    return this.applicationServiceFacade
      .buildGetUserPreferences()
      .map((response: Response) => response.json());
  }

  public updateUserPreferences(data): Observable<Response> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildUpdateUserPreferences(body)
      .map((response: Response) => response);
  }

  public getUserImages(image): Observable<Response> {
    const body = `?docker_image=${image}`;
    return this.applicationServiceFacade
      .buildGetUserImages(body)
      .map((response: Response) => response.json());
  }

  public getImagesList(): Observable<Response> {
    return this.applicationServiceFacade
      .buildGetImagesList()
      .map((response: Response) => response.json());
  }

  public createAMI(data): Observable<Response> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateAMI(data)
      .map((response: Response) => response);
  }
}
