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
import { map, catchError } from 'rxjs/operators';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';

@Injectable()
export class ProjectService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public createProject(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildCreateProject(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public updateProject(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildUpdateProject(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getProjectsList(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetProjectsList()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getUserProjectsList(isActive?): Observable<{}> {
    const params = isActive ? '/me?active=true' : '';
    return this.applicationServiceFacade
      .buildGetUserProjectsList(params)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public toggleProjectStatus(data, action): Observable<{}> {
    const url = `/${action}`;
    return this.applicationServiceFacade
      .buildToggleProjectStatus(url, data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public updateProjectsBudget(data): Observable<{}> {
    const url = '/budget';
    return this.applicationServiceFacade
      .buildUpdateProjectsBudget(url, data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
