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
import { Subscription, timer, interval } from 'rxjs';
import { takeWhile } from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';

import { ApplicationSecurityService,
  HealthStatusService,
  AppRoutingService,
  UserAccessKeyService,
  SchedulerService,
  StorageService} from '../../core/services';
import { GeneralEnvironmentStatus } from '../../health-status/environment-status.model';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import { HTTP_STATUS_CODES, FileUtils } from '../../core/util';
import { NotificationDialogComponent } from '../modal-dialog/notification-dialog';

@Component({
  selector: 'dlab-navbar',
  templateUrl: 'navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class NavbarComponent implements OnInit, OnDestroy {
  readonly PROVIDER = DICTIONARY.cloud_provider;

  private alive: boolean = false;
  private readonly CHECK_ACCESS_KEY_TIMEOUT: number = 30000;
  private readonly CHECK_ACTIVE_SCHEDULE_TIMEOUT: number = 55000;
  private readonly CHECK_ACTIVE_SCHEDULE_PERIOD: number = 15;

  currentUserName: string;
  quotesLimit: number = 70;
  isLoggedIn: boolean = false;

  healthStatus: GeneralEnvironmentStatus;
  subscriptions: Subscription = new Subscription();

  @ViewChild('keyUploadModal') keyUploadDialog;
  @ViewChild('preloaderModal') preloaderDialog;

  constructor(
    public toastr: ToastrService,
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService,
    private healthStatusService: HealthStatusService,
    private userAccessKeyService: UserAccessKeyService,
    private schedulerService: SchedulerService,
    private storage: StorageService,
    private dialog: MatDialog
  ) { }

  ngOnInit() {
    this.applicationSecurityService.loggedInStatus.subscribe(response => {
      this.subscriptions.unsubscribe();
      this.subscriptions.closed = false;
      this.alive = false;

      this.isLoggedIn = response;

      if (this.isLoggedIn) {
        this.subscriptions.add(this.healthStatusService.statusData.subscribe(result => {
          this.healthStatus = result;
          result.status && this.checkQuoteUsed(this.healthStatus);
        }));
        this.subscriptions.add(this.userAccessKeyService.accessKeyEmitter.subscribe(result => {
          result && this.processAccessKeyStatus(result.status);
        }));
        this.subscriptions.add(timer(0, this.CHECK_ACTIVE_SCHEDULE_TIMEOUT).subscribe(() => this.refreshSchedulerData()));
        this.currentUserName = this.getUserName();
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.alive = false;
  }

  getUserName(): string {
    return this.storage.getUserName() || '';
  }

  logout_btnClick(): void {
    this.healthStatusService.resetStatusValue();
    this.userAccessKeyService.resetUserAccessKey();
    this.applicationSecurityService.logout().subscribe(
      () => {
        this.appRoutingService.redirectToLoginPage();
        this.subscriptions.unsubscribe();
      },
      error => console.error(error));
  }

  public emitQuotes(alert, user_quota?, total_quota?): void {
    const dialogRef: MatDialogRef<NotificationDialogComponent> = this.dialog.open(NotificationDialogComponent, {
      data: { template: this.selectQuotesAlert(alert, user_quota, total_quota), type: 'message' },
      width: '550px'
    });
    dialogRef.afterClosed().subscribe(() => {
      this.storage.setBillingQuoteUsed('informed');
    });
  }

  public generateUserKey($event): void {
    console.log('generate key', $event);
    this.userAccessKeyService.generateAccessKey().subscribe(
      data => {
        FileUtils.downloadFile(data);
        this.userAccessKeyService.initialUserAccessKeyCheck();
      }, error => {
        this.toastr.error(error.message || 'Access key generation failed!', 'Oops!');
      });
  }

  public checkCreationProgress($event): void {
    this.userAccessKeyService.initialUserAccessKeyCheck();
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

  private processAccessKeyStatus(status: number): void {
    if (status === HTTP_STATUS_CODES.NOT_FOUND) {
      this.keyUploadDialog.open({ isFooter: false });
      this.alive = false;
    } else if (status === HTTP_STATUS_CODES.ACCEPTED) {
      !this.preloaderDialog.bindDialog.isOpened && this.preloaderDialog.open({ isHeader: false, isFooter: false });

      if (!this.alive) {
        this.alive = true;
        this.subscriptions.add(
          interval(this.CHECK_ACCESS_KEY_TIMEOUT)
            .pipe(takeWhile(() => this.alive))
            .subscribe(() => this.userAccessKeyService.initialUserAccessKeyCheck()));
      }

    } else if (status === HTTP_STATUS_CODES.OK) {
      this.alive = false;
      this.userAccessKeyService.emitActionOnKeyUploadComplete();
      this.preloaderDialog.close();
      this.keyUploadDialog.close();
    }
  }

  private refreshSchedulerData(): void {
      this.schedulerService.getActiveSchcedulersData(this.CHECK_ACTIVE_SCHEDULE_PERIOD).subscribe((list: Array<any>) => {
        if (list.length) {
          if (this.dialog.openDialogs.length > 0 || this.dialog.openDialogs.length > 0) return;
          const filteredData = this.groupSchedulerData(list);
          const dialogRef: MatDialogRef<NotificationDialogComponent> = this.dialog.open(NotificationDialogComponent, {
            data: { template: filteredData, type: 'list' },
            width: '550px'
          });
        }
    });
  }

  private groupSchedulerData(sheduler_data) {
    const memo = { notebook: [], cluster: [] };
    sheduler_data.map(item =>  !item.computational_name ? memo.notebook.push(item) : memo.cluster.push(item));
    memo.cluster = memo.cluster.filter(el => !memo.notebook.some(elm => el.exploratory_name === elm.exploratory_name));
    return memo;
  }

  private selectQuotesAlert(type: string, user_quota?: number, total_quota?: number): string {
    const alerts = {
      user_exceed: `Dear <b>${ this.currentUserName }</b>,<br />
          DLab cloud infrastructure usage quota associated with your user has been exceeded.
          All your analytical environment will be stopped. To proceed working with environment, 
          request increase of user quota from DLab administrator.`,
      total_exceed: `Dear <b>${ this.currentUserName }</b>,<br />
          DLab cloud infrastructure usage quota has been exceeded.
          All your analytical environment will be stopped. To proceed working with environment, 
          request increase application quota from DLab administrator.`,
      user_quota: `Dear <b>${ this.currentUserName }</b>,<br />
          Cloud infrastructure usage quota associated with your user has been used for <b>${user_quota}%</b>.
          Once quota is depleted all your analytical environment will be stopped. 
          To proceed working with environment you'll have to request increase of user quota from DLab administrator.`,
      total_quota: `Dear <b>${ this.currentUserName }</b>,<br />
          DLab cloud infrastructure usage quota has been used for <b>${total_quota}%</b>.
          Once quota is depleted all your analytical environment will be stopped. 
          To proceed working with environment you'll have to request increase of user quota from DLab administrator. `
    };

    return alerts[type];
  }
}
