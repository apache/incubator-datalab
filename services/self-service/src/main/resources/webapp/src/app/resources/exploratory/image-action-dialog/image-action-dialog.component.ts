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

import { Component, Inject, Input, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ImageActions, ImageActionModalData } from '../../images';
import { DialogWindowTabConfig, UserData } from './image-action.model';

const CONFIRM_BUTTON_CONFIG = {
  share: 'Share',
  terminate: 'Yes'
};

@Component({
  selector: 'datalab-image-action-dialog',
  templateUrl: './image-action-dialog.component.html',
  styleUrls: ['./image-action-dialog.component.scss']
})
export class ImageActionDialogComponent implements OnInit {
  @Input() activeTabIndex: boolean;
  @Input() isShareBtnDisabled: Boolean;
  @Input() sharingDataList: UserData[] = [];
  @Input() isTerminate: boolean = false;

  readonly actionType: typeof ImageActions = ImageActions;

  confirmBtnName!: string;

  constructor(
    public dialogRef: MatDialogRef<ImageActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ImageActionModalData,
  ) { }

  ngOnInit(): void {
    this.createConfirmBtnName();
  }

  private createConfirmBtnName(): void {
    this.confirmBtnName = CONFIRM_BUTTON_CONFIG[this.data.actionType];
  }
}
