/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

import { Component } from '@angular/core';

import { LoginModel } from "./loginModel";
import {AppRoutingService} from "../routing/appRouting.service";
import {ApplicationSecurityService} from "../services/applicationSecurity.service";

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
  userPattern = "/S+";
  //
  // Override
  //

  constructor(private applicationSecurityService: ApplicationSecurityService, private appRoutingService : AppRoutingService) {}

  ngOnInit() {
    this.applicationSecurityService.isLoggedIn().subscribe(result => {
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
      .login(this.model.username, this.model.password)
      .subscribe((result) => {
        if (result) {
          this.appRoutingService.redirectToHomePage();
          return true;
        }

        return false;
      }, (err) => {
          if(err.status == 401){
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
