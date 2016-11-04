/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

import { Injectable } from '@angular/core';
import {Response} from '@angular/http';
import {Observable} from "rxjs";
import {ApplicationServiceFacade} from "./applicationServiceFacade.service";
import {AppRoutingService} from "../routing/appRouting.service";

@Injectable()
export class ApplicationSecurityService {
  private accessTokenKey : string = "access_token";
  private userNameKey : string = "user_name";

  constructor(private serviceFacade : ApplicationServiceFacade, private appRoutingService : AppRoutingService) {
  }

  public login(userName, password) : Observable<boolean> {
    let body = JSON.stringify({'username': userName, 'password': password, 'access_token': ''});

    return this.serviceFacade.buildLoginRequest(body).map((response : Response) => {
      if (response.status === 200) {
        this.setAuthToken(response.text());
        this.setUserName(userName);

        return true;
      }
      return false;
    }, this);
  }

  public logout() : Observable<boolean> {
    let authToken = this.getAuthToken();

    if(!!authToken)
    {
      this.clearAuthToken();
      return this.serviceFacade.buildLogoutRequest("").map((response: Response) => {
        return response.status === 200;
      }, this)
    }

    return Observable.of(false);
  }

  public getCurrentUserName() : string {
    return localStorage.getItem(this.userNameKey);
  }

  public getAuthToken() : string {
    return localStorage.getItem(this.accessTokenKey);
  }

  public isLoggedIn() : Observable<boolean> {
    let authToken = this.getAuthToken();
    let currentUser = this.getCurrentUserName();

    if(authToken && currentUser)
    {
      return this.serviceFacade.buildAuthorizeRequest(currentUser).map((response : Response) => {
        if(response.status === 200)
          return true;

        this.clearAuthToken();
        this.appRoutingService.redirectToLoginPage();
        return false;
      }, this);
    }

    this.appRoutingService.redirectToLoginPage();
    return Observable.of(false);
  }

  private setUserName(userName)
  {
    localStorage.setItem(this.userNameKey, userName);
  }

  private setAuthToken(accessToken){
    let encodedToken = accessToken;
    localStorage.setItem(this.accessTokenKey, encodedToken);
  }

  private clearAuthToken(){
    localStorage.removeItem(this.accessTokenKey);
  }
}
