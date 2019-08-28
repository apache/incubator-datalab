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

import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ProjectDataService } from './project-data.service';
import { HealthStatusService, ProjectService } from '../../core/services';
import { NotificationDialogComponent } from '../../shared/modal-dialog/notification-dialog';

export interface Project {
  name: string;
  endpoints: string[];
  tag: string;
  groups: string[];
}

@Component({
  selector: 'dlab-project',
  templateUrl: './project.component.html'
})
export class ProjectComponent implements OnInit, OnDestroy {
  projectList: Project[] = [];
  healthStatus: any;

  private subscriptions: Subscription = new Subscription();

  constructor(
    public dialog: MatDialog,
    public toastr: ToastrService,
    private projectService: ProjectService,
    private projectDataService: ProjectDataService,
    private healthStatusService: HealthStatusService
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.subscriptions.add(this.projectDataService._projects.subscribe(
      (value: Project[]) => {
        if (value) this.projectList = value;
      }));
    this.refreshGrid();
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  refreshGrid() {
    this.projectDataService.updateProjects();
  }

  createProject() {
    if (this.projectList.length)
      this.dialog.open(EditProjectComponent, { data: { action: 'create', item: null }, panelClass: 'modal-xl-s' })
        .afterClosed().subscribe(() => {
          console.log('Create project');
          this.getEnvironmentHealthStatus();
        });
  }

  public editProject($event) {
    this.dialog.open(EditProjectComponent, { data: { action: 'edit', item: $event }, panelClass: 'modal-xl-s' })
      .afterClosed().subscribe(() => {
        this.refreshGrid();
      });
  }

  public deleteProject($event) {
    this.dialog.open(NotificationDialogComponent, { data: { type: 'confirmation', item: $event }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => {
        result && this.projectService.deleteProject($event.name).subscribe(() => {
          this.refreshGrid();
        });
      });
  }

  public toggleStatus($event) {
    const data = { 'project_name': $event.project.name };

    if ($event.action === 'stop') {
      this.dialog.open(NotificationDialogComponent, { data: { type: 'confirmation', item: $event.project, action: 'stopped' }, panelClass: 'modal-sm' })
        .afterClosed().subscribe(result => {
          result && this.toggleStatusRequest(data, $event.action);
        }, error => this.toastr.error(error.message, 'Oops!'));
    } else {
      this.toggleStatusRequest(data, $event.action);
    }
  }

  private toggleStatusRequest(data, action) {
    this.projectService.toggleProjectStatus(data, action).subscribe(res => this.refreshGrid());
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => this.healthStatus = result);
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