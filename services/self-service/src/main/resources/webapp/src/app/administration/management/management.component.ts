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

import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import {
  HealthStatusService,
  ManageEnvironmentsService,
  BackupService,
  UserResourceService,
  StorageService
} from '../../core/services';

import {ActionsType, ActionTypeOptions, EnvironmentModel, GeneralEnvironmentStatus, ModalData, ModalDataType} from './management.model';
import { HTTP_STATUS_CODES } from '../../core/util';
import { BackupDilogComponent } from './backup-dilog/backup-dilog.component';
import { SsnMonitorComponent } from './ssn-monitor/ssn-monitor.component';
import { ManageEnvironmentComponent } from './manage-environment/manage-environment-dilog.component';
import { EndpointsComponent } from './endpoints/endpoints.component';
import { ExploratoryModel } from '../../resources/resources-grid/resources-grid.model';

import { EnvironmentsDataService } from './management-data.service';
import { ProjectService } from '../../core/services';
import { ConfirmationDialogComponent, ConfirmationDialogType } from '../../shared/modal-dialog/confirmation-dialog';
import { ManagementGridComponent, ReconfirmationDialogComponent } from './management-grid/management-grid.component';
import { FolderTreeComponent } from '../../resources/bucket-browser/folder-tree/folder-tree.component';
import { StatusTypes } from '../../core/models';
import {AmiCreateDialogComponent} from '../../resources/exploratory/ami-create-dialog';

@Component({
  selector: 'environments-management',
  templateUrl: './management.component.html',
  styleUrls: ['./management.component.scss']
})
export class ManagementComponent implements OnInit {
  public user: string = '';
  public healthStatus: GeneralEnvironmentStatus;
  // public anyEnvInProgress: boolean = false;
  public dialogRef: any;
  public selected: any[] = [];
  public isActionsOpen: boolean = false;
  public selectedRunning: boolean;
  public selectedStopped: boolean;
  public resourceStatus = StatusTypes;

  @ViewChild(ManagementGridComponent, { static: true }) managementGrid;

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

  public refreshGrid() {
    this.buildGrid();
  }

  public manageEnvironmentAction($event) {
    this.manageEnvironmentsService
      .environmentManagement(
        $event.environment.user,
        $event.action,
        $event.environment.project,
        $event.environment.type === 'edge node' ? 'edge' : $event.environment.name,
        $event.resource ? $event.resource.computational_name : null
      )
      .subscribe(
        () => this.buildGrid(),
        error => this.toastr.error('Environment management failed!', 'Oops!')
      );
  }

  // showBackupDialog() {
  //   this.dialog.open(BackupDilogComponent, { panelClass: 'modal-sm' });
  // }

  showEndpointsDialog() {
    this.dialog.open(EndpointsComponent, { panelClass: 'modal-xl-s' })
      .afterClosed().subscribe(result => result && this.buildGrid());
  }

  openManageEnvironmentDialog() {
    this.projectService.getProjectsList().subscribe(projectsList => {
      this.getTotalBudgetData()
        .subscribe(
          total => {
            this.dialogRef = this.dialog.open(ManageEnvironmentComponent, { data: { projectsList, total }, panelClass: 'modal-xl-s' });
            this.dialogRef.afterClosed().subscribe(result => result && this.setBudgetLimits(result));
          },
          () => this.toastr.error('Failed users list loading!', 'Oops!')
        );
    });
  }

  // openSsnMonitorDialog() {
  //   this.dialog.open(SsnMonitorComponent, { panelClass: 'modal-lg' });
  // }
  //
  // isEnvironmentsInProgress(exploratory): boolean {
  //   return exploratory.some(item => {
  //     return item.exploratory.some(el => el.status === 'creating' || el.status === 'starting' ||
  //       el.resources.some(elem => elem.status === 'creating' || elem.status === 'starting' || elem.status === 'configuring'));
  //   });
  // }

  setBudgetLimits($event) {
    if ($event.projects.length) {
      this.projectService.updateProjectsBudget($event.projects)
        .subscribe(
          (result: any) => {
            if ($event.isTotalChanged) {
              this.healthStatusService.updateTotalBudgetData($event.total).subscribe((res: any) => {
                result.status === HTTP_STATUS_CODES.OK
                  && res.status === HTTP_STATUS_CODES.NO_CONTENT
                  && this.toastr.success('Budget limits updated!', 'Success!');
                this.buildGrid();
              });
            } else {
              result.status === HTTP_STATUS_CODES.OK && this.toastr.success('Budget limits updated!', 'Success!');
              this.buildGrid();
            }
          },
          error => this.toastr.error(error.message, 'Oops!'));
    } else {
      this.healthStatusService.updateTotalBudgetData($event.total)
        .subscribe((res: any) => {
          res.status === HTTP_STATUS_CODES.NO_CONTENT
            && this.toastr.success('Budget limits updated!', 'Success!');
          this.buildGrid();
        });
    }
  }


  // manageEnvironment(event: { action: string, project: any }) {
  //   if (event.action === 'stop')
  //     this.projectService.stopProjectAction(event.project.project_name)
  //       .subscribe(() => this.handleSuccessAction(event.action), error => this.toastr.error(error.message, 'Oops!'));
  //
  //   if (event.action === 'terminate')
  //     this.projectService.deleteProject(event.project.project_name)
  //       .subscribe(() => this.handleSuccessAction(event.action), error => this.toastr.error(error.message, 'Oops!'));
  // }

  // handleSuccessAction(action) {
  //   this.toastr.success(`Action ${action} is processing!`, 'Processing!');
  //   this.projectService.getProjectsManagingList().subscribe(data => {
  //     this.dialogRef.componentInstance.data.projectsList = data;
  //     this.dialogRef.componentInstance.setProjectsControl();
  //   });
  //   this.buildGrid();
  // }
  //
  // get creatingBackup(): boolean {
  //   return this.backupService.inProgress;
  // }

  // private getExploratoryList() {
  //   this.userResourceService.getUserProvisionedResources()
  //     .subscribe((result) => this.anyEnvInProgress = this.isEnvironmentsInProgress(
  //       ExploratoryModel.loadEnvironments(result)));
  // }

  private getEnvironmentHealthStatus() {
    this.healthStatusService
      .getEnvironmentStatuses()
      .subscribe((status: GeneralEnvironmentStatus) => {
        this.healthStatus = status;
        // this.getExploratoryList();
      });
  }

  // private getActiveUsersList() {
  //   return this.healthStatusService.getActiveUsers();
  // }

  private getTotalBudgetData() {
    return this.healthStatusService.getTotalBudgetData();
  }

  public selectedList($event) {
    this.selected = $event;
    if (this.selected.length === 0) {
      this.isActionsOpen = false;
    }

    this.selectedRunning = this.selected.every(item => item.status === this.resourceStatus.running);
    this.selectedStopped = this.selected.every(item => item.status === this.resourceStatus.stopped);
  }

  public toogleActions() {
    this.isActionsOpen = !this.isActionsOpen;
  }

  toggleResourceAction({ environment, action, resource = null}): void {
    this.openDialog(environment, action, resource);
  }

  private clearSelection() {
    this.selected = [];
    this.isActionsOpen = false;
    if (this.managementGrid.selected && this.managementGrid.selected.length !== 0) {
      this.managementGrid.selected.forEach(item => item.isSelected = false);
      this.managementGrid.selected = [];
    }
  }

  public resourceAction(action: ActionTypeOptions) {
    this.toggleResourceAction({ environment: this.selected, action: action });
  }

  private openDialog(environment, action, resource) {
    let config: ModalData = {
      type: '',
      action
    };
    const observer = {
      next: (res) => {}
    };

    if (resource) {
      config.resource_name = resource.computational_name;
      config.user = environment.user;
      config.type = ModalDataType.cluster;
      observer.next = (res) => {
        res && this.manageEnvironmentAction({ action, environment, resource });
      };
    } else {
      const notebooks = this.selected.length ? this.selected : [environment];
      config = this.getModalConfig(config, action, notebooks);
      observer.next = (res) => {
        if (res) {
          notebooks.forEach((env) => {
            if (action === 'create image') {
              env['isAdmin'] = true;
              env['userName'] = env.user;
              this.dialog.open(AmiCreateDialogComponent, { data: env, panelClass: 'modal-sm' })
                .afterClosed().subscribe(
                () => this.buildGrid(),
                error => console.log(error)
              );
            } else {
              this.getNotebookAction(env, action);
            }
          });
        }
        this.clearSelection();
      };
      this.isActionsOpen = false;
    }

    // TODO if action run and recreate are restored, uncomment this piece of code

    // if(action === 'run' || action === 'recreate') {
    //   this.getHealthAction(action);
    // } else {
    //   this.dialog.open(ReconfirmationDialogComponent, {
    //     data: config,
    //     width: '550px',
    //     panelClass: 'error-modalbox'
    //   }).afterClosed().subscribe(observer);
    // }

    // TODO if action run and recreate are restored remove this piece of code
      this.dialog.open(ReconfirmationDialogComponent, {
        data: config,
        width: '550px',
        panelClass: 'error-modalbox'
      }).afterClosed().subscribe(observer);
  }

  private getModalConfig(config: ModalData, action: ActionsType, notebooks: EnvironmentModel[]): ModalData {
    config = {...config, type: ModalDataType.notebook};
    if (action === ActionsType.stop) {
      notebooks = notebooks.filter(note => note.status !== this.resourceStatus.stopped);
      return {...config, notebooks};
    }
    if (action === ActionsType.start) {
      notebooks = notebooks.filter(note => note.status === this.resourceStatus.stopped);
      return {...config, notebooks};
    }
    return {...config, notebooks};
  }

  private getNotebookAction(env: EnvironmentModel, action: ActionTypeOptions): void {
    this.manageEnvironmentsService.environmentManagement(env.user, action, env.project, env.name)
      .subscribe(
        () => this.buildGrid(),
        error => console.log(error)
      );
  }

  private getHealthAction(action: ActionsType) {
    let nodeAction: ActionsType.run | ActionsType.recreate;
    const observer = {
      next: () => {
        this.buildGrid();
        this.toastr.success(`Edge Node ${nodeAction} is processing!`, 'Processing!');
      },
      error: () => this.toastr.error(`Edge Node ${nodeAction} failed!`, 'Oops!')
    };
    if (action === ActionsType.run) {
      nodeAction = ActionsType.run;
      this.healthStatusService.runEdgeNode().subscribe(observer);
    }
    if (action === ActionsType.recreate) {
      nodeAction = ActionsType.recreate;
      this.healthStatusService.recreateEdgeNode().subscribe(observer);
    }
  }
}


