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
import { of as observableOf, Observable, BehaviorSubject } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { AppRoutingService } from './appRouting.service';
import { StorageService } from './storage.service';
import { LoginModel } from '../../login/login.model';
import { ErrorUtils, HTTP_STATUS_CODES } from '../util';
import { DICTIONARY } from '../../../dictionary/global.dictionary';

@Injectable()
export class ApplicationSecurityService {
  private _loggedInStatus = new BehaviorSubject<boolean>(null);
  readonly DICTIONARY = DICTIONARY;

  emitter: BehaviorSubject<any> = new BehaviorSubject<any>('');
  emitter$ = this.emitter.asObservable();

  constructor(
    private serviceFacade: ApplicationServiceFacade,
    private appRoutingService: AppRoutingService,
    private storage: StorageService,
  ) { }

  get loggedInStatus() {
    return this._loggedInStatus.asObservable();
  }

  public locationCheck() {
    return this.serviceFacade.buildLocationCheck()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public login(loginModel: LoginModel): Observable<boolean | {}> {
    return this.serviceFacade
      .buildLoginRequest(loginModel.toJsonString())
      .pipe(
        map(response => {
          if (response.status === HTTP_STATUS_CODES.OK) {
            if (!DICTIONARY.use_ldap) {
              this.storage.setAuthToken(response.body.access_token);
              this.storage.setUserName(response.body.username);
            } else {
              this.storage.setAuthToken(response.body);
              this.storage.setUserName(loginModel.username);
            }
            this._loggedInStatus.next(true);
            return true;
          }
          this._loggedInStatus.next(false);
          return false;
        }),
        catchError(ErrorUtils.handleServiceError));
  }

  public logout(): Observable<boolean> {
    const authToken = this.storage.getToken();

    if (!!authToken) {
      return this.serviceFacade
        .buildLogoutRequest()
        .pipe(
          map(response => {
            this.storage.destroyToken();
            this.storage.setBillingQuoteUsed('');
            this._loggedInStatus.next(false);

            return response.status === HTTP_STATUS_CODES.OK;
          }, this));
    }

    return observableOf(false);
  }

  public isLoggedIn(): Observable<boolean> {
    const authToken = this.storage.getToken();
    const currentUser = this.storage.getUserName();
    if (authToken && currentUser) {
      return this.serviceFacade
        .buildAuthorizeRequest(currentUser)
        .pipe(
          map(response => {
            if (response.status === HTTP_STATUS_CODES.OK) {
              this._loggedInStatus.next(true);
              return true;
            }

            this.storage.destroyToken();
            return false;
          }),
          catchError(error => {
            this.emmitMessage(error.message);
            this.storage.destroyToken();

            return observableOf(false);
          }));
    }
    this._loggedInStatus.next(false);
    return observableOf(false);
  }

  public redirectParams(params): Observable<boolean> {
    const code = `?code=${params.code}`;

    return this.serviceFacade
      .buildGetAuthToken(code)
      .pipe(
        map((response: any) => {
          const data = response.body;
          if (response.status === HTTP_STATUS_CODES.OK && data.access_token) {
            this.storage.setAuthToken(data.access_token);
            this.storage.setUserName(data.username);

            this.appRoutingService.redirectToHomePage();
            return true;
          }

          if (response.status !== 200) {
            const errObj = response.body;
            this.emmitMessage(errObj.message);
          }
          return false;

        }), catchError((error: any) => {
          this.emmitMessage(error.message);
          return observableOf(false);
        }));
  }

  private emmitMessage(message): void {
    this.appRoutingService.redirectToLoginPage();
    this.emitter.next(message);
  }
}
