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
import {FileUtils} from '../../core/util';

@Component({
  selector: 'dlab-bucket-browser',
  templateUrl: './bucket-browser.component.html',
  styleUrls: ['./bucket-browser.component.scss']
})
export class BucketBrowserComponent implements OnInit {
  public filenames: Array<any> = [];
  public addedFiles = [];
  public folderItems = [];
  public path = '';
  public pathInsideBucket = '';
  public bucketName = '';
  public endpoint = '';

  @ViewChild(FolderTreeComponent, {static: true}) folderTreeComponent;
  public selectedFolder: any;
  private isUploading: boolean;
  private selected: any[];
  private uploadForm: FormGroup;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<BucketBrowserComponent>,
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
    private bucketBrowserService: BucketBrowserService,
    private formBuilder: FormBuilder
  ) {

  }

  ngOnInit() {
    // this.bucketBrowserService.getBacketData();
    this.endpoint = this.data.endpoint;
    this.uploadForm = this.formBuilder.group({
      file: ['']
    });
  }

  public showItem(item) {
    const flatItem = this.folderTreeComponent.nestedNodeMap.get(item);
    this.folderTreeComponent.showItem(flatItem);
  }

  public handleFileInput(event) {
    //   for (let i = 0; i < files.length; i++) {
    //     const file = files[i];
    //     const path = file.webkitRelativePath.split('/');
    //   }
    // }
    if (event.target.files.length > 0) {
      const file = event.target.files[0];
      this.uploadForm.get('file').setValue(file);
      this.filenames = Object['values'](event.target.files).map(v => ({item: v.name, 'size': (v.size / 1048576).toFixed(2)} as unknown as TodoItemNode));
      this.addedFiles = [...this.addedFiles, ...this.filenames];
    }

  }

  public toggleSelectedFile(file) {
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

  public onFolderClick(event) {
    this.selectedFolder = event.flatNode;
    this.folderItems = event.element ? event.element.children : event.children;
    // this.folderItems = this.folderItems.sort((a, b) => (a.children > b.children) ? 1 : -1)
    // console.log(this.folderItems);
    this.path = event.path;
    this.pathInsideBucket = this.path.indexOf('/') !== -1 ?  this.path.slice(this.path.indexOf('/') + 1) + '/' : '';
    this.bucketName = this.path.substring(0, this.path.indexOf('/')) || this.path;
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

  public deleteAddedFile(file) {
    this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
  }

  private uploadNewFile() {
    const path = `${this.pathInsideBucket}${this.uploadForm.get('file').value.name}`;

    const formData = new FormData();
    formData.append('file', this.uploadForm.get('file').value);
    formData.append('object', path);
    formData.append('bucket', 'ofuks-1304-prj1-local-bucket');
    formData.append('endpoint', this.endpoint);
    // file.inProgress = true;
    this.isUploading = true;
    this.bucketBrowserService.uploadFile(formData)
      // .pipe(
      // map(event => {
      //   switch (event.type) {
      //     case HttpEventType.UploadProgress:
      //       file.progress = Math.round(event.loaded * 100 / event.total);
      //       break;
      //     case HttpEventType.Response:
      //       return event;
      //   }
      // }),
      // catchError((error: HttpErrorResponse) => {
      //   file.inProgress = false;
      //   return of(`${file.name} upload failed.`);
      // }))
      .subscribe((event: any) => {
      //   if (typeof (event) === 'object') {
      //     console.log(event.body);
      //   }
      // this.isUploading = false;
        this.bucketBrowserService.initialize();
        this.addedFiles = [];
        this.isUploading = false;
        this.toastr.success('File successfully uploaded!', 'Success!');
      }, error => this.toastr.error(error.message || 'File uploading error!', 'Oops!')
    );
  }

  public fileAction(action) {
    this.selected = this.folderItems.filter(item => item.isSelected);
    const path = encodeURIComponent(`${this.pathInsideBucket}${this.selected[0].item}`);
    if (action === 'download') {
      this.bucketBrowserService.downloadFile(`/${this.bucketName}/object/${path}/endpoint/${this.endpoint}/download`
      ).subscribe(data =>  {
        FileUtils.downloadFile(data);
        // this.downLoadFile(response, 'aplication/octet-stream');
          this.toastr.success('File downloading started!', 'Success!');
        }, error => this.toastr.error(error.message || 'File downloading error!', 'Oops!')
      );
    }

    if (action === 'delete') {
      this.bucketBrowserService.deleteFile(`/${this.bucketName}/object/${path}/endpoint/${this.endpoint}`).subscribe(() => {
        this.bucketBrowserService.initialize();
          this.toastr.success('File successfully deleted!', 'Success!');
        }, error => this.toastr.error(error.message || 'File deleting error!', 'Oops!')
      );
    }

    this.folderItems.forEach(item => item.isSelected = false);
    this.selected = this.folderItems.filter(item => item.isSelected);
  }

  // downLoadFile(data: any, type: string) {
  //   const blob = new Blob([data], { type: type});
  //   const url = window.URL.createObjectURL(blob);
  //   const pwa = window.open(url);
  //   if (!pwa || pwa.closed || typeof pwa.closed === 'undefined') {
  //     alert( 'Please disable your Pop-up blocker and try again.');
  //   }
  // }
}



