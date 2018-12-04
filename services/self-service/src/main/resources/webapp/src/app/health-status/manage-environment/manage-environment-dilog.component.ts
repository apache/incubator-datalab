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

import { Component, ViewChild, Output, EventEmitter, ViewEncapsulation, Inject } from '@angular/core';
import { Validators, FormBuilder, FormGroup, FormArray } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { DICTIONARY } from '../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-manage-env-dilog',
  templateUrl: './manage-environment-dilog.component.html',
  styleUrls: ['./manage-environment-dilog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ManageEnvironmentComponent {
  readonly DICTIONARY = DICTIONARY;
  public usersList: Array<string> = [];
  public manageUsersForm: FormGroup;
  public manageTotalsForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;
  @Output() manageEnv: EventEmitter<{}> = new EventEmitter();
  @Output() setBudget: EventEmitter<{}> = new EventEmitter();

  constructor(
    private _fb: FormBuilder,
    public dialog: MatDialog
  ) { }

  get usersEnvironments(): FormArray{
    return <FormArray>this.manageUsersForm.get('users');
  }

  public open(param, data, settings): void {
    this.usersList = data;
    !this.manageUsersForm && this.initForm();

    this.manageUsersForm.setControl('users',
    this._fb.array((this.usersList || []).map((x: any) => this._fb.group({
      name: x.name, budget: [x.budget, [Validators.min(0), this.userValidityCheck.bind(this)]]
    }))));
    this.manageUsersForm.controls['total'].setValue(settings.conf_max_budget || null);
    this.bindDialog.open(param);
  }

  public setBudgetLimits(value) {
    this.setBudget.emit(value);
    this.bindDialog.close();
  }

  public applyAction(action, user) {
    const dialogRef: MatDialogRef<ConfirmActionDialogComponent> = this.dialog.open(
      ConfirmActionDialogComponent, { data: {action, user: user.value.name}, width: '550px' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) this.manageEnv.emit({action, user: user.value.name});
    });
  }

  private initForm(): void {
    this.manageUsersForm = this._fb.group({
      total: [null, [Validators.min(0), this.totalValidityCheck.bind(this)]],
      users: this._fb.array([this._fb.group({ name: '', budget: null })])
    });
  }

  private getCurrentUsersTotal(): number {
    return this.manageUsersForm.value.users.reduce((memo, el) => memo += el.budget, 0);
  }

  private getCurrentTotalValue(): number {
    return this.manageUsersForm.value.total;
  }

  private totalValidityCheck(control) {
    return (control && control.value)
      ? (control.value >= this.getCurrentUsersTotal() ? null : { overrun: true })
      : null;
  }

  private userValidityCheck(control) {
    if (control && control.value) {
      return (this.getCurrentTotalValue() && this.getCurrentTotalValue() < this.getCurrentUsersTotal()) ? { overrun: true } : null;
    }
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
export class ConfirmActionDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
