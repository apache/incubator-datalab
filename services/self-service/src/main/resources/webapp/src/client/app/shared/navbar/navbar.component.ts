/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

import { Component, ViewEncapsulation, OnInit} from '@angular/core';
import { ApplicationSecurityService } from "../../services/applicationSecurity.service";
import { AppRoutingService } from "../../routing/appRouting.service";

@Component({
  moduleId: module.id,
  selector: 'sd-navbar',
  templateUrl: 'navbar.component.html',
  styleUrls: ['./navbar.component.css'],
  encapsulation : ViewEncapsulation.None
})

export class NavbarComponent implements OnInit{
  currentUserName: String;

  constructor(
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService : AppRoutingService
  ) {}

  ngOnInit() {
    this.currentUserName = this.getUserName();
  }

  getUserName() {
    return this.applicationSecurityService.getCurrentUserName() || '';
  }

  logout_btnClick() {
    this.applicationSecurityService.logout().subscribe(
      () => this.appRoutingService.redirectToLoginPage(),
      error => console.log(error),
      () => this.appRoutingService.redirectToLoginPage());
  }
}
