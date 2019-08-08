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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';

import { LoginModel } from './login.model';
import { AppRoutingService, HealthStatusService, ApplicationSecurityService } from '../core/services';
import { HTTP_STATUS_CODES } from '../core/util';
import { DICTIONARY } from '../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-login',
  templateUrl: 'login.component.html',
  styleUrls: ['./login.component.css']
})

export class LoginComponent implements OnInit, OnDestroy {
  readonly DICTIONARY = DICTIONARY;
  model = new LoginModel('', '');
  error: string;
  loading = false;
  userPattern = '\\w+.*\\w+';

  subscriptions: Subscription;

  constructor(
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService,
    private healthStatusService: HealthStatusService
  ) {
    this.subscriptions = this.applicationSecurityService.emitter$
      .subscribe(message => this.error = message);
  }

  ngOnInit() { }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  login_btnClick() {
    this.error = '';
    this.loading = true;

    this.applicationSecurityService
      .login(this.model)
      .subscribe((result) => {
        if (result) {
          this.appRoutingService.redirectToHomePage();
          return true;
        }

        return false;
      }, error => {
        if (DICTIONARY.cloud_provider === 'azure' && error && error.status === HTTP_STATUS_CODES.FORBIDDEN) {
          window.location.href = error.headers.get('Location');
        } else {
          this.error = error.message;
          this.loading = false;
        }
      });

    return false;
  }

  loginWithKeyClock() {
    this.applicationSecurityService.locationCheck().subscribe(location => window.location.href = location.headers.get('Location'));
  }

  checkHealthStatusAndRedirect(isLoggedIn) {
    this.healthStatusService.isHealthStatusOk()
      .subscribe(isHealthStatusOk => {
        if (isLoggedIn && !isHealthStatusOk) {
          this.appRoutingService.redirectToHealthStatusPage();
        } else {
          this.appRoutingService.redirectToHomePage();
        }
      });
  }
}
