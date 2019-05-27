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

import { ProjectDataService } from './project-data.service';
import { NotificationDialogComponent } from '../../shared/modal-dialog/notification-dialog';

export interface Project {
  name: string;
  endpoints: string[];
  tag: string;
  groups: string[];
}

@Component({
  selector: 'dlab-project',
  templateUrl: './project.component.html',
  styleUrls: ['./project.component.scss']
})
export class ProjectComponent implements OnInit, OnDestroy {
  projects: Project[] = [];
  private subscriptions: Subscription = new Subscription();

  constructor(
    public dialog: MatDialog,
    private projectDataService: ProjectDataService
  ) { }

  ngOnInit() {
    this.subscriptions.add(this.projectDataService._projects.subscribe(
      value => this.projects = value));
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  createProject() {
    console.log('create');

    if (this.projects.length)
      this.dialog.open(EditProjectComponent, { data: { action: 'create', item: null }, panelClass: 'modal-xl-s' })
        .afterClosed().subscribe(() => {
          console.log('Create project');
        });
  }

  public editProject($event) {
    this.dialog.open(EditProjectComponent, { data: { action: 'edit', item: $event }, panelClass: 'modal-xl-s' })
      .afterClosed().subscribe(() => {
        console.log('Update project');
      });
  }

  public deleteProject($event) {
    $event.name = $event.project_name;
    this.dialog.open(NotificationDialogComponent, { data: { type: 'confirmation', item: $event }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(() => {
        console.log('Delete project');
      });
  }
}

@Component({
  selector: 'edit-project',
  template: `
    <div id="dialog-box" class="edit-form">
      <div class="dialog-header">
        <h4 class="modal-title">
          <span *ngIf="data?.action === 'create'">Create new project</span>
          <span *ngIf="data?.action === 'edit'">Edit {{ data.item.project_name }}</span>
        </h4>
        <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
      </div>
      <div mat-dialog-content class="content project-form">
        <project-form [item]="data.item"></project-form>
      </div>
    </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 0; font-size: 14px; font-weight: 400; margin: 0; overflow: hidden; }
    .edit-form { height: 380px; }
  `]
})
export class EditProjectComponent {
  constructor(
    public dialogRef: MatDialogRef<EditProjectComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}