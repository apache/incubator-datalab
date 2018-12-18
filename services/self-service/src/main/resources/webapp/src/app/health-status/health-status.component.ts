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

import { Component, OnInit, ViewChild, ViewContainerRef, OnDestroy } from '@angular/core';
import { ToastsManager } from 'ng2-toastr';
import { ISubscription } from 'rxjs/Subscription';

import { EnvironmentStatusModel } from './environment-status.model';
import { HealthStatusService, BackupService, UserResourceService, UserAccessKeyService, RolesGroupsService } from '../core/services';
import { HTTP_STATUS_CODES } from '../core/util';

@Component({
  selector: 'health-status',
  templateUrl: 'health-status.component.html',
  styleUrls: ['./health-status.component.scss']
})
export class HealthStatusComponent implements OnInit, OnDestroy {
  private clear = undefined;
  private subscription: ISubscription;

  environmentsHealthStatuses: Array<EnvironmentStatusModel>;
  healthStatus: string;
  anyEnvInProgress: boolean = false;
  notebookInProgress: boolean = false;
  usersList: Array<string> = [];
  uploadKey: boolean = true;

  @ViewChild('backupDialog') backupDialog;
  @ViewChild('manageEnvDialog') manageEnvironmentDialog;
  @ViewChild('keyUploadModal') keyUploadDialog;
  @ViewChild('preloaderModal') preloaderDialog;
  @ViewChild('ssnMonitor') ssnMonitorDialog;
  @ViewChild('rolesGroupsModal') rolesGroupsDialog;

  constructor(
    private healthStatusService: HealthStatusService,
    private backupService: BackupService,
    private userResourceService: UserResourceService,
    private userAccessKeyService: UserAccessKeyService,
    private rolesService: RolesGroupsService,
    public toastr: ToastsManager,
    public vcr: ViewContainerRef
  ) {
    this.toastr.setRootViewContainerRef(vcr);
  }

  ngOnInit(): void {
    this.buildGrid();
    this.subscription = this.userAccessKeyService.accessKeyEmitter.subscribe(result => {
      this.uploadKey = result ? result.status === 200 : false;
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  buildGrid(): void {
    this.healthStatusService.getEnvironmentStatuses().subscribe(result => {
      this.environmentsHealthStatuses = this.loadHealthStatusList(result);
    });
  }

  loadHealthStatusList(healthStatusList): Array<EnvironmentStatusModel> {
    this.healthStatus = healthStatusList;
    this.userAccessKeyService.initialUserAccessKeyCheck();
    this.getExploratoryList();

    if (healthStatusList.list_resources)
      return healthStatusList.list_resources.map(value => {
        return new EnvironmentStatusModel(value.type, value.resource_id, value.status);
      });
  }

  showBackupDialog() {
    if (!this.backupDialog.isOpened)
      this.backupDialog.open({ isFooter: false });
  }

  getActiveUsersList() {
    return this.healthStatusService.getActiveUsers();
  }

  getTotalBudgetData() {
    return this.healthStatusService.getTotalBudgetData();
  }

  openManageEnvironmentDialog() {
    this.getActiveUsersList().subscribe(usersList => {
      this.getTotalBudgetData().subscribe(total => this.manageEnvironmentDialog.open({ isFooter: false }, usersList, total));
    },
    () => this.toastr.error('Failed users list loading!', 'Oops!', { toastLife: 5000 }));
  }

  openSsnMonitorDialog() {
    this.healthStatusService.getSsnMonitorData()
      .subscribe(data => this.ssnMonitorDialog.open({ isHeader: false, isFooter: false }, data));
  }

  openManageRolesDialog() {
    this.rolesService.getGroupsData().subscribe(group => {
        this.rolesService.getRolesData().subscribe(
          roles => this.rolesGroupsDialog.open({ isFooter: false }, group, roles),
          error => this.toastr.error(error.message, 'Oops!', { toastLife: 5000 }));
      },
      error => this.toastr.error(error.message, 'Oops!', { toastLife: 5000 }));
  }

  getGroupsData() {
    this.rolesService.getGroupsData().subscribe(
      list => this.rolesGroupsDialog.updateGroupData(list),
      error => this.toastr.error(error.message, 'Oops!', { toastLife: 5000 }));
  }

  manageEnvironment(event: {action: string, user: string}) {
    this.healthStatusService
      .manageEnvironment(event.action, event.user)
      .subscribe(res => {
          this.getActiveUsersList().subscribe(usersList => {
            this.manageEnvironmentDialog.usersList = usersList;
            this.toastr.success(`Action ${ event.action } is processing!`, 'Processing!', { toastLife: 5000 });
            this.buildGrid();
          });
        },
      error => this.toastr.error(error.message, 'Oops!', { toastLife: 5000 }));
  }

  setBudgetLimits($event) {
    this.healthStatusService.updateUsersBudget($event.users).subscribe((result: any) => {
      this.healthStatusService.updateTotalBudgetData($event.total).subscribe((res: any) => {
        result.status === HTTP_STATUS_CODES.OK
        && res.status === HTTP_STATUS_CODES.NO_CONTENT
        && this.toastr.success('Budget limits updated!', 'Success!', { toastLife: 5000 });
        this.buildGrid();
      });
    }, error => this.toastr.error(error.message, 'Oops!', { toastLife: 5000 }));
  }

  manageRolesGroups($event) {
    switch ($event.action) {
      case 'create':
        this.rolesService.setupNewGroup($event.value).subscribe(res => {
          this.toastr.success('Group creation success!', 'Created!', { toastLife: 5000 });
          this.getGroupsData();
        }, () => this.toastr.error('Group creation failed!', 'Oops!', { toastLife: 5000 }));
        break;
      case 'update':
        if ($event.type === 'roles') {
          this.rolesService.setupRolesForGroup($event.value).subscribe(res => {
            this.toastr.success('Roles list successfully updated!', 'Success!', { toastLife: 5000 });
            this.getGroupsData();
          }, () => this.toastr.error('Failed roles list updating!', 'Oops!', { toastLife: 5000 }));
        } else if ($event.type === 'users') {
          this.rolesService.setupUsersForGroup($event.value).subscribe(res => {
            this.toastr.success('Users list successfully updated!', 'Success!', { toastLife: 5000 });
            this.getGroupsData();
          }, () => this.toastr.error('Failed users list updating!', 'Oops!', { toastLife: 5000 }));
        }
        break;
      case 'delete':
        if ($event.type === 'users') {
          this.rolesService.removeUsersForGroup($event.value).subscribe(res => {
            this.toastr.success('Users was successfully deleted!', 'Success!', { toastLife: 5000 });
            this.getGroupsData();
          }, () => this.toastr.error('Failed users deleting!', 'Oops!', { toastLife: 5000 }));
        } else if ($event.type === 'group') {
          console.log('delete group');
          this.rolesService.removeGroupById($event.value).subscribe(res => {
            this.toastr.success('Group was successfully deleted!', 'Success!', { toastLife: 5000 });
            this.getGroupsData();
          }, () => this.toastr.error('Failed group deleting!', 'Oops!', { toastLife: 5000 }));
        }
        break;
      default:
    }
  }

  createBackup($event) {
    this.backupService.createBackup($event).subscribe(result => {
      this.getBackupStatus(result);
      this.toastr.success('Backup configuration is processing!', 'Processing!', { toastLife: 5000 });
      this.clear = window.setInterval(() => this.getBackupStatus(result), 3000);
    });
  }

  getExploratoryList() {
    this.userResourceService.getUserProvisionedResources()
      .subscribe((result) => {
        this.anyEnvInProgress = this.isEnvironmentsInProgress(result);
        this.notebookInProgress = this.isNotebookInProgress(result);
      });
  }

  isEnvironmentsInProgress(data): boolean {
    return data.exploratory.some(el => {
      return el.status === 'creating' || el.status === 'starting' ||
        el.computational_resources.some(elem => elem.status === 'creating' || elem.status === 'starting' || elem.status === 'configuring');
    });
  }

  isNotebookInProgress(data): boolean {
    return data.exploratory.some(el => el.status === 'creating');
  }

  private getBackupStatus(result) {
    const uuid = result.text();
    this.backupService.getBackupStatus(uuid)
        .subscribe((backupStatus: any) => {
        if (!this.creatingBackup) {
          backupStatus.status === 'FAILED'
          ? this.toastr.error('Backup configuration failed!', 'Oops!', { toastLife: 5000 })
          : this.toastr.success('Backup configuration completed!', 'Success!', { toastLife: 5000 });
          clearInterval(this.clear);
        }
    }, error => {
      clearInterval(this.clear);
      this.toastr.error('Backup configuration failed!', 'Oops!', { toastLife: 5000 });
    });
  }

  get creatingBackup(): boolean {
    return this.backupService.inProgress;
  }
}
