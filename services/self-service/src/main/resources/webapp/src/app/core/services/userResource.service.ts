/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ErrorUtils } from '../util/';
import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import {
  ComputationalResourceModel
} from '../../resources/computational/computational-resource-create-dialog/computational.resource.model';

@Injectable()
export class UserResourceService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getExploratoryTemplates(project, endpoint): Observable<any> {
    const url = `/${project}/${endpoint}/exploratory_templates`;
    return this.applicationServiceFacade
      .buildGetTemplatesRequest(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getComputationalTemplates(project, endpoint, provider): Observable<ComputationalResourceModel> {
    const url = `/${project}/${endpoint}/templates`;
    return this.applicationServiceFacade
      .buildGetComputationTemplatesRequest(url, provider)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getUserProvisionedResources(): Observable<any> {
    return this.applicationServiceFacade
      .buildGetUserProvisionedResourcesRequest()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createExploratoryEnvironment(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateExploratoryEnvironmentRequest(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public runExploratoryEnvironment(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildRunExploratoryEnvironmentRequest(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getProjectByExploratoryEnvironment(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetExploratoryEnvironmentRequest()
      .pipe(
        map(response => response.body.project_exploratories),
        catchError(ErrorUtils.handleServiceError));
  }

  public suspendExploratoryEnvironment(notebook: any, action): Observable<{}> {
    const url = '/' + notebook.project + '/' + notebook.name + '/' + action;

    return this.applicationServiceFacade
      .buildSuspendExploratoryEnvironmentRequest(JSON.stringify(url))
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createComputationalResource_DataengineService(data, provider): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineServiceRequest(body, provider)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createComputationalResource_Dataengine(data, provider): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineRequest(body, provider)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public suspendComputationalResource(
    projectName: string,
    notebookName: string,
    computationalResourceName: string,
    provider: string
  ): Observable<{}> {
    const body = JSON.stringify('/' + projectName + '/' + notebookName + '/' + computationalResourceName + '/terminate');
    return this.applicationServiceFacade
      .buildDeleteComputationalResourcesRequest(body, provider)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public toggleStopStartAction(
    project: string,
    notebook: string,
    resource: string,
    action,
    provider: string
  ): Observable<{}> {
    const url = `/${project}/${notebook}/${resource}/${action}`;
    if (action === 'stop') {
      return this.applicationServiceFacade
        .buildStopSparkClusterAction(JSON.stringify(url), provider)
        .pipe(
          map(response => response),
          catchError(ErrorUtils.handleServiceError));
    } else if (action === 'start') {
      return this.applicationServiceFacade
        .buildStartSparkClusterAction(url, provider)
        .pipe(
          map(response => response),
          catchError(ErrorUtils.handleServiceError));
    }
  }

  public getUserPreferences(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetUserPreferences()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public updateUserPreferences(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildUpdateUserPreferences(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getUserImages(image, project, endpoint): Observable<{}> {
    const body = `?docker_image=${image}&project=${project}&endpoint=${endpoint}`;
    return this.applicationServiceFacade
      .buildGetUserImages(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getImagesList(project?): Observable<{}> {
    const body = project ? `/all?project=${project}` : '';
    return this.applicationServiceFacade
      .buildGetImagesList(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createAMI(data): Observable<any> {
    return this.applicationServiceFacade
      .buildCreateAMI(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
