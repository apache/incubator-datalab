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

export class TodoItemNode {
  children: TodoItemNode[];
  item: string;
  id: string;
  object: any;
}

export class TodoItemFlatNode {
  item: string;
  level: number;
  expandable: boolean;
  obj: string;
}

@Injectable({
  providedIn: 'root'
})
export class BucketBrowserService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getBucketData(bucket, endpoint): Observable<{}> {
    const url = `/${bucket}/endpoint/${endpoint}`;
    return this.applicationServiceFacade
      .buildGetBucketData(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public downloadFile(data) {
    return this.applicationServiceFacade
      .buildDownloadFileFromBucket(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public uploadFile(data) {
    return this.applicationServiceFacade
      .buildUploadFileToBucket(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public createFolder(data) {
    return this.applicationServiceFacade
      .buildCreateFolderInBucket(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public deleteFile(data) {
    return this.applicationServiceFacade
      .buildDeleteFileFromBucket(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
