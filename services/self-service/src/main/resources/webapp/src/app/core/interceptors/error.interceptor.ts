/***************************************************************************

Copyright (c) 2019, EPAM SYSTEMS INC

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
import {
    HttpInterceptor,
    HttpRequest,
    HttpHandler,
    HttpEvent
} from '@angular/common/http';

import { Observable, throwError, of as observableOf } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { StorageService, AppRoutingService } from '../services';
import { HTTP_STATUS_CODES } from '../util';

@Injectable() export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private jwtService: StorageService,
    private routingService: AppRoutingService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError(error => {
        let url = error.url;

        if (url.indexOf('?') > -1) {
          url = url.substr(0, url.indexOf('?'));
        }

        if ((error.status === HTTP_STATUS_CODES.UNAUTHORIZED) && !url.endsWith('login')) {
          this.jwtService.destroyToken();
          this.routingService.redirectToLoginPage();
          return observableOf(error);
        } else {
          return throwError(error);
        }
      }));
    }
}
