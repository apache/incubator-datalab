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

import { Component, OnInit, ViewChild } from '@angular/core';
import { EnvironmentStatusModel } from './environment-status.model';
import { HealthStatusService, BackupService, UserResourceService, UserAccessKeyService } from '../core/services';
import { FileUtils, HTTP_STATUS_CODES } from '../core/util';

@Component({
  moduleId: module.id,
  selector: 'health-status',
  templateUrl: 'health-status.component.html',
  styleUrls: ['./health-status.component.scss']
})
export class HealthStatusComponent implements OnInit {
  environmentsHealthStatuses: Array<EnvironmentStatusModel>;
  healthStatus: string;
  billingEnabled: boolean;
  isAdmin: boolean;
  anyEnvInProgress: boolean = false;
  notebookInProgress: boolean = false;
  usersList: Array<string> = [];
  uploadKey: boolean = true;

  private readonly CHECK_ACCESS_KEY_TIMEOUT: number = 20000;
  private clear = undefined;
  @ViewChild('backupDialog') backupDialog;
  @ViewChild('manageEnvDialog') manageEnvironmentDialog;
  @ViewChild('keyUploadModal') keyUploadDialog;
  @ViewChild('preloaderModal') preloaderDialog;

  constructor(
    private healthStatusService: HealthStatusService,
    private backupService: BackupService,
    private userResourceService: UserResourceService,
    private userAccessKeyService: UserAccessKeyService
  ) {}

  ngOnInit(): void {
    this.buildGrid();
  }

  buildGrid(): void {
    this.healthStatusService.getEnvironmentStatuses().subscribe(result => {
      this.environmentsHealthStatuses = this.loadHealthStatusList(result);
    });
  }

  loadHealthStatusList(healthStatusList): Array<EnvironmentStatusModel> {
    this.healthStatus = healthStatusList.status;
    this.billingEnabled = healthStatusList.billingEnabled;
    this.isAdmin = healthStatusList.admin;

    this.checkUserAccessKey();
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
 
  openManageEnvironmentDialog() {
    this.getActiveUsersList().subscribe(usersList => {
      this.manageEnvironmentDialog.open({ isFooter: false }, usersList);
    });
  }

  manageEnvironment($event) {
    this.healthStatusService
      .manageEnvironment($event.action, $event.user)
      .subscribe(res => {
          this.getActiveUsersList().subscribe(usersList => {
              this.manageEnvironmentDialog.usersList = usersList;
              this.buildGrid();
            });
        },
      (error) => {
        this.manageEnvironmentDialog.errorMessage = JSON.parse(error.message).message;
      });
  }

  public generateUserKey($event) {
    this.userAccessKeyService.generateAccessKey().subscribe(
      data => {
        FileUtils.downloadFile(data);
        this.buildGrid();
      });
  }

  public checkUserAccessKey() {
    this.userAccessKeyService.checkUserAccessKey()
      .subscribe(
        response => this.processAccessKeyStatus(response.status),
        error => this.processAccessKeyStatus(error.status));
  }

  private processAccessKeyStatus(status: number) {
    if (status === HTTP_STATUS_CODES.NOT_FOUND) {
      this.healthStatus === 'error' && this.keyUploadDialog.open({ isFooter: false });
      this.uploadKey = false;
    } else if (status === HTTP_STATUS_CODES.ACCEPTED) {
      this.preloaderDialog.open({ isHeader: false, isFooter: false });

      setTimeout(() => this.buildGrid(), this.CHECK_ACCESS_KEY_TIMEOUT);
    } else if (status === HTTP_STATUS_CODES.OK) {
      this.preloaderDialog.close();
      this.keyUploadDialog.close();
      this.uploadKey = true;
    }
  }

  createBackup($event) {
    this.backupService.createBackup($event).subscribe(result => {
      this.getBackupStatus(result);
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
        el.computational_resources.some(elem => elem.status === 'creating' || elem.status === 'starting' || elem.status === 'configuring')
    });
  }

  isNotebookInProgress(data): boolean {
    return data.exploratory.some(el => el.status === 'creating');
  }

  private getBackupStatus(result) {
    const uuid = result.text();
    this.backupService.getBackupStatus(uuid).subscribe(status => {
      if (!this.creatingBackup) clearInterval(this.clear);
    }, error => clearInterval(this.clear));
  }

  get creatingBackup(): boolean {
    return this.backupService.inProgress;
  }
}
