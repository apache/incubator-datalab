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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { LibraryInfoItem, Library, ImageDetailModalData, SharingStatus, SharedWithField } from '../../images';
import { LibraryInfoModalComponent } from '../library-info-modal/library-info-modal.component';
import { caseInsensitiveSortUtil } from '../../../core/util';

@Component({
  selector: 'datalab-image-detail-dialog',
  templateUrl: './image-detail-dialog.component.html',
  styleUrls: [
    './image-detail-dialog.component.scss',
    '../detail-dialog/detail-dialog.component.scss'
  ]
})

export class ImageDetailDialogComponent implements OnInit {
  readonly sharingStatus: typeof SharingStatus = SharingStatus;

  maxDescriptionLength: number = 170;
  libraryList = [];
  sortedShareWith: SharedWithField;

  constructor(
    public dialogRef: MatDialogRef<ImageDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ImageDetailModalData,
    private dialog: MatDialog,
  ) { }

  ngOnInit(): void {
    this.libraryList = this.normalizeLibraries();
    this.sortedShareWith = this.sortUserData();
  }

  onLibraryInfo(libraryList): void {
    this.dialog.open(LibraryInfoModalComponent, {
      data: {
        libraryList
      },
      panelClass: 'library-dialog-container'
    });
  }

  private normalizeLibraries(): LibraryInfoItem[] {
    return this.data.image.libraries.reduce((acc, item) => {
      const libraryName = this.normalizeLibraryName(item);
      const isLibAdded = acc.find(({name}) => item.group === name);
      if (!isLibAdded) {
        const newLibrary: LibraryInfoItem = {
          name: item.group,
          libs: [`${libraryName} v ${item.version}`]
        };
        acc.push(newLibrary);
      } else {
        acc.find(({name}) => item.group === name).libs.push(`${libraryName} v ${item.version}`);
      }
      return acc;
    }, [])
      .map(item => {
        caseInsensitiveSortUtil(item.libs);
        return item;
    });
  }

  private normalizeLibraryName(library: Library): string {
    if (library.group === 'java') {
      const [, libName] = library.name.split(':');
      return libName;
    }
    return library.name;
  }

  private sortUserData(): SharedWithField {
    const { groups, users } = this.data.image.sharedWith;
    return {
      users: this.sortUserDataByValue(users),
      groups: this.sortUserDataByValue(groups),
    };
  }

  private sortUserDataByValue(arr: string[]): string[] {
    return arr.sort((a, b) => a.toLowerCase() > b.toLowerCase() ? 1 : -1);
  }
}
