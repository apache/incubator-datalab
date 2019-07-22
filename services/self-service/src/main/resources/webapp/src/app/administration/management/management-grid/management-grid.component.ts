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

import { Component, OnInit, ViewChild, Input, Output, EventEmitter, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ToastrService } from 'ngx-toastr';

import { HealthStatusService } from '../../../core/services';
import { ConfirmationDialogType } from '../../../shared';
import { ConfirmationDialogComponent } from '../../../shared/modal-dialog/confirmation-dialog';
import { EnvironmentsDataService } from '../management-data.service';
import { EnvironmentModel } from '../management.model';

export interface ManageAction {
  action: string;
  environment: string;
  resource: string;
}

@Component({
  selector: 'management-grid',
  templateUrl: 'management-grid.component.html',
  styleUrls: [
    './management-grid.component.scss',
    '../../../resources/resources-grid/resources-grid.component.scss',
    '../../../resources/computational/computational-resources-list/computational-resources-list.component.scss'
  ]
})
export class ManagementGridComponent implements OnInit {
  allEnvironmentData: Array<any>;
  loading: boolean = false;

  @Input() environmentsHealthStatuses: Array<any>;
  @Input() resources: Array<any>;
  @Input() isAdmin: boolean;
  @Input() currentUser: string = '';
  @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();
  @Output() actionToggle: EventEmitter<ManageAction> = new EventEmitter();

  displayedColumns: string[] = ['user', 'type', 'project', 'shape', 'status', 'resources', 'actions'];

  constructor(
    private healthStatusService: HealthStatusService,
    private environmentsDataService: EnvironmentsDataService,
    public toastr: ToastrService,
    public dialog: MatDialog
  ) { }

  ngOnInit() {
    this.environmentsDataService._data.subscribe(data => this.allEnvironmentData = EnvironmentModel.loadEnvironments(data));
  }

  buildGrid(): void {
    this.refreshGrid.emit();
  }

  toggleResourceAction(environment, action: string, resource?) {
    if (resource) {
      const resource_name = resource ? resource.computational_name : environment.name;
      this.dialog.open(ReconfirmationDialogComponent, {
        data: { action, resource_name, user: environment.user },
        width: '550px', panelClass: 'error-modalbox'
      }).afterClosed().subscribe(result => {
        result && this.actionToggle.emit({ action, environment, resource });
      });
    } else {
      const type = (environment.type.toLowerCase() === 'edge node')
        ? ConfirmationDialogType.StopEdgeNode : ConfirmationDialogType.StopExploratory;

      if (action === 'stop') {
        this.dialog.open(ConfirmationDialogComponent, {
          data: { notebook: environment, type: type, manageAction: this.isAdmin }, panelClass: 'modal-md'
        }).afterClosed().subscribe(() => this.buildGrid());
      } else if (action === 'terminate') {
        this.dialog.open(ConfirmationDialogComponent, {
          data: { notebook: environment, type: ConfirmationDialogType.TerminateExploratory, manageAction: this.isAdmin }, panelClass: 'modal-md'
        }).afterClosed().subscribe(() => this.buildGrid());
      } else if (action === 'run') {
        this.healthStatusService.runEdgeNode().subscribe(() => {
          this.buildGrid();
          this.toastr.success('Edge node is starting!', 'Processing!');
        }, () => this.toastr.error('Edge Node running failed!', 'Oops!'));
      } else if (action === 'recreate') {
        this.healthStatusService.recreateEdgeNode().subscribe(() => {
          this.buildGrid();
          this.toastr.success('Edge Node recreation is processing!', 'Processing!');
        }, () => this.toastr.error('Edge Node recreation failed!', 'Oops!'));
      }
    }
  }

  isResourcesInProgress(notebook) {
    if (notebook) {
      if (notebook.name === 'edge node') {
        return this.allEnvironmentData
          .filter(env => env.user === notebook.user)
          .some(el => this.inProgress([el]) || this.inProgress(el.resources));
      } else if (notebook.resources && notebook.resources.length) {
        return this.inProgress(notebook.resources);
      }
    }
    return false;
  }

  inProgress(resources) {
    return resources.filter(resource => (
      resource.status !== 'failed'
      && resource.status !== 'terminated'
      && resource.status !== 'running'
      && resource.status !== 'stopped')).length > 0;
  }
}


@Component({
  selector: 'confirm-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title"><span class="capitalize">{{ data.action }}</span> resource</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div mat-dialog-content class="content">
    <p>Resource <strong> {{ data.resource_name }}</strong> of user <strong> {{ data.user }} </strong> will be
      <span *ngIf="data.action === 'terminate'"> decommissioned.</span>
      <span *ngIf="data.action === 'stop'">stopped.</span>
    </p>
    <p class="m-top-20"><strong>Do you want to proceed?</strong></p>
  </div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: [
  ]
})
export class ReconfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ReconfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
