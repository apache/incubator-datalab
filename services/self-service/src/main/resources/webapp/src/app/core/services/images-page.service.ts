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
import { catchError } from 'rxjs/operators';
import {  ErrorUtils } from '../util';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import {
  ImageActionType,
  ImageFilterFormValue,
  ProjectImagesInfo,
  ImageParams,
  URL_Chunk
} from '../../resources/images';
import { ShareDialogData, UserData } from '../../resources/exploratory/image-action-dialog/image-action.model';

@Injectable()
export class ImagesPageService {
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

  shareImagesAllUser(shareParams: ImageParams): Observable<ProjectImagesInfo> {
    return this.applicationServiceFacade.buildShareImageAllUsers(shareParams)
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }

   terminateImage(image: ImageParams, action: ImageActionType): Observable<ProjectImagesInfo> {
    const url = `/${image.projectName}/${image.endpoint}/${image.imageName}/${action}`;

    return this.applicationServiceFacade
      .buildDeleteImage(JSON.stringify(url))
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }

  getImageShareInfo(imageInfo: ImageParams): Observable<ShareDialogData> {
    const { imageName, projectName, endpoint} = imageInfo;
    const url = `/${URL_Chunk.sharingInfo}/${imageName}/${projectName}/${endpoint}/`;
    return this.applicationServiceFacade
      .buildGetImageShareInfo(url)
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }

  getUserDataForShareDropdown(imageInfo: ImageParams, userData: string): Observable<UserData[]> {
    const { imageName, projectName, endpoint} = imageInfo;
    const url = `/${URL_Chunk.autocomplete}/${imageName}/${projectName}/${endpoint}?value=${userData}`;
    return this.applicationServiceFacade
      .buildGetImageShareInfo(url)
      .pipe(
        catchError(ErrorUtils.handleServiceError)
      );
  }
}
