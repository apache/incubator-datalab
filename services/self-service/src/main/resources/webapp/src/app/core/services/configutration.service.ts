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
import {Observable, of} from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';

@Injectable()
export class ConfigurationService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getServiceSettings(service): Observable<{}> {
     return this.applicationServiceFacade
      .buildGetServiceConfig(service)
       .pipe(
      map(response => response),
      catchError(ErrorUtils.handleServiceError));
    }

  public setServiceConfig(service, config) {
    const settings = {
      ymlString: config
    };
    return this.applicationServiceFacade
      .buildSetServiceConfig(service, settings)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public restartServices(self, prov, billing) {
    const queryString = `?billing=${billing}&provserv=${prov}&ui=${self}`;
    return this.applicationServiceFacade
      .buildRestartServices(queryString)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

}
