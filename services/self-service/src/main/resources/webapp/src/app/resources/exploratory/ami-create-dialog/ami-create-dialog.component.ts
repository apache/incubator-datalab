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

import { Component, OnInit, Inject } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { UserResourceService } from '../../../core/services';
import {HTTP_STATUS_CODES, PATTERNS} from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-ami-create-dialog',
  templateUrl: './ami-create-dialog.component.html',
  styleUrls: ['./ami-create-dialog.component.scss']
})
export class AmiCreateDialogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public notebook: any;
  public createAMIForm: FormGroup;
  public provider: string;
  delimitersRegex = /[-_]?/g;
  imagesList: any;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<AmiCreateDialogComponent>,
    private _userResource: UserResourceService,
    private _fb: FormBuilder,
  ) { }

  ngOnInit() {
    this.notebook = this.data;
    this.provider = this.data.cloud_provider;

    this.initFormModel();
    this._userResource.getImagesList(this.data.project).subscribe(res => this.imagesList = res);
  }

  public assignChanges(data) {
    this._userResource.createAMI(data).subscribe(
      response => response.status === HTTP_STATUS_CODES.ACCEPTED && this.dialogRef.close(),
      error => this.toastr.error(error.message || `Image creation failed!`, 'Oops!'));
  }

  private initFormModel(): void {
    this.createAMIForm = this._fb.group({
      name: ['', [Validators.required, Validators.pattern(PATTERNS.namePattern), this.providerMaxLength, this.checkDuplication.bind(this)]],
      description: [''],
      exploratory_name: [this.notebook.name],
      project_name: [this.notebook.project]
    });
  }

  private providerMaxLength(control) {
    if (control && control.value)
      return control.value.length <= 10 ? null : { valid: false };
  }

  private checkDuplication(control) {
    if (control.value)
      return this.isDuplicate(control.value) ? { duplication: true } : null;
  }

  private isDuplicate(value: string) {
    for (let index = 0; index < this.imagesList.length; index++) {
      if (this.delimitersFiltering(value) === this.delimitersFiltering(this.imagesList[index].name))
        return true;
    }
    return false;
  }

  private delimitersFiltering(resource): string {
    return resource.replace(this.delimitersRegex, '').toString().toLowerCase();
  }
}
