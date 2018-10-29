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

import { Component, ViewEncapsulation, Input, OnInit } from '@angular/core';

import { ApplicationSecurityService } from '../../core/services';
import { AppRoutingService } from '../../core/services';
import { DICTIONARY } from '../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-navbar',
  templateUrl: 'navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class NavbarComponent implements OnInit {
  readonly PROVIDER = DICTIONARY.cloud_provider;
  currentUserName: string;

  @Input() healthStatus: string;
  @Input() billingEnabled: boolean;
  @Input() admin: boolean;

  constructor(
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService
  ) { }

  ngOnInit() {
    this.currentUserName = this.getUserName();
  }

  getUserName() {
    return this.applicationSecurityService.getCurrentUserName() || '';
  }

  logout_btnClick() {
    this.applicationSecurityService.logout()
      .subscribe(
      () => this.appRoutingService.redirectToLoginPage(),
      error => console.log(error),
      () => this.appRoutingService.redirectToLoginPage());
  }
}
