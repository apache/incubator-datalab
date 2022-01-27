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
import { Observable, BehaviorSubject } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';

@Injectable()
export class UserAccessKeyService {
  _accessKeyEmitter: BehaviorSubject<any> = new BehaviorSubject<boolean>(null);
  _keyUploadProccessEmitter: BehaviorSubject<any> = new BehaviorSubject<boolean>(null);

  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  get accessKeyEmitter() {
    return this._accessKeyEmitter.asObservable();
  }

  get keyUploadProccessEmitter() {
    return this._keyUploadProccessEmitter.asObservable();
  }

  public initialUserAccessKeyCheck() {
    this.checkUserAccessKey()
      .subscribe(
        response => {
          this._accessKeyEmitter.next(response);
        }, 
        error => {
          this._accessKeyEmitter.next(error);
        }
      );
  }

  public checkUserAccessKey(): Observable<{}> {
    console.log('checkUserAccessKey');
    return this.applicationServiceFacade
      .buildCheckUserAccessKeyRequest()
      .pipe(
        map(response => {
          return response;
        }),
        catchError(ErrorUtils.handleServiceError));
  }

  public emitActionOnKeyUploadComplete() {
    console.log('key uploaded!!!!!!!!!!!!!');
    this._keyUploadProccessEmitter.next(true);
  }

  public generateAccessKey(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGenerateAccessKey()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public regenerateAccessKey(): Observable<{}> {
    const param = '?is_primary_uploading=false';
    return this.applicationServiceFacade
      .buildRegenerateAccessKey(param)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public uploadUserAccessKey(data): Observable<{}> {
    return this.applicationServiceFacade
      .buildUploadUserAccessKeyRequest(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public reuploadUserAccessKey(data): Observable<{}> {
    const param = '?is_primary_uploading=false';
    return this.applicationServiceFacade
      .buildReuploadUserAccessKeyRequest(data, param)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public resetUserAccessKey() {
    this._accessKeyEmitter.next(<any>{});
  }
}
