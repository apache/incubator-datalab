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

  public updateAccountCredentialsForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('tabGroup') tabGroup;

  constructor(
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder) {
    this.model = MangeUngitModel.getDefault(manageUngitService);
  }

  ngOnInit() {
    this.initFormModel();
    this.getGitCredentials();
  }

  // public open(param): void {
  //   if (!this.bindDialog.isOpened)
  //     this.bindDialog.open(param);
  // }

  public open(param): void {
    if (!this.bindDialog.isOpened)
      this.model = new MangeUngitModel((response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          // this.getInstalledLibrariesList();
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

  initFormModel(): void {
    this.updateAccountCredentialsForm = this._fb.group({
      'hostname': '',
      'username': '',
      'email': '',
      'login': '',
      'password': ''
    });
  }

  public editAccounts_btnClick($event, item) {
    console.log($event, item);
    
    this.model.confirmAction(item);
    this.tabGroup.selectedIndex = 0;

  }

  private getGitCredentials(): void {
    this.model.getGitCredentials()
      .subscribe((response: any) => {
          console.log(response);
          this.gitCredentials = response.git_creds;
        },
        error => {
          console.log(error);
        });
  }
}
