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

import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { MatDialog } from '@angular/material';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';

import {
  HealthStatusService,
  ManageEnvironmentsService,
  UserAccessKeyService,
  BackupService,
  UserResourceService,
  RolesGroupsService,
  StorageService
} from '../../core/services';

import { EnvironmentModel, GeneralEnvironmentStatus } from './management.model';
import { HTTP_STATUS_CODES } from '../../core/util';
import { BackupDilogComponent } from './backup-dilog/backup-dilog.component';
import { SsnMonitorComponent } from './ssn-monitor/ssn-monitor.component';
import { ManageEnvironmentComponent } from './manage-environment/manage-environment-dilog.component';
import { EndpointsComponent } from './endpoints/endpoints.component';

@Component({
  selector: 'environments-management',
  templateUrl: './management.component.html',
  styleUrls: ['./management.component.scss']
})
export class ManagementComponent implements OnInit, OnDestroy {
  public user: string = '';
  public healthStatus: GeneralEnvironmentStatus;
  public allEnvironmentData: Array<EnvironmentModel>;
  public uploadKey: boolean = true;
  public anyEnvInProgress: boolean = false;
  public notebookInProgress: boolean = false;

  private subscriptions: Subscription = new Subscription();
  private clear = undefined;

  @ViewChild('backupDialog') backupDialog;
  @ViewChild('manageEnvDialog') manageEnvironmentDialog;
  @ViewChild('keyUploadModal') keyUploadDialog;
  @ViewChild('ssnMonitor') ssnMonitorDialog;
  // @ViewChild('rolesGroupsModal') rolesGroupsDialog;

  constructor(
    public toastr: ToastrService,
    public dialog: MatDialog,
    private healthStatusService: HealthStatusService,
    private backupService: BackupService,
    private manageEnvironmentsService: ManageEnvironmentsService,
    private userAccessKeyService: UserAccessKeyService,
    private userResourceService: UserResourceService,
    private rolesService: RolesGroupsService,
    private storageService: StorageService
  ) { }

  ngOnInit() {
    this.buildGrid();
    this.user = this.storageService.getUserName();
    this.subscriptions.add(this.userAccessKeyService.accessKeyEmitter
      .subscribe(result => this.uploadKey = (result && result.status === 200)));

    this.subscriptions.add(this.userAccessKeyService.keyUploadProccessEmitter.subscribe(response => {
      if (response) this.buildGrid();
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  public buildGrid() {
    this.getEnvironmentHealthStatus();
  }

  public manageEnvironmentAction($event) {
    this.manageEnvironmentsService
      .environmentManagement(
        $event.environment.user,
        $event.action,
        $event.environment.name === 'edge node' ? 'edge' : $event.environment.name,
        $event.resource ? $event.resource.computational_name : null
      ).subscribe(
        () => this.buildGrid(),
        error => this.toastr.error('Environment management failed!', 'Oops!'));
  }

  showBackupDialog() {
    this.dialog.open(BackupDilogComponent, { panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => result && this.createBackup(result));
  }

  showEndpointsDialog() {
    this.dialog.open(EndpointsComponent, { panelClass: 'modal-xl-s' })
      .afterClosed().subscribe(result => result && this.buildGrid());
  }

  getActiveUsersList() {
    return this.healthStatusService.getActiveUsers();
  }

  getTotalBudgetData() {
    return this.healthStatusService.getTotalBudgetData();
  }

  openManageEnvironmentDialog() {
    this.getActiveUsersList().subscribe(usersList => {
      this.getTotalBudgetData().subscribe(
        total => {
          const dialogRef = this.dialog.open(ManageEnvironmentComponent, { data: { usersList, total }, panelClass: 'modal-xl-s'});
          dialogRef.componentInstance.manageEnv.subscribe((data) => this.manageEnvironment(data));
          dialogRef.afterClosed().subscribe(result => result && this.setBudgetLimits(result));
        }, () => this.toastr.error('Failed users list loading!', 'Oops!'));
    });
  }

  openSsnMonitorDialog() {
    this.healthStatusService.getSsnMonitorData().subscribe(
      data => this.dialog.open(SsnMonitorComponent, { data: data, panelClass: 'modal-lg' }),
      () => this.toastr.error('Failed ssn data loading!', 'Oops!'));
  }

  // openManageRolesDialog() {
  //   this.rolesService.getGroupsData().subscribe(group => {
  //     this.rolesService.getRolesData().subscribe(
  //       roles => this.rolesGroupsDialog.open({ isFooter: false }, group, roles),
  //       error => this.toastr.error(error.message, 'Oops!'));
  //   },
  //     error => this.toastr.error(error.message, 'Oops!'));
  // }

  isEnvironmentsInProgress(data): boolean {
    return data.exploratory.some(el => {
      return el.status === 'creating' || el.status === 'starting' ||
        el.computational_resources.some(elem => elem.status === 'creating' || elem.status === 'starting' || elem.status === 'configuring');
    });
  }

  isNotebookInProgress(data): boolean {
    return data.exploratory.some(el => el.status === 'creating');
  }

  createBackup($event) {
    this.backupService.createBackup($event).subscribe(result => {
      this.getBackupStatus(result);
      this.toastr.success('Backup configuration is processing!', 'Processing!');
      this.clear = window.setInterval(() => this.getBackupStatus(result), 3000);
    },
      error => this.toastr.error(error.message, 'Oops!'));
  }

  // manageRolesGroups($event) {
  //   switch ($event.action) {
  //     case 'create':
  //       this.rolesService.setupNewGroup($event.value).subscribe(res => {
  //         this.toastr.success('Group creation success!', 'Created!');
  //         this.getGroupsData();
  //       }, () => this.toastr.error('Group creation failed!', 'Oops!'));
  //       break;
  //     case 'update':
  //       this.rolesService.updateGroup($event.value).subscribe(res => {
  //         this.toastr.success('Group data successfully updated!', 'Success!');
  //         this.getGroupsData();
  //       }, () => this.toastr.error('Failed group data updating!', 'Oops!'));
  //       break;
  //     case 'delete':
  //       if ($event.type === 'users') {
  //         this.rolesService.removeUsersForGroup($event.value).subscribe(res => {
  //           this.toastr.success('Users was successfully deleted!', 'Success!');
  //           this.getGroupsData();
  //         }, () => this.toastr.error('Failed users deleting!', 'Oops!'));
  //       } else if ($event.type === 'group') {
  //         console.log('delete group');
  //         this.rolesService.removeGroupById($event.value).subscribe(res => {
  //           this.toastr.success('Group was successfully deleted!', 'Success!');
  //           this.getGroupsData();
  //         }, () => this.toastr.error('Failed group deleting!', 'Oops!'));
  //       }
  //       break;
  //     default:
  //   }
  // }

  setBudgetLimits($event) {
    this.healthStatusService.updateUsersBudget($event.users).subscribe((result: any) => {
      this.healthStatusService.updateTotalBudgetData($event.total).subscribe((res: any) => {
        result.status === HTTP_STATUS_CODES.OK
          && res.status === HTTP_STATUS_CODES.NO_CONTENT
          && this.toastr.success('Budget limits updated!', 'Success!');
        this.buildGrid();
      });
    }, error => this.toastr.error(error.message, 'Oops!'));
  }

  // getGroupsData() {
  //   this.rolesService.getGroupsData().subscribe(
  //     list => this.rolesGroupsDialog.updateGroupData(list),
  //     error => this.toastr.error(error.message, 'Oops!'));
  // }

  manageEnvironment(event: { action: string, user: string }) {
    this.healthStatusService
      .manageEnvironment(event.action, event.user)
      .subscribe(res => {
        this.getActiveUsersList().subscribe(usersList => {
          this.manageEnvironmentDialog.usersList = usersList;
          this.toastr.success(`Action ${event.action} is processing!`, 'Processing!');
          this.buildGrid();
        });
      },
        error => this.toastr.error(error.message, 'Oops!'));
  }

  private getExploratoryList() {
    this.userResourceService.getUserProvisionedResources()
      .subscribe((result) => {
        this.anyEnvInProgress = this.isEnvironmentsInProgress(result);
        this.notebookInProgress = this.isNotebookInProgress(result);
      });
  }

  private getBackupStatus(result) {
    const uuid = result.body;
    this.backupService.getBackupStatus(uuid)
      .subscribe((backupStatus: any) => {
        if (!this.creatingBackup) {
          backupStatus.status === 'FAILED'
            ? this.toastr.error('Backup configuration failed!', 'Oops!')
            : this.toastr.success('Backup configuration completed!', 'Success!');
          clearInterval(this.clear);
        }
      }, () => {
        clearInterval(this.clear);
        this.toastr.error('Backup configuration failed!', 'Oops!');
      });
  }

  get creatingBackup(): boolean {
    return this.backupService.inProgress;
  }

  private getAllEnvironmentData() {
    this.manageEnvironmentsService
      .getAllEnvironmentData()
      .subscribe((result: Array<EnvironmentModel>) => this.allEnvironmentData = this.loadEnvironmentList(result));
  }

  private loadEnvironmentList(data): Array<EnvironmentModel> {
    if (data)
      return data.map(value => new EnvironmentModel(
        value.resource_name || value.resource_type,
        value.status,
        value.shape,
        value.computational_resources,
        value.user,
        value.public_ip,
        value.resource_type
      ));
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService
      .getEnvironmentStatuses()
      .subscribe((status: GeneralEnvironmentStatus) => {
        this.healthStatus = status;
        this.healthStatus.admin && this.getAllEnvironmentData();
        this.userAccessKeyService.initialUserAccessKeyCheck();
        this.getExploratoryList();
      });
  }
}
