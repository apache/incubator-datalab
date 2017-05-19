/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import { Injectable } from '@angular/core';
import { Response } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';

import { LoginModel } from '../../login/login.model';
import { ApplicationServiceFacade, AppRoutingService } from './';
import { HTTP_STATUS_CODES } from '../util';

@Injectable()
export class ApplicationSecurityService {
  private accessTokenKey: string = 'access_token';
  private userNameKey: string = 'user_name';

  constructor(
    private serviceFacade: ApplicationServiceFacade,
    private appRoutingService: AppRoutingService
  ) { }

  public login(loginModel: LoginModel): Observable<boolean> {
    return this.serviceFacade
      .buildLoginRequest(loginModel.toJsonString())
      .map((response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.setAuthToken(response.text());
          this.setUserName(loginModel.username);

          return true;
        }
        return false;
      }, this);
  }

  public logout(): Observable<boolean> {
    const authToken = this.getAuthToken();

    if (!!authToken) {
      return this.serviceFacade
        .buildLogoutRequest()
        .map((response: Response) => {
          this.clearAuthToken();

          return response.status === HTTP_STATUS_CODES.OK;
        }, this);
    }

    return Observable.of(false);
  }

  public getCurrentUserName(): string {
    return localStorage.getItem(this.userNameKey);
  }

  public getAuthToken(): string {
    return localStorage.getItem(this.accessTokenKey);
  }

  public isLoggedIn(): Observable<boolean> {
    const authToken = this.getAuthToken();
    const currentUser = this.getCurrentUserName();

    if (authToken && currentUser) {
      return this.serviceFacade
        .buildAuthorizeRequest(currentUser)
        .map((response: Response) => {
          if (response.status === HTTP_STATUS_CODES.OK)
            return true;

          this.clearAuthToken();
          this.appRoutingService.redirectToLoginPage();
          return false;
        }, this);
    }

    this.appRoutingService.redirectToLoginPage();
    return Observable.of(false);
  }

  private setUserName(userName): void {
    localStorage.setItem(this.userNameKey, userName);
  }

  private setAuthToken(accessToken): void {
    const encodedToken = accessToken;
    localStorage.setItem(this.accessTokenKey, encodedToken);
  }

  private clearAuthToken(): void {
    localStorage.removeItem(this.accessTokenKey);
  }
}
