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

import {  Injectable } from '@angular/core';
import {  Observable } from 'rxjs';
import {  catchError } from 'rxjs/operators';
import {  ErrorUtils } from '../util';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import {
  ImageFilterFormValue,
  ProjectImagesInfo,
  ShareImageAllUsersParams
} from '../../resources/images';

@Injectable()
export class UserImagesPageService {
  constructor(
    private applicationServiceFacade: ApplicationServiceFacade
  ) { }


  getFilterImagePage(): Observable<ProjectImagesInfo> {
    return this.applicationServiceFacade.buildGetUserImagePage()
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }

  filterImagePage(params: ImageFilterFormValue): Observable<ProjectImagesInfo> {
    return this.applicationServiceFacade.buildFilterUserImagePage(params)
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }

  shareImagesAllUser(shareParams: ShareImageAllUsersParams): Observable<ProjectImagesInfo> {
    return this.applicationServiceFacade.buildShareImageAllUsers(shareParams)
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }
}
