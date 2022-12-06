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

import { Component, OnInit, OnDestroy, Inject, ViewChild } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ProjectDataService } from './project-data.service';
import {HealthStatusService, ProjectService } from '../../core/services';
import { NotificationDialogComponent } from '../../shared/modal-dialog/notification-dialog';
import { ProjectListComponent } from './project-list/project-list.component';
import { EnvironmentsDataService } from '../management/management-data.service';
import { Project } from './project.model';


@Component({
  selector: 'datalab-project',
  templateUrl: './project.component.html'
})

export class ProjectComponent implements OnInit, OnDestroy {
  projectList: Project[] = [];
  healthStatus: any;
  activeFiltering: boolean = false;
  resources: any = [];
  noPermissionMessage: string = 'You are not assigned to any project.';

  private subscriptions: Subscription = new Subscription();

  @ViewChild(ProjectListComponent) list: ProjectListComponent;

  constructor(
    public dialog: MatDialog,
    public toastr: ToastrService,
    private projectService: ProjectService,
    private projectDataService: ProjectDataService,
    private healthStatusService: HealthStatusService,
    private environmentsDataService: EnvironmentsDataService
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.subscriptions.add(this.projectDataService._projects
      .subscribe((value: Project[]) => {
        if (value) this.projectList = value;
      })
    );
    this.refreshGrid();
    this.getResources();
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  private getResources() {
    this.environmentsDataService.getEnvironmentDataDirect()
      .subscribe((data: any) => {
        this.resources = data;
      });
  }

  refreshGrid() {
    this.projectDataService.updateProjects();
    this.activeFiltering = false;
  }

  createProject() {
    if (this.projectList.length)
      this.dialog.open(EditProjectComponent, { data: { action: 'create', item: null }, panelClass: 'modal-xl-s' })
        .afterClosed().subscribe(() => {
          this.getEnvironmentHealthStatus();
        });
  }

  public toggleFiltering(): void {
    this.activeFiltering = !this.activeFiltering;
    this.activeFiltering ? this.list.showActiveInstances() : this.projectDataService.updateProjects();
  }

  public editProject($event) {
    this.dialog.open(EditProjectComponent, { data: { action: 'edit', item: $event }, panelClass: 'modal-xl-s' })
      .afterClosed().subscribe(() => {
        this.refreshGrid();
      });
  }

  public toggleStatus($event) {
    const data = {
      'project_name': $event.project.name,
      endpoint: $event.endpoint.map(endpoint => endpoint.name),
      'edge_status': $event.endpoint.map(endpoint => endpoint.status)[0]
    };
    this.toggleStatusRequest(data, $event.action, $event.oneEdge);
  }

  private toggleStatusRequest(data, action, isOnlyOneEdge?) {
    if ( action === 'terminate') {
      const projectsResources = this.resources
        .filter(resource => resource.project === data.project_name && resource.resource_type !== 'edge node');

      const activeProjectsResources = projectsResources?.length ? projectsResources
        .filter(expl => expl.status !== 'terminated' && expl.status !== 'terminating' && expl.status !== 'failed') : [];

      const termResources = data.endpoint.reduce((res, endp) => {
        res.push(...activeProjectsResources.filter(resource => resource.endpoint === endp));
        return res;
      }, []).map(resource => resource.resource_name);

      if (termResources.length === 0 && !isOnlyOneEdge) {
        this.edgeNodeAction(data, action);
      } else {
        this.dialog.open(NotificationDialogComponent, { data: {
            type: 'terminateNode',
            item: {action: data, resources: termResources}
          }, panelClass: 'modal-sm' })
          .afterClosed().subscribe(result => {
          result && this.edgeNodeAction(data, action);
        });
      }
    } else {
      this.edgeNodeAction(data, action);
    }
  }

  private edgeNodeAction(data, action) {
    this.projectService.toggleProjectStatus(data, action)
      .subscribe(
        () => {
          this.refreshGrid();
          this.toastr.success(`Edge node ${this.toEndpointAction(action)} is in progress!`, 'Processing!');
        },
        error => {
          this.toastr.error(error.message, 'Oops!');
        }
      );
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => this.healthStatus = result);
  }

  private toEndpointAction(action) {
    if (action === 'start') return 'starting';
    else if (action === 'stop') return 'stopping';
    else if (action === 'terminate') return 'terminating';
    else return action;
  }
}

@Component({
  selector: 'edit-project',
  template: `
    <div id="dialog-box" class="edit-form">
      <div class="dialog-header">
        <h4 class="modal-title">
          <span *ngIf="data?.action === 'create'">Create new project</span>
          <span *ngIf="data?.action === 'edit'">Edit {{ data.item.name }}</span>
        </h4>
        <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
      </div>
      <div mat-dialog-content class="content project-form">
        <project-form [item]="data.item" (update)="dialogRef.close(true)"></project-form>
      </div>
    </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 0; font-size: 14px; font-weight: 400; margin: 0; overflow: hidden; }
    .edit-form { height: 410px; overflow: hidden; }
  `]
})
export class EditProjectComponent {
  constructor(
    public dialogRef: MatDialogRef<EditProjectComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
