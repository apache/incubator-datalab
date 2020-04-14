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

import { Component, OnInit, ViewChild, Inject } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { ManageUngitService } from '../../core/services';

import {FolderTreeComponent} from './folder-tree/folder-tree.component';
import {BucketBrowserService, TodoItemNode} from '../../core/services/bucket-browser.service';

@Component({
  selector: 'dlab-bucket-browser',
  templateUrl: './bucket-browser.component.html',
  styleUrls: ['./bucket-browser.component.scss']
})
export class BucketBrowserComponent implements OnInit {
  filenames: Array<any> = [];
  addedFiles = [];
  folderItems = [];
  path = '';

  @ViewChild(FolderTreeComponent, {static: true}) folderTreeComponent;
  private selectedFolder: any;
  private isUploading: boolean;
  private selected: any[];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: 'string',
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<BucketBrowserComponent>,
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
    private bucketBrowserService: BucketBrowserService
  ) {

  }

  ngOnInit() {
    this.bucketBrowserService.initBucket(this.data);
    this.bucketBrowserService.initialize();
    // console.log(this.data);
  }

  showItem(item) {
    const flatItem = this.folderTreeComponent.nestedNodeMap.get(item);
    this.folderTreeComponent.showItem(flatItem);
  }

  handleFileInput(files) {
    //   for (let i = 0; i < files.length; i++) {
    //     const file = files[i];
    //     const path = file.webkitRelativePath.split('/');
    //   }
    // }
    this.filenames = Object['values'](files).map(v => ({item: v.name, 'size': (v.size / 1048576).toFixed(2)} as unknown as TodoItemNode));
    this.addedFiles = [...this.addedFiles, ...this.filenames];
  }

  toggleSelectedFile(file) {
    file.isSelected = !file.isSelected;
    this.selected = this.folderItems.filter(item => item.isSelected);
  }

  filesPicked(files) {
    // console.log(files);

    Array.prototype.forEach.call(files, file => {
      this.addedFiles.push(file.webkitRelativePath);
    });
    // console.log(this.addedFiles);
  }

  onFolderClick(event) {
    this.selectedFolder = event.element1;
    this.folderItems = event.element ? event.element.children : event.children;
    // this.folderItems = this.folderItems.sort((a, b) => (a.children > b.children) ? 1 : -1)
    // console.log(this.folderItems);
    this.path = event.path;
    this.folderItems.forEach(item => item.isSelected = false);
  }

  private upload(tree, path) {
    tree.files.forEach(file => {
      this.addedFiles.push(path + file.name);
    });
    tree.directories.forEach(directory => {
      const newPath = path + directory.name + '/';
      this.addedFiles.push(newPath);
      this.upload(directory, newPath);
    });
  }

  deleteAddedFile(file) {
    this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
  }

  uploadItems() {
    this.isUploading = true;
    setTimeout(() => this.upLoading(), 5000);
  }

  downloadItems() {
    setTimeout(() => this.downloadItemsAction(), 1000);
  }

  upLoading() {
    this.addedFiles.forEach(v => {
      this.folderTreeComponent.addNewItem(this.selectedFolder, v, true);
    });
    this.toastr.success(this.addedFiles.length === 1 ? 'File successfully uploaded' : 'Files successfully uploaded', 'Success!');
    this.addedFiles = [];
    this.isUploading = false;
  }

  downloadItemsAction() {
    this.folderItems.forEach(item => item.isSelected = false);
    this.toastr.success(this.selected.length === 1 ? 'File downloading started' : 'Files downloading started', 'Success!');
    this.selected = this.folderItems.filter(item => item.isSelected);
  }
}



