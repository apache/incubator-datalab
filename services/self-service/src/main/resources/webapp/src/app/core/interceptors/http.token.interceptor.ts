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
import { StorageService } from '../services/storage.service';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';

import { Observable } from 'rxjs';

@Injectable() export class HttpTokenInterceptor implements HttpInterceptor {
  constructor(private jwtService: StorageService) { }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.jwtService.getToken();
    const headersConfig = {};

    if (token)
      headersConfig['Authorization'] = `Bearer ${token}`;

    // if (request.url.indexOf('api/bucket') !== -1) {
    //   headersConfig['Authorization'] = `Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJfSEVvZmliX2djZXJpcllidE51dVBoSk81OEJNOFc5M1dHZW9VR3hTR2l3In0.eyJqdGkiOiIxY2E4OTQ1OS02MDU5LTQzOTctYTZhMy1kMzY5YTY0OTkyNzIiLCJleHAiOjE1ODc5OTUyNjgsIm5iZiI6MCwiaWF0IjoxNTg3OTk0OTY4LCJpc3MiOiJodHRwczovL2lkcC5kZW1vLmRsYWJhbmFseXRpY3MuY29tL2F1dGgvcmVhbG1zL2RsYWIiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiNWIwYWEwMmYtYTU3ZS00MGM0LTk4ODQtNDlmYmU5OGViMzU4IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoib2Z1a3MtMTMwNC11aSIsImF1dGhfdGltZSI6MTU4Nzk5MDU0NCwic2Vzc2lvbl9zdGF0ZSI6IjUwY2E5Y2I5LWFhODAtNDEwMS1hYzdjLWMzNzk3NzJlMWU3YiIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cDovLzM1LjIzMy4xODMuNTUiLCJodHRwczovLzM1LjIzMy4xODMuNTUiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJBbm5hIE9ybG92c2thIFZpdGFuc2thIiwiZ3JvdXBzIjpbImFkbWluIl0sInByZWZlcnJlZF91c2VybmFtZSI6InZpcmFfdml0YW5za2FAZXBhbS5jb20iLCJnaXZlbl9uYW1lIjoiQW5uYSBPcmxvdnNrYSIsImZhbWlseV9uYW1lIjoiVml0YW5za2EiLCJlbWFpbCI6InZpcmFfdml0YW5za2FAZXBhbS5jb20ifQ.hw6BfsM9bKIkOLhUXiVWi2pWAILqgECXxRbGiDPOGXwe4tLwSJvn-zkUlbLYOuZJGIWt4rujGgB61x5uR2BA-0ZMYWKBomBIigrD2IAdmxKvwSsFGnVQ3D_dl9smkjNHOYX38Ca2fLhgcJqyijwYwQq9M4Fp5kVGp4cHYLsEXd9ZxR6Rm1se-2u-mexzBe2VGP778h9cs1or4zKk_IWWa-mb2hWPZ9ers-8dqUO_w6JUL0wsDRkPg14pjWGfwOVDGC2M7cLQEz8pOPQgD0cbY-kXi2fi7S1g8sk4YZywKH5d9RG8ZhMznxMmSbgiURrQ5ZqY5rfknMAeRZx8cxrHmA`;
    // }

    if (!request.headers.has('Content-Type')
      && !request.headers.has('Upload')
      && request.url.indexOf('upload') === -1
      && request.url.indexOf('download') === -1)

      headersConfig['Content-Type'] = 'application/json; charset=UTF-8';

    const header = request.clone({ setHeaders: headersConfig });
    return next.handle(header);
  }
}
