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
import {BucketDataService} from './bucket-data.service';

@Component({
  selector: 'dlab-bucket-browser',
  templateUrl: './bucket-browser.component.html',
  styleUrls: ['./bucket-browser.component.scss']
})
export class BucketBrowserComponent implements OnInit {
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
    private bucketDataService: BucketDataService,
    private formBuilder: FormBuilder
  ) {

  }

  ngOnInit() {
    setTimeout(() => this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint), 3000);
    // this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
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
    if (event.target.files.length > 0) {
      const file = event.target.files[0];
      this.uploadForm.get('file').setValue(file);
      const newAddedFiles = Object['values'](event.target.files).map(v => (
        {item: v.name, 'size': (v.size / 1048576).toFixed(2)} as unknown as TodoItemNode
      ));
      this.addedFiles = [...this.addedFiles, ...newAddedFiles];
    }
  }


  public toggleSelectedFile(file) {
   // remove if when will be possible download several files
    if (!file.isSelected) {
      this.folderItems.forEach(item => item.isSelected = false);
    }
    file.isSelected = !file.isSelected;
    this.selected = this.folderItems.filter(item => item.isSelected);
  }

  filesPicked(files) {
    // console.log(files);

    Array.prototype.forEach.call(files, file => {
      this.addedFiles.push(file.webkitRelativePath);
    });

  }

  public onFolderClick(event) {
    this.selectedFolder = event.flatNode;
    this.folderItems = event.element ? event.element.children : event.children;
    const folders = this.folderItems.filter(v => v.children).sort((a, b) => a.item > b.item ? 1 : -1);
    const files = this.folderItems.filter(v => !v.children).sort((a, b) => a.item > b.item ? 1 : -1);
    this.folderItems = [...folders, ...files];
    this.path = event.path;
    this.pathInsideBucket = this.path.indexOf('/') !== -1 ?  this.path.slice(this.path.indexOf('/') + 1) + '/' : '';
    this.bucketName = this.path.substring(0, this.path.indexOf('/')) || this.path;
    this.folderItems.forEach(item => item.isSelected = false);
  }

  public deleteAddedFile(file) {
    this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
  }

  private uploadNewFile() {
    const path = `${this.pathInsideBucket}${this.uploadForm.get('file').value.name}`;

    const formData = new FormData();
    formData.append('file', this.uploadForm.get('file').value);
    formData.append('object', path);
    formData.append('bucket', this.bucketName);
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
        this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
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
      this.bucketBrowserService.downloadFile(`/${this.bucketName}/object/${path}/endpoint/${this.endpoint}/download`).subscribe(data =>  {
        FileUtils.downloadBigFiles(data, this.selected[0].item);
        this.folderItems.forEach(item => item.isSelected = false);
        }, error => this.toastr.error(error.message || 'File downloading error!', 'Oops!')
      );
    }

    if (action === 'delete') {
      this.bucketBrowserService.deleteFile(`/${this.bucketName}/object/${path}/endpoint/${this.endpoint}`).subscribe(() => {
        this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
          this.toastr.success('File successfully deleted!', 'Success!');
        this.folderItems.forEach(item => item.isSelected = false);
        }, error => this.toastr.error(error.message || 'File deleting error!', 'Oops!')
      );
    }
  }

}



