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
import { AuthorizationGuard, AppRoutingService, HealthStatusService } from './';

import { DICTIONARY } from '../../../dictionary/global.dictionary';

import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/toPromise';
import { Observer } from 'rxjs/Observer';

@Injectable()
export class CloudProviderGuard implements CanActivate {
    constructor(
      private _authGuard: AuthorizationGuard,
      private _routing: AppRoutingService,
      private _healthStatus: HealthStatusService
    ) { }

    canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<any> {
        return this._authGuard.canActivate(next, state).toPromise().then((auth: boolean) => {
            if (!auth) Promise.resolve(false);

            this._healthStatus.isBillingEnabled()
              .subscribe(status => {
                const data = status.json()
                console.log(data);

                if (auth && !data.billingEnabled) {
                  this._routing.redirectToHomePage();
                  return Promise.resolve(false);
                }

                return Promise.resolve(true);
              });
        });
    }
}
