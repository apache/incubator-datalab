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

import { ErrorUtils } from '../util';
import { ApplicationServiceFacade } from './applicationServiceFacade.service';

@Injectable()
export class EndpointService {

  constructor(
    private applicationServiceFacade: ApplicationServiceFacade
  ) { }

  public getEndpointsData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetEndpointsData()
      .pipe(
        catchError(ErrorUtils.handleServiceError));
  }

  public createEndpoint(data): Observable<any> {
    return this.applicationServiceFacade
      .buildCreateEndpoint(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getEndpointsResource(endpoint): Observable<any> {
    return this.applicationServiceFacade
      .getEndpointsResource(endpoint)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public deleteEndpoint(data): Observable<any> {
    const url = `/${data}`;
    return this.applicationServiceFacade
      .buildDeleteEndpoint(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getEndpoinConnectionStatus(endpointUrl): Observable<{}> {
    return this.applicationServiceFacade
      .getEndpointConnectionStatus(endpointUrl)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}

