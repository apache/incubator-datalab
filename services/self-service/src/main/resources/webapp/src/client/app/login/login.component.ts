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

import { LoginModel } from "./loginModel";
import {AppRoutingService} from "../routing/appRouting.service";
import {ApplicationSecurityService} from "../services/applicationSecurity.service";
import HTTP_STATUS_CODES from 'http-status-enum';

@Component({
  moduleId: module.id,
  selector: 'sd-login',
  templateUrl: 'login.component.html',
  styleUrls: ['./login.component.css'],
  providers: [ApplicationSecurityService]
})

export class LoginComponent {
  model = new LoginModel ('', '');
  error = '';
  loading = false;
  userPattern = "\\w+.*\\w+";
  //
  // Override
  //

  constructor(private applicationSecurityService: ApplicationSecurityService,
              private appRoutingService : AppRoutingService) {}

  ngOnInit() {
    this.applicationSecurityService.isLoggedIn()
      .subscribe(result => {
      if (result)
        this.appRoutingService.redirectToHomePage();
    });
  }

  //
  // Handlers
  //

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
      }, (err) => {
          if(err.status == HTTP_STATUS_CODES.UNAUTHORIZED){
            this.error = 'Username or password is incorrect.';
            this.loading = false;
          }
          else {
            this.error = 'System failure. Please contact administrator.';
            this.loading = false;
          }
        });

    return false;
  }
}
