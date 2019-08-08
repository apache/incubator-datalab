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

import { Component, ViewEncapsulation, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material';
import { Subscription, timer, interval } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ApplicationSecurityService, HealthStatusService, AppRoutingService, SchedulerService, StorageService } from '../../core/services';
import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import { FileUtils } from '../../core/util';
import { NotificationDialogComponent } from '../modal-dialog/notification-dialog';

@Component({
  selector: 'dlab-navbar',
  templateUrl: 'navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class NavbarComponent implements OnInit, OnDestroy {
  readonly PROVIDER = DICTIONARY.cloud_provider;

  private readonly CHECK_ACTIVE_SCHEDULE_TIMEOUT: number = 55000;
  private readonly CHECK_ACTIVE_SCHEDULE_PERIOD: number = 15;

  currentUserName: string;
  quotesLimit: number = 70;
  isLoggedIn: boolean = false;
  metadata: any;
  isExpanded: boolean = true;

  healthStatus: GeneralEnvironmentStatus;
  subscriptions: Subscription = new Subscription();

  constructor(
    public toastr: ToastrService,
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService,
    private healthStatusService: HealthStatusService,
    private schedulerService: SchedulerService,
    private storage: StorageService,
    private dialog: MatDialog
  ) { }

  ngOnInit() {
    this.applicationSecurityService.loggedInStatus.subscribe(response => {
      this.subscriptions.unsubscribe();
      this.subscriptions.closed = false;

      this.isLoggedIn = response;
      if (this.isLoggedIn) {
        this.subscriptions.add(this.healthStatusService.statusData.subscribe(result => {
          this.healthStatus = result;
          result.status && this.checkQuoteUsed(this.healthStatus);
          result.status && !result.projectAssigned && this.checkAssignment(this.healthStatus);
        }));
        this.subscriptions.add(timer(0, this.CHECK_ACTIVE_SCHEDULE_TIMEOUT).subscribe(() => this.refreshSchedulerData()));
        this.currentUserName = this.getUserName();
        this.checkVersionData();
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  getUserName(): string {
    return this.storage.getUserName() || '';
  }

  logout_btnClick(): void {
    this.healthStatusService.resetStatusValue();
    this.applicationSecurityService.logout().subscribe(
      () => {
        this.appRoutingService.redirectToLoginPage();
        this.subscriptions.unsubscribe();
      },
      error => console.error(error));
  }

  collapse() {
    this.isExpanded = !this.isExpanded;
  }

  public emitQuotes(alert, user_quota?, total_quota?): void {
    const dialogRef: MatDialogRef<NotificationDialogComponent> = this.dialog.open(NotificationDialogComponent, {
      data: { template: this.selectAlert(alert, user_quota, total_quota), type: 'message' },
      width: '550px'
    });
    dialogRef.afterClosed().subscribe(() => {
      this.storage.setBillingQuoteUsed('informed');
    });
  }

  private checkQuoteUsed(params): void {
    if (!this.storage.getBillingQuoteUsed() && params) {
      let checkQuotaAlert = '';

      if (params.billingUserQuoteUsed >= this.quotesLimit && params.billingUserQuoteUsed < 100) checkQuotaAlert = 'user_quota';
      if (params.billingQuoteUsed >= this.quotesLimit && params.billingQuoteUsed < 100) checkQuotaAlert = 'total_quota';
      if (Number(params.billingUserQuoteUsed) >= 100) checkQuotaAlert = 'user_exceed';
      if (Number(params.billingQuoteUsed) >= 100) checkQuotaAlert = 'total_exceed';

      if (this.dialog.openDialogs.length > 0 || this.dialog.openDialogs.length > 0) return;
      checkQuotaAlert && this.emitQuotes(checkQuotaAlert, params.billingUserQuoteUsed, params.billingQuoteUsed);
    }
  }

  private checkAssignment(params): void {
    if (this.dialog.openDialogs.length > 0) return;
    this.emitQuotes('permissions');
  }

  private refreshSchedulerData(): void {
    this.schedulerService.getActiveSchcedulersData(this.CHECK_ACTIVE_SCHEDULE_PERIOD).subscribe((list: Array<any>) => {
      if (list.length) {
        if (this.dialog.openDialogs.length > 0) return;
        const filteredData = this.groupSchedulerData(list);
        this.dialog.open(NotificationDialogComponent, {
          data: { template: filteredData, type: 'list' },
          width: '550px'
        });
      }
    });
  }

  private groupSchedulerData(sheduler_data) {
    const memo = { notebook: [], cluster: [] };
    sheduler_data.map(item => !item.computational_name ? memo.notebook.push(item) : memo.cluster.push(item));
    memo.cluster = memo.cluster.filter(el => !memo.notebook.some(elm => el.exploratory_name === elm.exploratory_name));
    return memo;
  }

  public checkVersionData(): void {
    this.healthStatusService.getAppMetaData().subscribe(
      result => this.metadata = result || null,
      error => {
        console.log('Metadata loading failed!');
        // this.toastr.error('Metadata loading failed!', 'Oops!');
      });
  }

  private selectAlert(type: string, user_quota?: number, total_quota?: number): string {
    const alerts = {
      user_exceed: `Dear <b>${this.currentUserName}</b>,<br />
          DLab cloud infrastructure usage quota associated with your user has been exceeded.
          All your analytical environment will be stopped. To proceed working with environment,
          request increase of user quota from DLab administrator.`,
      total_exceed: `Dear <b>${this.currentUserName}</b>,<br />
          DLab cloud infrastructure usage quota has been exceeded.
          All your analytical environment will be stopped. To proceed working with environment,
          request increase application quota from DLab administrator.`,
      user_quota: `Dear <b>${this.currentUserName}</b>,<br />
          Cloud infrastructure usage quota associated with your user has been used for <b>${user_quota}%</b>.
          Once quota is depleted all your analytical environment will be stopped.
          To proceed working with environment you'll have to request increase of user quota from DLab administrator.`,
      total_quota: `Dear <b>${this.currentUserName}</b>,<br />
          DLab cloud infrastructure usage quota has been used for <b>${total_quota}%</b>.
          Once quota is depleted all your analytical environment will be stopped.
          To proceed working with environment you'll have to request increase of user quota from DLab administrator. `,
      permissions: `Dear <b>${this.currentUserName}</b>,<br />
          Currently, you are not assigned to any project. To start working with the environment
          request permission from DLab administrator.`
    };

    return alerts[type];
  }
}
