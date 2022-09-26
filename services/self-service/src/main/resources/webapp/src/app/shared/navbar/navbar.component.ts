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

import { Component, ViewEncapsulation, OnInit, OnDestroy } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Subscription, timer } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { RouterOutlet } from '@angular/router';

import {
  ApplicationSecurityService,
  HealthStatusService,
  AppRoutingService,
  SchedulerService,
  StorageService
} from '../../core/services';
import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { NotificationDialogComponent } from '../modal-dialog/notification-dialog';
import {
  trigger,
  animate,
  transition,
  style,
  query,
  group,
} from '@angular/animations';
import {skip, take} from 'rxjs/operators';
import {ProgressBarService} from '../../core/services/progress-bar.service';
import {Sidebar_Names_Config, UserInfo} from './navbar.config';
import { RoutingListConfig } from '../../core/configs/routing-list.config';

interface Quota {
  projectQuotas: {};
  totalQuotaUsed: number;
}
@Component({
  selector: 'datalab-navbar',
  templateUrl: 'navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  animations: [trigger('fadeAnimation', [
    transition('* <=> *', [
      query(':enter,:leave', [
        style({ overflow: 'hidden' })
      ], { optional: true }),
      group([
        query(':leave', [
          animate('0s',
            style({
              opacity: 0,
            })
          )
        ], { optional: true }),
        query(':enter', [
          style({
            opacity: 0,
          }),
          animate('.3s .25s ease-in-out',
            style({
              opacity: 1
            })
          )
        ], { optional: true }),
      ])
    ])

  ])],
  encapsulation: ViewEncapsulation.None
})
export class NavbarComponent implements OnInit, OnDestroy {

  private readonly CHECK_ACTIVE_SCHEDULE_TIMEOUT: number = 300000;
  private readonly CHECK_ACTIVE_SCHEDULE_PERIOD: number = 15;
  readonly routerList: typeof RoutingListConfig = RoutingListConfig;

  currentUserName: string;
  quotesLimit: number = 70;
  isLoggedIn: boolean = false;
  metadata: any;
  isExpanded: boolean = true;
  healthStatus: GeneralEnvironmentStatus;
  subscriptions: Subscription = new Subscription();
  sideBarNames: typeof Sidebar_Names_Config = Sidebar_Names_Config;
  userData!: UserInfo;
  commitMaxLength: number = 22;

  constructor(
    public toastr: ToastrService,
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService,
    private healthStatusService: HealthStatusService,
    private schedulerService: SchedulerService,
    private storage: StorageService,
    private dialog: MatDialog,
    public progressBarService: ProgressBarService,
  ) { }

  ngOnInit() {
    this.applicationSecurityService.loggedInStatus.subscribe(response => {
      this.subscriptions.unsubscribe();
      this.subscriptions.closed = false;

      this.isLoggedIn = response;
      if (this.isLoggedIn) {
        this.subscriptions.add(this.healthStatusService.statusData.pipe(skip(1)).subscribe(result => {
          this.healthStatus = result;
          result.status && this.checkQuoteUsed();
          result.status && !result.projectAssigned && !result.admin && this.checkAssignment(this.healthStatus);
        }));
        this.subscriptions.add(timer(0, this.CHECK_ACTIVE_SCHEDULE_TIMEOUT).subscribe(() => this.refreshSchedulerData()));
        this.currentUserName = this.getUserName();
        this.checkVersionData();
      }
    });
    this.userData = this.getUserData();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  public getRouterOutletState(routerOutlet: RouterOutlet) {
    return routerOutlet.isActivated ? routerOutlet.activatedRoute : '';
  }

  getUserName(): string {
    return this.storage.getUserName() || '';
  }

  logout_btnClick(): void {
    this.healthStatusService.resetStatusValue();
    this.applicationSecurityService.logout().subscribe(
      (response: any) => {
        const redirect_parameter = response.headers.get('Location');
        redirect_parameter ? this.appRoutingService.redirectToUrl(redirect_parameter) : this.appRoutingService.redirectToLoginPage();
        this.subscriptions.unsubscribe();
      },
      error => console.error(error));
  }

  collapse() {
    this.isExpanded = !this.isExpanded;
  }

  public emitQuotes(alert, total_quota?, exideedProjects?, informProjects?): void {
    const dialogRef: MatDialogRef<NotificationDialogComponent> = this.dialog.open(NotificationDialogComponent, {
      data: { template: this.selectAlert(alert, total_quota, exideedProjects, informProjects), type: 'message' },
      width: '550px'
    });
    dialogRef.afterClosed().subscribe(() => {
      this.storage.setBillingQuoteUsed('informed');
    });
  }

  private checkQuoteUsed(): void {
    if (!this.storage.getBillingQuoteUsed( )) {
      this.healthStatusService.getQuotaStatus().pipe(take(1)).subscribe((params: Quota) => {
        let checkQuotaAlert = '';
        const exceedProjects = [], informProjects = [];
        Object.keys(params.projectQuotas).forEach(key => {
          if (params.projectQuotas[key] > this.quotesLimit && params.projectQuotas[key] < 100) {
            informProjects.push(key);
          } else if (params.projectQuotas[key] >= 100) {
            exceedProjects.push(key);
          }
        });

        if (informProjects.length > 0 && exceedProjects.length === 0) checkQuotaAlert = 'project_quota';
        if (params.totalQuotaUsed >= this.quotesLimit && params.totalQuotaUsed < 100) checkQuotaAlert = 'total_quota';
        if (exceedProjects.length > 0 && informProjects.length === 0) checkQuotaAlert = 'project_exceed';
        if (informProjects.length > 0 && exceedProjects.length > 0) checkQuotaAlert = 'project_inform_and_exceed';
        if (params.totalQuotaUsed >= this.quotesLimit && params.totalQuotaUsed < 100 && exceedProjects.length > 0) {
          checkQuotaAlert = 'total_quota_and_project_exceed';
        }
        if (params.totalQuotaUsed >= this.quotesLimit && params.totalQuotaUsed < 100
          && informProjects.length > 0 && exceedProjects.length > 0) checkQuotaAlert = 'total_quota_and_project_inform_and_exceed';


        if (Number(params.totalQuotaUsed) >= 100) checkQuotaAlert = 'total_exceed';

        if (checkQuotaAlert === '') {
          this.storage.setBillingQuoteUsed('informed');
        } else {
          this.storage.setBillingQuoteUsed('');
        }

        if (this.dialog.openDialogs.length > 0 || this.dialog.openDialogs.length > 0) return;
        checkQuotaAlert && this.emitQuotes(checkQuotaAlert, params.totalQuotaUsed, exceedProjects, informProjects);
      });
    }
  }

  private getUserData(): UserInfo {
    const token = localStorage.getItem('JWT_TOKEN');
    const [_, tokenInfo] = token.split('.');
    const {name, email} = JSON.parse(atob(tokenInfo));

    return {
      name: name || 'Jhon Doe',
      email: email || 'Email not found'
    };
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
    memo.notebook = memo.notebook.reduce((acc, v) => {
      const existedProject = acc.find(el => el.project === v.project);
      if (existedProject) {
        existedProject.notebook.push(v.exploratory_name);
      } else {
        acc.push({project: v.project, notebook: [v.exploratory_name]});
      }
      return acc;
    }, []);
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

  private selectAlert(type: string, total_quota?: number, exideedProjects?: string[], informProjects?: string[]): string {
    const alerts = {
      total_quota_and_project_inform_and_exceed: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          DataLab cloud infrastructure usage quota has been used for <span class="strong">${total_quota}%</span>.
          Once quota is depleted all your analytical environment will be stopped.<br /><br />
          Quota associated with project(s) <span class="strong">${exideedProjects.join(', ')}</span> has been exceeded. All your analytical environment will be stopped.<br /><br />
          Quota associated with project(s) <span class="strong">${informProjects.join(', ')}</span> has been used over <span class="strong">${this.quotesLimit}%</span>.
          If quota is depleted all your analytical environment will be stopped.<br /><br />
          To proceed working with environment you'll have to request increase of quota from DataLab administrator. `,

      total_quota_and_project_exceed: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          DataLab cloud infrastructure usage quota has been used for <span class="strong">${total_quota}%</span>.
          Once quota is depleted all your analytical environment will be stopped.<br /><br />
          Quota associated with project(s) <span class="strong">${exideedProjects.join(', ')}</span> has been exceeded. All your analytical environment will be stopped.<br /><br />
          To proceed working with environment you'll have to request increase of quota from DataLab administrator. `,

      project_inform_and_exceed: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          DataLab cloud infrastructure usage quota associated with project(s) <span class="strong">${exideedProjects.join(', ')}</span> has been exceeded. All your analytical environment will be stopped.<br /><br />
          Quota associated with project(s) <span class="strong">${informProjects.join(', ')}</span> has been used over <span class="strong">${this.quotesLimit}%</span>.
          If quota is depleted all your analytical environment will be stopped.<br /><br />
          To proceed working with environment, request increase of project quota from DataLab administrator.`,
      project_exceed: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          DataLab cloud infrastructure usage quota associated with project(s) <span class="strong">${exideedProjects.join(', ')}</span> has been exceeded.
          All your analytical environment will be stopped.<br /><br />
          To proceed working with environment,
          request increase of project(s) quota from DataLab administrator.`,
      total_exceed: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          DataLab cloud infrastructure usage quota has been exceeded.
          All your analytical environment will be stopped.<br /><br />
          To proceed working with environment,
          request increase application quota from DataLab administrator.`,
      project_quota: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          Cloud infrastructure usage quota associated with project(s) <span class="strong">${informProjects.join(', ')}</span> has been used over <span class="strong">${this.quotesLimit}%</span>.
          Once quota is depleted all your analytical environment will be stopped.<br /><br />
          To proceed working with environment you'll have to request increase of project(s) quota from DataLab administrator.`,
      total_quota: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          DataLab cloud infrastructure usage quota has been used for <span class="strong">${total_quota}%</span>.
          Once quota is depleted all your analytical environment will be stopped.<br /><br />
          To proceed working with environment you'll have to request increase of total quota from DataLab administrator. `,
      permissions: `Dear <span class="strong">${this.currentUserName}</span>,<br /><br />
          Currently, you are not assigned to any project. To start working with the environment
          request permission from DataLab administrator.`
    };

    return alerts[type];
  }
}
