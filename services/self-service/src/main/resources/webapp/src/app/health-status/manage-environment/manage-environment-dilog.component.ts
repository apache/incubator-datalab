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

import { Component, OnInit, ViewChild, Output, EventEmitter, ViewEncapsulation, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { DICTIONARY } from './../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-manage-env-dilog',
  templateUrl: './manage-environment-dilog.component.html',
  styleUrls: ['./manage-environment-dilog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ManageEnvironmentComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  public errorMessage: string = '';
  public usersList: Array<string> = [];

  @ViewChild('bindDialog') bindDialog;
  @Output() manageEnv: EventEmitter<{}> = new EventEmitter();

  constructor(public dialog: MatDialog) { }

  ngOnInit() {
    this.bindDialog.onClosing = () => {
      this.errorMessage = '';
    };
  }

  public open(param, data): void {
    this.usersList = data;
    this.bindDialog.open(param);
  }

  public applyAction(action, user) {
    const dialogRef: MatDialogRef<ConfirmActionDialog> = this.dialog.open(ConfirmActionDialog, { data: {action, user}, width: '550px' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) this.manageEnv.emit({action, user});
    });
  }
}


@Component({
  selector: 'dialog-result-example-dialog',
  template: `
  <div mat-dialog-content class="content">
    <p>Environment of <b>{{ data.user }}</b> will be 
      <span *ngIf="data.action === 'terminate'"> terminated.</span>
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
export class ConfirmActionDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmActionDialog>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}