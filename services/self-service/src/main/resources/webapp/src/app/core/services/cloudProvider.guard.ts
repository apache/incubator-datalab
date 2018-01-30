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
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthorizationGuard, AppRoutingService } from './';

import { DICTIONARY } from '../../../dictionary/global.dictionary';
import 'rxjs/add/operator/toPromise';

@Injectable()
export class CloudProviderGuard implements CanActivate {
    constructor(
      private _authGuard: AuthorizationGuard,
      private _routing: AppRoutingService
    ) { }

    canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        return this._authGuard.canActivate(next, state).toPromise().then((auth: boolean) => {
          if (!auth || DICTIONARY.cloud_provider === 'gcp') {
            auth && this._routing.redirectToHomePage();
            return Promise.resolve(false);
          } else {
            return Promise.resolve(true);
          }
        });
    }
}
