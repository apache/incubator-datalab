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

import { Component, ViewEncapsulation, OnInit, OnDestroy } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material';
import { ISubscription } from 'rxjs/Subscription';

import { ApplicationSecurityService, HealthStatusService } from '../../core/services';
import { NotificationDialogComponent } from '../modal-dialog/notification-dialog';
import { AppRoutingService } from '../../core/services';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import { GeneralEnvironmentStatus } from '../../health-status/environment-status.model';

@Component({
  selector: 'dlab-navbar',
  templateUrl: 'navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class NavbarComponent implements OnInit, OnDestroy {
  readonly PROVIDER = DICTIONARY.cloud_provider;
  currentUserName: string;
  healthStatus: GeneralEnvironmentStatus;
  isLoggedIn: boolean;
  subscription: ISubscription;
  quotesLimit: number;

  constructor(
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService,
    private healthStatusService: HealthStatusService,
    private dialog: MatDialog
  ) {
    this.healthStatusService.statusData.subscribe(
      result => {
        this.healthStatus = result;
        console.log('úpdate')
      }
    )
  }

  ngOnInit() {
    this.applicationSecurityService.loggedInStatus.subscribe(res => {
      this.isLoggedIn = res;

      if (this.isLoggedIn) {
        this.healthStatusService.reloadInitialStatusData();
        this.healthStatusService.statusData.subscribe(result => {
          this.healthStatus = result;
          this.checkQuoteUsed(this.healthStatus);
          console.log('úpdate');
        });
      } 
    });

    this.quotesLimit = 70;
    this.currentUserName = this.getUserName();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
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

  public emitQuotes() {
    const dialogRef: MatDialogRef<NotificationDialogComponent> = this.dialog.open(NotificationDialogComponent, {
      data: `NOTE: Currently used billing quote is ${ this.healthStatus.billingQuoteUsed }%`,
      width: '550px'
    });
    dialogRef.afterClosed().subscribe(() => {
      this.applicationSecurityService.setBillingQuoteUsed('informed');
    });
  }

  private checkQuoteUsed(params) {
    if (params.billingQuoteUsed >= this.quotesLimit && !this.applicationSecurityService.getBillingQuoteUsed()) {
      if (this.dialog.openDialogs.length > 0 || this.dialog.openDialogs.length > 0) return;
      this.emitQuotes();
    }
  }
}
