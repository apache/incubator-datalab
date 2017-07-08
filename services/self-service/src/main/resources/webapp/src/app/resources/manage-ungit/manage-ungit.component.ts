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
  gitCredentials: Array<AccountCredentials>;

  public editableForm: boolean = false;
  public updateAccountCredentialsForm: FormGroup;

  currentEditableItem: AccountCredentials;
  currentEditableHostname: string;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('tabGroup') tabGroup;
  @ViewChild('confirm') confirmPassword;

  constructor(
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder) {
    this.model = MangeUngitModel.getDefault(manageUngitService);
  }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetForm();

    this.initFormModel();
    this.getGitCredentials();
  }

  public open(param): void {
    if (!this.bindDialog.isOpened)
      this.model = new MangeUngitModel((response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          // this.resetDialog();
        }
      },
      (response: Response) => {
        // this.processError = true;
        // this.errorMessage = ErrorMapUtils.setErrorMessage(response);
        console.log('ERROR');
        
      },
      () => {
        this.bindDialog.open(param);

        if (!this.gitCredentials)
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
    console.log(this.confirmPassword);
  }

  public cancelModifyings() {
    this.getGitCredentials();
    this.editableForm = false;
  }

  public editSpecificAccount(item: AccountCredentials) {
    this.tabGroup.selectedIndex = 1;
    this.currentEditableItem = item;

    this.updateAccountCredentialsForm = this._fb.group({
      'hostname': [item.hostname, Validators.required],
      'username': [item.username, Validators.required],
      'email': [item.email, Validators.required],
      'login': [item.login, Validators.required],
      'password': [item.password, Validators.required]
    });
  }

  public assignChanges(current: FormGroup): void {
    const modifiedCredentials = JSON.parse(JSON.stringify(this.gitCredentials));
    const index = modifiedCredentials.findIndex(el => JSON.stringify(el) === JSON.stringify(this.currentEditableItem));
    
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
      'hostname': ['', Validators.required],
      'username': ['', Validators.required],
      'email': ['', Validators.required],
      'login': ['', Validators.required],
      'password': ['', Validators.required]
    });
  }

  private getGitCredentials(): void {
    this.model.getGitCredentials()
      .subscribe((response: any) => {
          this.gitCredentials = response.git_creds || [];
        },
        error => console.log(error));
  }
}
