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

import { Component, OnInit, Inject, ViewEncapsulation } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'bucket-confirmation-dialog',
  templateUrl: 'bucket-confirmation-dialog.component.html',
  styleUrls: ['./bucket-confirmation-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class BucketConfirmationDialogComponent implements OnInit {
  isFolders: boolean = false;
  uploadActions = ['Replace existing object', 'Skip uploading object'];
  fileAction: string = this.uploadActions[1];
  actionForAll: boolean = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<BucketConfirmationDialogComponent>,
    public toastr: ToastrService
  ) { }

  ngOnInit() {
    if (this.data.type === 'delete') {
      this.isFolders = !!this.data.items.filter(v => v.children).length;
    }
  }

  toggleActionForAll() {
    this.actionForAll = !this.actionForAll;
  }

  submitResolving() {
    const submitObj = {
      replaceObject: !this.uploadActions.indexOf(this.fileAction), 
      forAll: this.actionForAll
    };

    this.dialogRef.close(submitObj);
  }
}