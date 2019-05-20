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

import { HealthStatusService, UserAccessKeyService } from '../../../core/services';
import { ConfirmationDialogType } from '../../../shared';
import { FileUtils } from '../../../core/util';

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
    '../../../resources/resources-grid/resources-grid.component.css',
    '../../../resources/computational/computational-resources-list/computational-resources-list.component.scss'
  ]
})
export class ManagementGridComponent implements OnInit {
  @Input() allEnvironmentData: Array<any>;
  @Input() environmentsHealthStatuses: Array<any>;
  @Input() healthStatus: string;
  @Input() resources: Array<any>;
  @Input() uploadKey: boolean;
  @Input() isAdmin: boolean;
  @Input() currentUser: string = '';
  @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();
  @Output() actionToggle: EventEmitter<ManageAction> = new EventEmitter();

  @ViewChild('confirmationDialog') confirmationDialog;
  @ViewChild('keyReuploadDialog') keyReuploadDialog;

  constructor(
    private healthStatusService: HealthStatusService,
    private userAccessKeyService: UserAccessKeyService,
    public toastr: ToastrService,
    public dialog: MatDialog
  ) {}

  ngOnInit() {
  }

  buildGrid(): void {
    this.refreshGrid.emit();
  }

  toggleResourceAction(environment, action: string, resource?) {
    if (resource) {
      const resource_name = resource ? resource.computational_name : environment.name;
      const dialogRef: MatDialogRef<ConfirmationDialogComponent> = this.dialog.open(ConfirmationDialogComponent, {
        data: { action, resource_name, user: environment.user },
        width: '550px',
        panelClass: 'error-modalbox'
      });
      dialogRef.afterClosed().subscribe(result => {
        result && this.actionToggle.emit({ action, environment, resource });
      });
    } else {
      if (action === 'stop') {
        this.confirmationDialog.open(
          { isFooter: false },
          environment,
          (environment.name === 'edge node' || environment.type.toLowerCase() === 'edge node')
            ? ConfirmationDialogType.StopEdgeNode
            : ConfirmationDialogType.StopExploratory,
          );
      } else if (action === 'terminate') {
        this.confirmationDialog.open({ isFooter: false }, environment, ConfirmationDialogType.TerminateExploratory);
      } else if (action === 'run') {
        this.healthStatusService
          .runEdgeNode()
          .subscribe(() => {
            this.buildGrid();
            this.toastr.success('Edge node is starting!', 'Processing!');
          }, error => this.toastr.error('Edge Node running failed!', 'Oops!'));
        } else if (action === 'recreate') {
          this.healthStatusService
            .recreateEdgeNode()
            .subscribe(() => {
              this.buildGrid();
              this.toastr.success('Edge Node recreation is processing!', 'Processing!');
            }, error => this.toastr.error('Edge Node recreation failed!', 'Oops!'));
        }
    }
  }

  isResourcesInProgress(notebook) {
    if (notebook) {
      if (notebook.name === 'edge node') {
        return this.allEnvironmentData
          .filter(env => env.user === notebook.user)
          .some(el => this.inProgress([el]) || this.inProgress(el.resources));
      } else if (notebook.resources.length) {
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

  showReuploaKeydDialog() {
    this.keyReuploadDialog.open({ isFooter: false });
  }

  public generateUserKey() {
    this.userAccessKeyService.regenerateAccessKey().subscribe(data => {
      FileUtils.downloadFile(data);
      this.buildGrid();
    });
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
export class ConfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}
}
