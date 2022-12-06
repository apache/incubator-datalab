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

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';
import { ActionTypeOptions } from '../../administration/management/management.model';

@Injectable()
export class ManageEnvironmentsService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {}

  getAllEnvironmentData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetAllEnvironmentData()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  environmentManagement(data, action: ActionTypeOptions, project: string, resource: string, computational?: string): Observable<{}> {
    const params = computational ? `/${action}/${project}/${resource}/${computational}` : `/${action}/${project}/${resource}`;
    const headers = action === 'createImage' ? { 'Content-Type': 'application/json; charset=UTF-8' } : { 'Content-Type': 'text/plain' };
    return this.applicationServiceFacade
      .buildEnvironmentManagement(params, data, headers)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
