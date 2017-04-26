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

import { Component } from '@angular/core';

import { LoginModel } from './login.model';
import { AppRoutingService, HealthStatusService, ApplicationSecurityService } from '../core/services';

@Component({
  moduleId: module.id,
  selector: 'dlab-login',
  templateUrl: 'login.component.html',
  styleUrls: ['./login.component.css'],
  providers: [ApplicationSecurityService]
})

export class LoginComponent {
  model = new LoginModel('', '');
  error = '';
  loading = false;
  userPattern = '\\w+.*\\w+';

  constructor(
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService : AppRoutingService,
    private healthStatusService : HealthStatusService
  ) { }

  ngOnInit() {
    this.applicationSecurityService.isLoggedIn()
      .subscribe(result => {
        this.checkHealthStatusAndRedirect(result);
    });
  }

  login_btnClick() {
    this.error = '';
    this.loading = true;

    this.applicationSecurityService
      .login(this.model)
      .subscribe((result) => {
        if (result) {
          this.checkHealthStatusAndRedirect(result);
          return true;
        }

        return false;
      }, (err) => {
          this.error = err.text();
          this.loading = false;
        });

    return false;
  }

  checkHealthStatusAndRedirect(isLoggedIn) {
   if(isLoggedIn)
     this.healthStatusService.isHealthStatusOk()
      .subscribe(isHealthStatusOk => {
        if(isLoggedIn && !isHealthStatusOk) {
          this.appRoutingService.redirectToHealthStatusPage();
        } else {
          this.appRoutingService.redirectToHomePage();
        }
      });
  }
}

