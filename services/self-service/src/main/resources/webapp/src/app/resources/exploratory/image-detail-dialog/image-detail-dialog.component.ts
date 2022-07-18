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

const libraryMock: Library[] = [
  {
    add_pkgs: [],
    available_versions: [],
    error_message: 'no_error\n',
    group: 'os_pkg',
    name: 'csvkit',
    status: 'installed',
    version: '1.0.2-2',
  },
  {
    add_pkgs: [],
    available_versions: [],
    error_message: ' ',
    group: 'pip3',
    name: 'sScrapy',
    status: 'installed',
    version: '2.6.1',
  },
  {
    add_pkgs: [],
    available_versions: [],
    error_message: ' ',
    group: 'pip3',
    name: 'zScrapy',
    status: 'installed',
    version: '2.6.1',
  }
  ,
  {
    add_pkgs: [],
    available_versions: [],
    error_message: ' ',
    group: 'pip3',
    name: 'aScrapy',
    status: 'installed',
    version: '2.6.1',
  },
  {
    add_pkgs: null,
    available_versions: null,
    error_message: null,
    group: 'java',
    name: 'io.github.egonw.bacting:bioclipse-core',
    status: 'installed',
    version: '2.8.0.15',
  },
  {
    add_pkgs: null,
    available_versions: null,
    error_message: null,
    group: 'java',
    name: 'io.github.egonw.bacting:bioclipse-core',
    status: 'installed',
    version: '2.8.0.15',
  },
  {
    add_pkgs: [],
    available_versions: [],
    error_message: ' ',
    group: 'others',
    name: 'Pillow3f',
    status: 'installed',
    version: '0.0.7',
  }
];

@Component({
  selector: 'datalab-image-detail-dialog',
  templateUrl: './image-detail-dialog.component.html',
  styleUrls: [
    './image-detail-dialog.component.scss',
    '../detail-dialog/detail-dialog.component.scss'
  ]
})

export class ImageDetailDialogComponent implements OnInit {
  maxDescriptionLength: number = 170;
  libraryList = [];

  constructor(
    public dialogRef: MatDialogRef<ImageDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ModalData,
    private dialog: MatDialog,
  ) { }

  ngOnInit() {
    this.data.image.libraries = libraryMock;
    this.libraryList = this.normalizeLibraries();
    console.log(this.normalizeLibraries());
  }

  onLibraryInfo(libraryList): void {
    this.dialog.open(LibraryInfoModalComponent, {
      data: {
        libraryList
      },
      panelClass: 'library-dialog-container'
    });
  }

  private normalizeLibraries() {
    return this.data.image.libraries.reduce((acc, item) => {
      const libraryName = this.normalizeLibraryName(item);
      const isLibAdded = acc.find(({name}) => item.group === name);
      if (!isLibAdded) {
        const newLibrary = {
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
        item.libs.sort();
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
}
