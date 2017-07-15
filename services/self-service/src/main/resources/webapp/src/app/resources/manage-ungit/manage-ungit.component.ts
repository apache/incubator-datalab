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

import { Component, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Response } from '@angular/http';
import { MdDialog, MdDialogRef, MdDialogConfig } from '@angular/material';

import { AccountCredentials, MangeUngitModel } from './manage-ungit.model';
import { ManageUngitService } from './../../core/services';
import { ErrorMapUtils, HTTP_STATUS_CODES } from './../../core/util';

@Component({
  selector: 'dlab-manage-ungit',
  templateUrl: './manage-ungit.component.html',
  styleUrls: ['./manage-ungit.component.css',
              '../exploratory/install-libraries/install-libraries.component.css']
})
export class ManageUngitComponent implements OnInit {
  model: MangeUngitModel;
  gitCredentials: Array<AccountCredentials> = [];
  currentEditableItem: AccountCredentials;

  mail_validity_pattern = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;
  hostname_validity_pattern = /^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])+\.[a-z\.]+/;

  public editableForm: boolean = false;
  public updateAccountCredentialsForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('tabGroup') tabGroup;

  constructor(
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
    public dialog: MdDialog) {
    this.model = MangeUngitModel.getDefault(manageUngitService);
  }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.cancelAllModifyings();

    this.initFormModel();
    this.getGitCredentials();
  }

  public open(param): void {
    if (!this.bindDialog.isOpened)
      this.model = new MangeUngitModel((response: Response) => { },
      (response: Response) => {
        // this.processError = true;
        // this.errorMessage = ErrorMapUtils.setErrorMessage(response);
        console.log('ERROR', response);
      },
      () => {
        this.bindDialog.open(param);

        if (!this.gitCredentials.length)
          this.tabGroup.selectedIndex = 1;
      },
      this.manageUngitService);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  public resetForm(): void {
    this.initFormModel();
    this.currentEditableItem = null;
  }

  public cancelAllModifyings() {
    this.editableForm = false;
    this.getGitCredentials();
    this.resetForm();
  }

  public editSpecificAccount(item: AccountCredentials) {
    this.tabGroup.selectedIndex = 1;
    this.currentEditableItem = item;

    this.updateAccountCredentialsForm = this._fb.group({
      'hostname': [item.hostname, Validators.compose([Validators.required, Validators.pattern(this.hostname_validity_pattern)])],
      'username': [item.username, Validators.required],
      'email': [item.email, Validators.compose([Validators.required, Validators.pattern(this.mail_validity_pattern)])],
      'login': [item.login, Validators.required],
      'password': ['', Validators.compose([Validators.required, Validators.minLength(6)])],
      'confirmPassword': ['', Validators.compose([Validators.required, this.validConfirmField.bind(this)])]
    });
  }

  public deleteAccount(item: AccountCredentials) {
    const dialogRef: MdDialogRef<DialogResultExampleDialog> = this.dialog.open(DialogResultExampleDialog, { data: item, width: '550px' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.gitCredentials.splice(this.gitCredentials.indexOf(item), 1);
        this.model.confirmAction(this.gitCredentials);
      }
    });
  }

  public assignChanges(current: any): void {
    const modifiedCredentials = JSON.parse(JSON.stringify(this.gitCredentials));
    const index = modifiedCredentials.findIndex(el => JSON.stringify(el) === JSON.stringify(this.currentEditableItem));

    delete current['confirmPassword'];
    index > -1 ? modifiedCredentials.splice(index, 1, current) : modifiedCredentials.push(current);

    this.gitCredentials = modifiedCredentials;
    this.editableForm = true;
    this.tabGroup.selectedIndex = 0;
    this.resetForm();
  }

  public editAccounts_btnClick() {
    this.model.confirmAction(this.gitCredentials);
    this.tabGroup.selectedIndex = 0;
    this.editableForm = false;
  }

  private initFormModel(): void {
    this.updateAccountCredentialsForm = this._fb.group({
      'hostname': ['', Validators.compose([Validators.required, Validators.pattern(this.hostname_validity_pattern)])],
      'username': ['', Validators.required],
      'email': ['', Validators.compose([Validators.required, Validators.pattern(this.mail_validity_pattern)])],
      'login': ['', Validators.required],
      'password': ['', Validators.compose([Validators.required, Validators.minLength(6)])],
      'confirmPassword': ['', Validators.compose([Validators.required, this.validConfirmField.bind(this)])]
    });
  }

  private getGitCredentials(): void {
    this.model.getGitCredentials()
      .subscribe((response: any) => {
          this.gitCredentials = response.git_creds || [];
        },
        error => console.log(error));
  }

  private validConfirmField(control) {
    if (this.updateAccountCredentialsForm) {
      const passReq = this.updateAccountCredentialsForm.get('password');
      const confirmPassReq = this.updateAccountCredentialsForm.get('confirmPassword');

      return passReq.value === confirmPassReq.value ? null : { valid: false };
    }
  }
}

@Component({
  selector: 'dialog-result-example-dialog',
  template: `
  <div md-dialog-content class="content">
    <p>Account <strong>{{ dialogRef.config.data.hostname }}</strong> will be decommissioned.</p>
    <p><strong>Do you want to proceed?</strong></p>
  </div>
  <div class="text-center">
    <button type="button" class="butt" md-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" md-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400 }
  `]
})
export class DialogResultExampleDialog {
  constructor(public dialogRef: MdDialogRef<DialogResultExampleDialog>) { }
}