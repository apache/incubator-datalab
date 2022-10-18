/*!
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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ModalTitle } from '../../images';
import { AddModalData, AddPlatformFromValue } from '../connected-platforms.models';
import { ConfirmButtonNames } from '../connected-platforms.config';
import { PATTERNS } from '../../../core/util';

const URL_REGEXP_VALIDATION_STRING = '^(http(s)?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]';

@Component({
  selector: 'datalab-connected-platform-dialog',
  templateUrl: './connected-platform-dialog.component.html',
  styleUrls: ['./connected-platform-dialog.component.scss']
})
export class ConnectedPlatformDialogComponent implements OnInit {
  readonly modalTitle: typeof ModalTitle = ModalTitle;
  readonly confirmButtonName: typeof ConfirmButtonNames = ConfirmButtonNames;

  connectedPlatformForm!: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<ConnectedPlatformDialogComponent>,
    private fb: FormBuilder,
    @Inject(MAT_DIALOG_DATA) public data: AddModalData
  ) { }

  ngOnInit(): void {
    this.initForm();
  }

  onBtnClick(flag: boolean): void {
    let responseObj: AddPlatformFromValue;
    if (flag) {
      responseObj = this.connectedPlatformForm.value;
    }
    this.dialogRef.close(responseObj);
  }

  private initForm(): void {
    this.connectedPlatformForm = this.fb.group({
      type: ['', Validators.required],
      url: ['', [ Validators.required, Validators.pattern(URL_REGEXP_VALIDATION_STRING)]],
      name: ['', [ Validators.required, Validators.pattern(PATTERNS.projectName), Validators.minLength(2)]]
    });
  }

  get isFormValid(): boolean {
    return this.connectedPlatformForm.valid;
  }
}
