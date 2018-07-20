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

import { HealthStatusService, UserAccessKeyService } from '../../core/services';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

@Component({
  selector: 'management-grid',
  templateUrl: 'management-grid.component.html',
  styleUrls: ['./management-grid.component.scss',
              '../../resources/resources-grid/resources-grid.component.css',
              '../../resources/computational/computational-resources-list/computational-resources-list.component.scss']
})
export class ManagementGridComponent implements OnInit {

   @Input() allEnvironmentData: Array<any>;
   @Input() resources: Array<any>;
   @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();

   @ViewChild('confirmationDialog') confirmationDialog;
   @ViewChild('keyReuploadDialog') keyReuploadDialog;
   

    constructor(
      public dialog: MatDialog
    ) { }

    ngOnInit(): void { }
    
    buildGrid(): void {
      this.refreshGrid.emit();
    }

    toggleResourceAction(environment, resource, action) {
      const dialogRef: MatDialogRef<ConfirmationDialog> = this.dialog.open(ConfirmationDialog, { data: {action, resource}, width: '550px' });
      dialogRef.afterClosed().subscribe(result => {
        console.log(environment, result);
      });
    }
}

@Component({
  selector: 'confirmation-dialog',
  template: `
  <div mat-dialog-content class="content">

    <p>Computational resource <strong> {{ data.resource.computational_name }}</strong> will be 
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
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400 }
  `]
})
export class ConfirmationDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialog>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}