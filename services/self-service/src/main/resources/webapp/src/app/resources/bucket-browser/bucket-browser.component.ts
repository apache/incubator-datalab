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

// import { AccountCredentials, MangeUngitModel } from './bucket-browser.model';
import { ManageUngitService } from '../../core/services';
import {logger} from 'codelyzer/util/logger';
import {FolderTreeComponent} from './folder-tree/folder-tree.component';

@Component({
  selector: 'dlab-bucket-browser',
  templateUrl: './bucket-browser.component.html',
  styleUrls: ['./bucket-browser.component.scss']
})
export class BucketBrowserComponent implements OnInit {
  filenames: Array<any> = [];
  uploadPaths = [];
  folderItems = [];
  path = '';
  selectedFolderItems = [];
  // @ViewChild('tabGroupGit', { static: false }) tabGroupGit;
  @ViewChild(FolderTreeComponent, {static: true}) folderTreeComponent;
  private selectedFolder: any;

  constructor(
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<BucketBrowserComponent>,
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
  ) {

  }

  ngOnInit() {

  }

  showItem(item) {
    const flatItem = this.folderTreeComponent.nestedNodeMap.get(item);
    // this.folderTreeComponent.treeControl.isExpanded(flatItem) = ;
    this.folderTreeComponent.showItem(flatItem);
    // console.log(item);
    // this.onFolderClick(item);
  }

  handleFileInput(files) {
    //   for (let i = 0; i < files.length; i++) {
    //     const file = files[i];
    //     const path = file.webkitRelativePath.split('/');
    //   }
    // }
    console.log(files);
    this.filenames = Object['values'](files).map(v => v.name);
    this.uploadPaths = [...this.uploadPaths, ...this.filenames];
  }

  filesPicked(files) {
    console.log(files);
    // this.uploadPaths = [];
    Array.prototype.forEach.call(files, file => {
      this.uploadPaths.push(file.webkitRelativePath);
    });
    console.log(this.uploadPaths);
  }

  onFolderClick(event) {
    this.selectedFolder = event.element1;
    this.folderItems = event.element ? event.element.children : event.children;
    this.path = event.path;

  }

  private upload(tree, path) {
    tree.files.forEach(file => {
      this.uploadPaths.push(path + file.name);
    });
    tree.directories.forEach(directory => {
      const newPath = path + directory.name + '/';
      this.uploadPaths.push(newPath);
      this.upload(directory, newPath);
    });
  }

  deleteAddedFile(file) {
    this.uploadPaths.splice(this.uploadPaths.indexOf(file), 1);
  }

  uploadItems(){
    // this.folderTreeComponent.addNewItem(this.selectedFolder);
    this.uploadPaths.forEach(v => {
      this.folderTreeComponent.addNewItem(this.selectedFolder, v, true);
    });
    this.uploadPaths = [];
    // this.folderTreeComponent.saveNode(this.selectedFolder);

  }
}

// @Component({
//   selector: 'dialog-result-example-dialog',
//   template: `
//   <div class="dialog-header">
//     <h4 class="modal-title"><i class="material-icons">priority_high</i>Warning</h4>
//     <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
//   </div>
//   <div mat-dialog-content class="content">
//     <p>Account <span class="strong">{{ data.hostname }}</span> will be decommissioned.</p>
//     <p class="m-top-20"><span class="strong">Do you want to proceed?</span></p>
//   </div>
//   <div class="text-center">
//     <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
//     <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
//   </div>
//   `,
//   styles: [`
//     .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400 }
//   `]
// })
// export class ConfirmDeleteAccountDialog {
//   constructor(
//     public dialogRef: MatDialogRef<ConfirmDeleteAccountDialog>,
//     @Inject(MAT_DIALOG_DATA) public data: any
//   ) { }
// }
