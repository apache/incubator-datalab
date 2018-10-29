/***************************************************************************

Copyright (c) 2017, EPAM SYSTEMS INC

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
import {  CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { ApplicationSecurityService, AuthorizationGuard } from '.';

import 'rxjs/add/operator/toPromise';

@Injectable()
export class CheckParamsGuard implements CanActivate {
  result: any;

  constructor(
    private applicationSecurityService: ApplicationSecurityService,
    private _authGuard: AuthorizationGuard
  ) {}

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    return this._authGuard
      .canActivate(next, state)
      .toPromise()
      .then((auth: boolean) => {
        const search = document.URL.split('?')[1];

        if (search && this.checkParamsCoincidence(search)) {
          this.result = search.split('&').reduce(function(prev, curr) {
            const params = curr.split('=');
            prev[decodeURIComponent(params[0])] = decodeURIComponent(params[1]);
            return prev;
          }, {});

          return this.applicationSecurityService
            .redirectParams(this.result)
            .toPromise();
        }

        return Promise.resolve(!!auth);
      });
  }

  private checkParamsCoincidence(search): boolean {
    return ['code', 'state', 'error', 'error_description'].some(el =>
      search.indexOf(el)
    );
  }
}
