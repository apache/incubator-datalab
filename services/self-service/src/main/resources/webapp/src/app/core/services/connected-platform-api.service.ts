/*!
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
import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { catchError } from 'rxjs/operators';
import { ErrorUtils } from '../util';
import { Observable } from 'rxjs';
import { AddPlatformFromValue, ConnectedPlatformsInfo } from '../../resources/connected-platforms/connected-platforms.models';

@Injectable({
  providedIn: 'root'
})
export class ConnectedPlatformApiService {

  constructor(
    private applicationServiceFacade: ApplicationServiceFacade
  ) { }

  getConnectedPlatformsPage(): Observable<ConnectedPlatformsInfo> {
    return this.applicationServiceFacade.buildGetConnectedPlatformsPage()
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }

  addPlatform(platformParams: AddPlatformFromValue) {
    return  this.applicationServiceFacade.buildAddPlatform(platformParams).pipe(
      catchError(ErrorUtils.handleServiceError)
    );
  }

  disconnectPlatform(platformName: string) {
    return  this.applicationServiceFacade.buildDisconnectPlatform(platformName).pipe(
      catchError(ErrorUtils.handleServiceError)
    );
  }
}
