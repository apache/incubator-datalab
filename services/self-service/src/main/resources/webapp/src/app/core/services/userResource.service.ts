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

@Injectable()
export class UserResourceService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getExploratoryTemplates(project): Observable<any> {
    const url = `/${project}/exploratory_templates`;
    return this.applicationServiceFacade
      .buildGetTemplatesRequest(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getComputationalTemplates(project): Observable<any> {
    const url = `/${project}/computational_templates`;
    return this.applicationServiceFacade
      .buildGetTemplatesRequest(url)
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

  public suspendExploratoryEnvironment(notebook: any, action): Observable<{}> {
    const url = '/' + notebook.name + '/' + action;

    return this.applicationServiceFacade
      .buildSuspendExploratoryEnvironmentRequest(JSON.stringify(url))
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createComputationalResource_DataengineService(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineServiceRequest(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createComputationalResource_Dataengine(data): Observable<{}> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateComputationalResources_DataengineRequest(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public suspendComputationalResource(notebookName: string, computationalResourceName: string): Observable<{}> {
    const body = JSON.stringify('/' + notebookName + '/' + computationalResourceName + '/terminate');
    return this.applicationServiceFacade
      .buildDeleteComputationalResourcesRequest(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public toggleStopStartAction(project, notebook: string, resource: string, action): Observable<{}> {
    const url = `/${project}/${notebook}/${resource}/${action}`;
    if (action === 'stop') {
      return this.applicationServiceFacade
        .buildStopSparkClusterAction(JSON.stringify(url))
        .pipe(
          map(response => response),
          catchError(ErrorUtils.handleServiceError));
    } else if (action === 'start') {
      return this.applicationServiceFacade
        .buildStartSparkClusterAction(url)
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

  public getUserImages(image): Observable<{}> {
    const body = `?docker_image=${image}`;
    return this.applicationServiceFacade
      .buildGetUserImages(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getImagesList(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetImagesList()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createAMI(data): Observable<any> {
    const body = JSON.stringify(data);
    return this.applicationServiceFacade
      .buildCreateAMI(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
