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

import { Component, OnInit, ViewChild, Inject } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { AccountCredentials, MangeUngitModel } from './manage-ungit.model';
import { ManageUngitService } from '../../core/services';

@Component({
  selector: 'datalab-manage-ungit',
  templateUrl: './manage-ungit.component.html',
  styleUrls: ['./manage-ungit.component.scss',
    '../exploratory/install-libraries/install-libraries.component.scss']
})
export class ManageUngitComponent implements OnInit {
  model: MangeUngitModel;
  gitCredentials: Array<AccountCredentials> = [];
  currentEditableItem: AccountCredentials = null;

  mail_validity_pattern = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,63})$/;
  hostname_validity_pattern = /^([a-zA-Z0-9]+(\.[a-zA-Z0-9]+)+.*)$/;
  login_acceptance_pattern = '[-_@.a-zA-Z0-9]+';
  acceptance_pattern = '[-_ a-zA-Z0-9]+';

  public editableForm: boolean = false;
  public updateAccountCredentialsForm: FormGroup;

  @ViewChild('tabGroupGit') tabGroupGit;

  constructor(
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<ManageUngitComponent>,
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
  ) {
    this.model = MangeUngitModel.getDefault(manageUngitService);
  }

  ngOnInit() {
    this.initFormModel();
    this.model.getGitCredentials().subscribe(
      (credentials: any) => {
        this.gitCredentials = credentials.git_creds || [];
        this.open();
      }, () => this.open());
  }

  public open(): void {
    this.model = new MangeUngitModel(() => { },
      error => this.toastr.error(error.message || 'Manage git credentials failed!', 'Oops!'),
      () => {
        // if (!this.gitCredentials.length)
        //   this.tabGroupGit.selectedIndex = 1;
      },
      this.manageUngitService);
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
    this.tabGroupGit.selectedIndex = 1;
    this.currentEditableItem = item;

    this.updateAccountCredentialsForm = this._fb.group({
      'hostname': [item.hostname, Validators.compose(
        [Validators.required,  Validators.pattern(this.hostname_validity_pattern), this.containsHostname.bind(this)]
      )],
      'username': [item.username, Validators.compose([Validators.required, Validators.pattern(this.acceptance_pattern)])],
      'email': [item.email, Validators.compose([Validators.required, Validators.pattern(this.mail_validity_pattern)])],
      'login': [item.login, Validators.compose([Validators.required, Validators.pattern(this.login_acceptance_pattern)])],
      'password': ['', Validators.compose([Validators.required, Validators.minLength(6)])],
      'confirmPassword': ['', Validators.compose([Validators.required])]
    });
  }

  public deleteAccount(item: AccountCredentials) {
    const dialogRef: MatDialogRef<ConfirmDeleteAccountDialogComponent> = this.dialog.open(
      ConfirmDeleteAccountDialogComponent,
      { data: item, width: '550px', panelClass: 'error-modalbox' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.gitCredentials.splice(this.gitCredentials.indexOf(item), 1);
        this.model.confirmAction(this.gitCredentials);
        this.toastr.success('Git credentials deleted successfully!', 'Success!');
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
    this.tabGroupGit.selectedIndex = 0;
    this.resetForm();
  }

  public editAccounts_btnClick() {
    this.model.confirmAction(this.gitCredentials);
    this.tabGroupGit.selectedIndex = 0;
    this.editableForm = false;
    this.toastr.success('Git credentials updated successfully!', 'Success!');
  }

  private initFormModel(): void {
    this.updateAccountCredentialsForm = this._fb.group({
      'hostname': ['', Validators.compose(
        [Validators.required, Validators.pattern(this.hostname_validity_pattern), this.containsHostname.bind(this)]
      )],
      'username': ['', Validators.compose([Validators.required, Validators.pattern(this.acceptance_pattern)])],
      'email': ['', Validators.compose([Validators.required, Validators.pattern(this.mail_validity_pattern)])],
      'login': ['', Validators.compose([Validators.required, Validators.pattern(this.login_acceptance_pattern)])],
      'password': ['', Validators.compose([Validators.required, Validators.minLength(6)])],
      'confirmPassword': ['', Validators.compose([Validators.required])]
    });
  }

  private getGitCredentials(): void {
    this.model.getGitCredentials()
      .subscribe(
        (response: any) => this.gitCredentials = response.git_creds || [],
        error => this.toastr.error(error.message || 'Git credentials loading failed!', 'Oops!'));
  }

  private containsHostname(control) {
    let duplication = null;

    if (control.value)
      for (let index = 0; index < this.gitCredentials.length; index++) {
        if (control.value === this.gitCredentials[index].hostname)
          duplication = { duplicate: true };
        if (this.currentEditableItem && control.value === this.currentEditableItem.hostname)
          duplication = null;
      }
    return duplication;
  }
}

@Component({
  selector: 'dialog-result-example-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title"><i class="material-icons">priority_high</i>Warning</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div mat-dialog-content class="content">
    <p>Account <span class="strong">{{ data.hostname }}</span> will be decommissioned.</p>
    <p class="m-top-20"><span class="strong">Do you want to proceed?</span></p>
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
export class ConfirmDeleteAccountDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteAccountDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
