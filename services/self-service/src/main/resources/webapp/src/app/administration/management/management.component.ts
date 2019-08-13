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

import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material';
import { ToastrService } from 'ngx-toastr';

import {
  HealthStatusService,
  ManageEnvironmentsService,
  BackupService,
  UserResourceService,
  StorageService
} from '../../core/services';

import { EnvironmentModel, GeneralEnvironmentStatus } from './management.model';
import { HTTP_STATUS_CODES } from '../../core/util';
import { BackupDilogComponent } from './backup-dilog/backup-dilog.component';
import { SsnMonitorComponent } from './ssn-monitor/ssn-monitor.component';
import { ManageEnvironmentComponent } from './manage-environment/manage-environment-dilog.component';
import { EndpointsComponent } from './endpoints/endpoints.component';
import { ExploratoryModel } from '../../resources/resources-grid/resources-grid.model';

import { EnvironmentsDataService } from './management-data.service';
import { ProjectService } from '../../core/services';

@Component({
  selector: 'environments-management',
  templateUrl: './management.component.html',
  styleUrls: ['./management.component.scss']
})
export class ManagementComponent implements OnInit {
  public user: string = '';
  public healthStatus: GeneralEnvironmentStatus;
  public anyEnvInProgress: boolean = false;
  public dialogRef: any;

  constructor(
    public toastr: ToastrService,
    public dialog: MatDialog,
    private healthStatusService: HealthStatusService,
    private backupService: BackupService,
    private manageEnvironmentsService: ManageEnvironmentsService,
    private userResourceService: UserResourceService,
    private storageService: StorageService,
    private environmentsDataService: EnvironmentsDataService,
    private projectService: ProjectService
  ) { }

  ngOnInit() {
    this.buildGrid();
    this.user = this.storageService.getUserName();
  }

  public buildGrid() {
    this.getEnvironmentHealthStatus();
    this.environmentsDataService.updateEnvironmentData();
  }

  public manageEnvironmentAction($event) {
    this.manageEnvironmentsService
      .environmentManagement(
        $event.environment.user,
        $event.action,
        $event.environment.type === 'edge node' ? 'edge' : $event.environment.name,
        $event.resource ? $event.resource.computational_name : null
      ).subscribe(
        () => this.buildGrid(),
        error => this.toastr.error('Environment management failed!', 'Oops!'));
  }

  showBackupDialog() {
    this.dialog.open(BackupDilogComponent, { panelClass: 'modal-sm' });
  }

  showEndpointsDialog() {
    this.dialog.open(EndpointsComponent, { panelClass: 'modal-xl-s' })
      .afterClosed().subscribe(result => result && this.buildGrid());
  }

  openManageEnvironmentDialog() {
    this.projectService.getProjectsList().subscribe(projectsList => {
      this.getTotalBudgetData().subscribe(total => {
        this.dialogRef = this.dialog.open(ManageEnvironmentComponent, { data: { projectsList, total }, panelClass: 'modal-xl-s' });
        this.dialogRef.componentInstance.manageEnv.subscribe((data) => this.manageEnvironment(data));
        this.dialogRef.afterClosed().subscribe(result => result && this.setBudgetLimits(result));
      }, () => this.toastr.error('Failed users list loading!', 'Oops!'));
    });
  }

  openSsnMonitorDialog() {
    this.dialog.open(SsnMonitorComponent, { panelClass: 'modal-lg' });
  }

  isEnvironmentsInProgress(exploratory): boolean {
    return exploratory.some(item => {
      return item.exploratory.some(el => el.status === 'creating' || el.status === 'starting' ||
        el.resources.some(elem => elem.status === 'creating' || elem.status === 'starting' || elem.status === 'configuring'));
    });
  }

  setBudgetLimits($event) {
    this.projectService.updateProjectsBudget($event.projects).subscribe((result: any) => {
      this.healthStatusService.updateTotalBudgetData($event.total).subscribe((res: any) => {
        result.status === HTTP_STATUS_CODES.OK
          && res.status === HTTP_STATUS_CODES.NO_CONTENT
          && this.toastr.success('Budget limits updated!', 'Success!');
        this.buildGrid();
      });
    }, error => this.toastr.error(error.message, 'Oops!'));
  }

  manageEnvironment(event: { action: string, project: any }) {
    if (event.action === 'stop')
      this.projectService.toggleProjectStatus(event.project, event.action)
        .subscribe(() => this.handleSuccessAction(event.action), error => this.toastr.error(error.message, 'Oops!'));

    if (event.action === 'terminate')
      this.projectService.deleteProject(event.project.project_name)
        .subscribe(() => this.handleSuccessAction(event.action), error => this.toastr.error(error.message, 'Oops!'));
  }

  handleSuccessAction(action) {
    this.toastr.success(`Action ${action} is processing!`, 'Processing!');
    this.projectService.getProjectsList().subscribe(data => {
      this.dialogRef.componentInstance.data.projectsList = data
      this.dialogRef.componentInstance.setProjectsControl();
    });
    this.buildGrid()
  }

  get creatingBackup(): boolean {
    return this.backupService.inProgress;
  }

  private getExploratoryList() {
    this.userResourceService.getUserProvisionedResources()
      .subscribe((result) => this.anyEnvInProgress = this.isEnvironmentsInProgress(
        ExploratoryModel.loadEnvironments(result)));
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService
      .getEnvironmentStatuses()
      .subscribe((status: GeneralEnvironmentStatus) => {
        this.healthStatus = status;
        this.getExploratoryList();
      });
  }

  private getActiveUsersList() {
    return this.healthStatusService.getActiveUsers();
  }

  private getTotalBudgetData() {
    return this.healthStatusService.getTotalBudgetData();
  }
}
