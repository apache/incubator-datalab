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

import { Component, Output, EventEmitter, ViewEncapsulation, Inject, OnInit } from '@angular/core';
import { Validators, FormBuilder, FormGroup, FormArray } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { CheckUtils } from '../../../core/util';

@Component({
  selector: 'datalab-manage-env-dilog',
  templateUrl: './manage-environment-dilog.component.html',
  styleUrls: ['./manage-environment-dilog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ManageEnvironmentComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  readonly CheckUtils = CheckUtils;

  public manageUsersForm: FormGroup;
  public manageTotalsForm: FormGroup;

  @Output() manageEnv: EventEmitter<{}> = new EventEmitter();
  private initialFormState: any;
  public isFormChanged: boolean = true;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<ManageEnvironmentComponent>,
    private _fb: FormBuilder,
  ) { }

  ngOnInit() {
    !this.manageUsersForm && this.initForm();
    this.setProjectsControl();
    this.manageUsersForm.controls['total'].setValue(this.data.total.conf_max_budget || '');
    this.onFormChange();
    this.manageUsersForm.value.total = +this.manageUsersForm.value.total;
    if (this.manageUsersForm.value.total === 0) this.manageUsersForm.value.total = null;
    this.initialFormState = this.manageUsersForm.value;
  }

  public onFormChange() {
    this.manageUsersForm.valueChanges.subscribe(value => {
      this.isFormChanged = JSON.stringify(this.initialFormState) === JSON.stringify(this.manageUsersForm.value);
      if (this.getCurrentTotalValue()) {
        if (this.getCurrentTotalValue() >= this.getCurrentUsersTotal()) {
          this.manageUsersForm.controls['total'].setErrors(null);
          if (this.manageUsersForm.controls['total'].value > 1000000000) this.manageUsersForm.controls['total'].setErrors({ max: true });
          this.manageUsersForm.controls['projects']['controls'].forEach(v => {
            v.controls['budget'].errors && 'max' in v.controls['budget'].errors 
              ? null 
              : v.controls['budget'].setErrors(null);
          });
        } else {
          this.manageUsersForm.controls['total'].setErrors({ overrun: true });
        }
      }
    });
  }

  get usersEnvironments(): FormArray {
    return <FormArray>this.manageUsersForm.get('projects');
  }

  public setBudgetLimits(value) {
    if (this.getCurrentTotalValue() >= this.getCurrentUsersTotal() || !this.getCurrentTotalValue()) {
      value.projects = value.projects.filter((v, i) =>
        this.initialFormState.projects[i].budget !== v.budget ||
        this.initialFormState.projects[i].monthlyBudget !== v.monthlyBudget);
      value.isTotalChanged = this.initialFormState.total !== value.total;
      this.dialogRef.close(value);
    } else {
      this.manageUsersForm.controls['total'].setErrors({ overrun: true });
    }
  }

  public setProjectsControl() {
    this.manageUsersForm.setControl('projects',
      this._fb.array((this.data.projectsList || [])
        .map((x: any, index: number) => this._fb.group({
          project: x.name,
          budget: [x.budget.value, [Validators.max(1000000000), this.userValidityCheck.bind(this)]],
          monthlyBudget: x.budget.monthlyBudget,
        }))
      )
    );
  }

  private initForm(): void {
    this.manageUsersForm = this._fb.group({
      total: [null, [Validators.min(0), this.totalValidityCheck.bind(this), Validators.max(1000000000)]],
      projects: this._fb.array([this._fb.group({ project: '', budget: null, status: '' })])
    });
  }

  private getCurrentUsersTotal(): number {
    return this.manageUsersForm.value.projects.reduce((memo, el) => memo += el.budget, 0);
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
      if (control.parent) {
        this.manageUsersForm.value.projects.find(v => v.project === control.parent.value.project).budget = control.value;
      }
      return (this.getCurrentTotalValue() && this.getCurrentTotalValue() < this.getCurrentUsersTotal()) ? { overrun: true } : null;
    }
  }
}

@Component({
  selector: 'dialog-result-example-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title"><span class="capitalize">{{ data.action }}</span> resource</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div mat-dialog-content class="content">
    <p>Environment of <span class="strong">{{ data.project }}</span> will be
      <span *ngIf="data.action === 'terminate'"> terminated.</span>
      <span *ngIf="data.action === 'stop'">stopped.</span>
    </p>
    <p class="m-top-20"><span class="strong">Do you want to proceed?</span></p>
  </div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: []
})
export class ConfirmActionDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
