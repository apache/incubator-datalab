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

import { ErrorUtils } from '../util/';
import { ApplicationServiceFacade } from '.';

@Injectable()
export class UserResourceService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getExploratoryEnvironmentTemplates(): Observable<any> {
    return this.applicationServiceFacade
      .buildGetExploratoryEnvironmentTemplatesRequest()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public getComputationalResourcesTemplates(): Observable<any> {
    return this.applicationServiceFacade
      .buildGetComputationalResourcesTemplatesRequest()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public getUserProvisionedResources(): Observable<any> {
    return this.applicationServiceFacade
      .buildGetUserProvisionedResourcesRequest()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public createExploratoryEnvironment(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateExploratoryEnvironmentRequest(body)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public runExploratoryEnvironment(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildRunExploratoryEnvironmentRequest(body)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public suspendExploratoryEnvironment(notebook: any, action): Observable<{}> {
    const url = '/' + notebook.name + '/' + action;

    return this.applicationServiceFacade
      .buildSuspendExploratoryEnvironmentRequest(JSON.stringify(url))
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public createComputationalResource_DataengineService(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineServiceRequest(body)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public createComputationalResource_Dataengine(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineRequest(body)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public suspendComputationalResource(notebookName: string, computationalResourceName: string): Observable<{}> {
    const body = JSON.stringify('/' + notebookName + '/' + computationalResourceName + '/terminate');
    return this.applicationServiceFacade
      .buildDeleteComputationalResourcesRequest(body)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public toggleStopStartAction(notebook: string, resource: string, action): Observable<{}> {
    const url = `/${notebook}/${resource}/${action}`;
    if (action === 'stop') {
      return this.applicationServiceFacade
        .buildStopSparkClusterAction(JSON.stringify(url))
        .map(response => response)
        .catch(ErrorUtils.handleServiceError);
    } else if (action === 'start') {
      return this.applicationServiceFacade
        .buildStartSparkClusterAction(url)
        .map(response => response)
        .catch(ErrorUtils.handleServiceError);
    }
  }

  public getUserPreferences(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetUserPreferences()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public updateUserPreferences(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildUpdateUserPreferences(body)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }

  public getUserImages(image): Observable<{}> {
    const body = `?docker_image=${image}`;
    return this.applicationServiceFacade
      .buildGetUserImages(body)
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public getImagesList(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetImagesList()
      .map(response => response.json())
      .catch(ErrorUtils.handleServiceError);
  }

  public createAMI(data): Observable<any> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateAMI(data)
      .map(response => response)
      .catch(ErrorUtils.handleServiceError);
  }
}
