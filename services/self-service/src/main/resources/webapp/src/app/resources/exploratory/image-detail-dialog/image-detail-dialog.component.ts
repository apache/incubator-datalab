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

import { Component, Inject, OnInit } from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from '@angular/material/dialog';
import {Library, ModalData} from '../../images';
import {LibraryInfoModalComponent} from '../library-info-modal/library-info-modal.component';

@Component({
  selector: 'datalab-image-detail-dialog',
  templateUrl: './image-detail-dialog.component.html',
  styleUrls: [
    './image-detail-dialog.component.scss',
    '../detail-dialog/detail-dialog.component.scss'
  ]
})

export class ImageDetailDialogComponent {
  maxDescriptionLength: number = 170;
  constructor(
    public dialogRef: MatDialogRef<ImageDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ModalData,
    private dialog: MatDialog,
  ) { }

  onLibraryInfo(library: Library): void {
    this.dialog.open(LibraryInfoModalComponent, {
      data: {
        library
      },
      panelClass: 'library-dialog-container'
    });
  }
}
