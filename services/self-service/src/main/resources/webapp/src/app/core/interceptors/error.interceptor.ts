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
import {BehaviorSubject} from 'rxjs';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';

import { Observable, throwError } from 'rxjs';
import { switchMap, filter, take, catchError } from 'rxjs/operators';
import { StorageService, AppRoutingService, ApplicationSecurityService } from '../services';
import { HTTP_STATUS_CODES } from '../util';

@Injectable() export class ErrorInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private jwtService: StorageService,
    private routingService: AppRoutingService,
    private auth: ApplicationSecurityService
  ) { }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError(error => {
        if (error.error && error.error.message && error.error.message.indexOf('query param artifact') !== -1) return throwError(error);
        if (error instanceof HttpErrorResponse) {
          switch ((<HttpErrorResponse>error).status) {
            case HTTP_STATUS_CODES.UNAUTHORIZED:
              return this.handleUnauthorized(request, next);
            case HTTP_STATUS_CODES.BAD_REQUEST:
              return this.handleBadRequest(error, request, next);
            default:
              return throwError(error);
          }
        } else {
          this.routingService.redirectToLoginPage();
          this.jwtService.destroyTokens();
          return throwError(error);
        }
      })
    );
  }

  private addToken(request: HttpRequest<any>, token: string) {
    return request.clone({ setHeaders: { 'Authorization': `Bearer ${token}` } });
  }

  private handleUnauthorized(request: HttpRequest<any>, next: HttpHandler) {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.jwtService.destroyAccessToken();
      this.refreshTokenSubject.next(null);

      return this.auth.refreshToken().pipe(
        switchMap((token: any) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(this.addToken(request, token.access_token));
          return next.handle(request);
        }));

    } else {
      return this.refreshTokenSubject.pipe(
        filter(token => token != null),
        take(1),
        switchMap(jwt => next.handle(this.addToken(request, jwt))));
    }
  }

  private handleBadRequest(error, request: HttpRequest<any>, next: HttpHandler) {
    if (error.url.indexOf('refresh') > -1) this.routingService.redirectToLoginPage();
    return next.handle(request);
  }
}