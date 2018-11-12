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

import { Component, ViewEncapsulation, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material';
import { Subscription } from 'rxjs/Subscription';

import { ApplicationSecurityService, HealthStatusService, AppRoutingService, UserAccessKeyService } from '../../core/services';
import { GeneralEnvironmentStatus } from '../../health-status/environment-status.model';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import { HTTP_STATUS_CODES } from '../../core/util';

import { NotificationDialogComponent } from '../modal-dialog/notification-dialog';

@Component({
  selector: 'dlab-navbar',
  templateUrl: 'navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class NavbarComponent implements OnInit, OnDestroy {
  readonly PROVIDER = DICTIONARY.cloud_provider;
  private readonly CHECK_ACCESS_KEY_TIMEOUT: number = 20000;

  currentUserName: string;
  isLoggedIn: boolean;
  quotesLimit: number;

  healthStatus: GeneralEnvironmentStatus;
  subscriptions: Subscription = new Subscription();

  @ViewChild('keyUploadModal') keyUploadDialog;
  @ViewChild('preloaderModal') preloaderDialog;

  constructor(
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService,
    private healthStatusService: HealthStatusService,
    private userAccessKeyService: UserAccessKeyService,
    private dialog: MatDialog
  ) { }

  ngOnInit() {
    this.applicationSecurityService.loggedInStatus.subscribe(response => {
      this.isLoggedIn = response;

      if (this.isLoggedIn) {
        this.subscriptions.add(this.healthStatusService.statusData.subscribe(result => {
          console.log('úpdate healthStatus');
          this.healthStatus = result;
          this.checkQuoteUsed(this.healthStatus);
        }));
        this.subscriptions.add(this.userAccessKeyService.accessKeyEmitter.subscribe(response => {
          console.log('úpdate accessKeyEmitter', response);
          response && this.processAccessKeyStatus(response.status);
        }));
      }
    });

    this.quotesLimit = 70;
    this.currentUserName = this.getUserName();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
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

  public generateUserKey($event) {
    console.log('generate key', $event);
  }

  public checkCreationProgress($event) {
    console.log('checkCreationProgress key', $event);
  }

  private checkQuoteUsed(params) {
    if (params.billingQuoteUsed >= this.quotesLimit && !this.applicationSecurityService.getBillingQuoteUsed()) {
      if (this.dialog.openDialogs.length > 0 || this.dialog.openDialogs.length > 0) return;
      this.emitQuotes();
    }
  }

  private processAccessKeyStatus(status: number) {
    if (status === HTTP_STATUS_CODES.NOT_FOUND) {
      this.keyUploadDialog.open({ isFooter: false });
    } else if (status === HTTP_STATUS_CODES.ACCEPTED) {
      !this.preloaderDialog.bindDialog.isOpened && this.preloaderDialog.open({ isHeader: false, isFooter: false });
      setTimeout(() => this.userAccessKeyService.initialUserAccessKeyCheck(), this.CHECK_ACCESS_KEY_TIMEOUT);

      this.userAccessKeyService.setActionOnKeyAccept();
    } else if (status === HTTP_STATUS_CODES.OK) {
      this.preloaderDialog.close();
      this.keyUploadDialog.close();
    }
  }
}
