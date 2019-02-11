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
import { StorageService } from '../services/storage.service';
import {
    HttpInterceptor,
    HttpRequest,
    HttpHandler,
    HttpEvent
} from '@angular/common/http';

import { Observable } from 'rxjs';

@Injectable() export class HttpTokenInterceptor implements HttpInterceptor {
  constructor(private jwtService: StorageService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.jwtService.getToken();
    const headersConfig = {
      'Content-Type': 'application/json; charset=UTF-8'
    };

    if (token)
      headersConfig['Authorization'] = `Bearer ${token}`;

    if (request.headers.has('text-content'))
      headersConfig['Content-Type'] = 'text/plain';

    const header = request.clone({ setHeaders: headersConfig });
    return next.handle(header);
  }
}
