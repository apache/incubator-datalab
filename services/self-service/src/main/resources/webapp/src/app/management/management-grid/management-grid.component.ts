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

import { Component, OnInit, ViewChild, Input, Output, EventEmitter, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ConfirmationDialogType } from '../../shared';

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
    '../../resources/resources-grid/resources-grid.component.css',
    '../../resources/computational/computational-resources-list/computational-resources-list.component.scss'
  ]
})
export class ManagementGridComponent {
  @Input() allEnvironmentData: Array<any>;
  @Input() resources: Array<any>;
  @Input() uploadKey: boolean;
  @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();
  @Output() actionToggle: EventEmitter<ManageAction> = new EventEmitter();

  @ViewChild('confirmationDialog') confirmationDialog;
  @ViewChild('keyReuploadDialog') keyReuploadDialog;

  constructor(public dialog: MatDialog) {}

  buildGrid(): void {
    this.refreshGrid.emit();
  }

  toggleResourceAction(environment, action, resource?) {
    if (resource) {
      let resource_name = resource ? resource.computational_name : environment.name;
      const dialogRef: MatDialogRef<ConfirmationDialog> = this.dialog.open(ConfirmationDialog, {
        data: { action, resource_name, user: environment.user },
        width: '550px'
      });
      dialogRef.afterClosed().subscribe(result => {
        result && this.actionToggle.emit({ action, environment, resource });
      });
    } else {
      if (action === 'stop') {
        this.confirmationDialog.open({ isFooter: false }, environment, environment.name === 'edge node' ? ConfirmationDialogType.StopEdgeNode : ConfirmationDialogType.StopExploratory);
      } else if (action === 'terminate') {
        this.confirmationDialog.open({ isFooter: false }, environment, ConfirmationDialogType.TerminateExploratory);
      }
    }
  }

  isResourcesInProgress(notebook) {
    if(notebook && notebook.resources.length) {
      return notebook.resources.filter(resource => (
        resource.status !== 'failed'
        && resource.status !== 'terminated'
        && resource.status !== 'running'
        && resource.status !== 'stopped')).length > 0;
    }
    return false;
  }
}

@Component({
  selector: 'confirm-dialog',
  template: `
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
    `
      .content {
        color: #718ba6;
        padding: 20px 50px;
        font-size: 14px;
        font-weight: 400;
      }
    `
  ]
})
export class ConfirmationDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialog>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}
}
