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
export class ConfigurationService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getServiceSettings(endpoint): Observable<{}> {
    const queryString = `?endpoint=${endpoint}`;
    return this.applicationServiceFacade
      .buildGetServiceConfig(queryString)
       .pipe(
      map(response => response),
      catchError(ErrorUtils.handleServiceError));
    }

  public setServiceConfig(service: string, config: string, endpoint: string): Observable<{}> {
    const settings = {
      ymlString: config,
      endpointName: endpoint
    };
    service = ConfigurationService.convertProvisioning(service);

    return this.applicationServiceFacade
      .buildSetServiceConfig(service, settings)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }


  public restartServices(ui: boolean, provserv: boolean, billing: boolean, endpoint: string): Observable<{}> {
    const body = {
      billing, provserv, ui, endpoint
    };
    return this.applicationServiceFacade
      .buildRestartServices(body)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  private static convertProvisioning(service: string): string {
    return (service === 'provisioning') ? 'provisioning-service' : service;
  }

}
