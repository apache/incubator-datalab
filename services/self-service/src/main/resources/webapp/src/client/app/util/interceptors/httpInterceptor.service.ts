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

import { ConnectionBackend, RequestOptions, Http, Request, RequestOptionsArgs, Response, Headers } from '@angular/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import HTTP_STATUS_CODES from 'http-status-enum';

export class HttpInterceptor extends Http {
  constructor(
    backend: ConnectionBackend,
    defaultOptions: RequestOptions,
    private router: Router) {
    super(backend, defaultOptions);
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.request(url, options));
  }

  get(url: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.get(this.addNoCacheToUrl(url), options));
  }

  post(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.post(this.addNoCacheToUrl(url), body, this.getRequestOptionArgs(options)));
  }

  put(url: string, body: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.put(this.addNoCacheToUrl(url), body, this.getRequestOptionArgs(options)));
  }

  delete(url: string, options?: RequestOptionsArgs): Observable<Response> {
    return this.intercept(super.delete(this.addNoCacheToUrl(url), options));
  }

  getRequestOptionArgs(options?: RequestOptionsArgs): RequestOptionsArgs {
    if (options === null)
      options = new RequestOptions();

    if (options.headers === null)
      options.headers = new Headers();

    return options;
  }

  intercept(observable: Observable<Response>): Observable<Response> {
    return observable.catch((err, source) => {
      let url = err.url;

      if (url.indexOf('?') > -1) {
        url = url.substr(0, url.indexOf('?'));
      }

      if ((err.status === HTTP_STATUS_CODES.FORBIDDEN
        || err.status === HTTP_STATUS_CODES.UNAUTHORIZED)
        && !url.endsWith('login')) {
        localStorage.removeItem('access_token');
        this.router.navigate(['/login']);
        return Observable.of(err);
      } else {
        return Observable.throw(err);
      }
    });
  }

  private addNoCacheToUrl(url: string) {
    let separator = url.indexOf('?') === -1 ? '?' : '&';
    let returnUrl = url + separator + 'noCache=' + new Date().getTime();

    return returnUrl;
  }
}
