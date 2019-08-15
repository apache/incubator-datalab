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

import { Component, OnInit, EventEmitter, Input, Output, ViewChild, Inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

import { KeyUploadDialogModel } from './key-upload.model';
import { UserAccessKeyService } from '../../../core/services';
import { HTTP_STATUS_CODES } from '../../../core/util';

@Component({
  selector: 'key-upload-dialog',
  templateUrl: 'key-upload-dialog.component.html'
})

export class UploadKeyDialogComponent implements OnInit {
  model: KeyUploadDialogModel;
  @Input() primaryUploading: boolean = true;

  @ViewChild('userAccessKeyUploadControl') userAccessKeyUploadControl;
  @Output() checkInfrastructureCreationProgress: EventEmitter<{}> = new EventEmitter();
  @Output() generateUserKey: EventEmitter<{}> = new EventEmitter();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<UploadKeyDialogComponent>,
    private userAccessKeyService: UserAccessKeyService,
    public toastr: ToastrService
  ) {
    this.model = KeyUploadDialogModel.getDefault();
  }

  ngOnInit() {
    this.model = new KeyUploadDialogModel(null,
      response => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.close();
          this.checkInfrastructureCreationProgress.emit();
        }
      },
      error => this.toastr.error(error.message, 'Oops!'),
      this.userAccessKeyService);
  }

  public uploadUserAccessKey_onChange($event) {
    if ($event.target.files.length > 0) {
      this.model.setUserAccessKey($event.target.files[0]);
    }
  }

  public generateUserAccessKey_btnClick($event) {
    this.generateUserKey.emit($event);
    this.close();
  }

  public uploadUserAccessKey_btnClick($event) {
    this.model.confirmAction(this.primaryUploading);
    $event.preventDefault();
    return false;
  }

  public close() {
    this.dialogRef.close();
  }
}
