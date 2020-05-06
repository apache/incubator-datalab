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
import { FormGroup, FormBuilder } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { ManageUngitService } from '../../core/services';

import {FolderTreeComponent} from './folder-tree/folder-tree.component';
import {BucketBrowserService, TodoItemNode} from '../../core/services/bucket-browser.service';
import {FileUtils} from '../../core/util';
import {BucketDataService} from './bucket-data.service';
import {ConfirmationDialogComponent, ConfirmationDialogType} from '../../shared/modal-dialog/confirmation-dialog';
import {BucketConfirmationDialogComponent} from './bucket-confirmation-dialog/bucket-confirmation-dialog.component';

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
  public isUploadWindowOpened = false;

  @ViewChild(FolderTreeComponent, {static: true}) folderTreeComponent;
  public selectedFolder: any;
  public selectedFolderForAction: any;
  public selected: any[];
  public bucketStatus;
  public allDisable: boolean;
  public folders: any[];
  private selectedItems;


  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<BucketBrowserComponent>,
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
    private bucketBrowserService: BucketBrowserService,
    private bucketDataService: BucketDataService,
  ) {

  }

  ngOnInit() {
    this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
    this.endpoint = this.data.endpoint;
    this.bucketStatus = this.data.bucketStatus;
  }

  public showItem(item) {
    const flatItem = this.folderTreeComponent.nestedNodeMap.get(item);
    this.folderTreeComponent.showItem(flatItem);
  }

  public handleFileInput(event) {
    if (event.target.files.length > 0) {
      const newAddedFiles = Object['values'](event.target.files).map(file => (
        {name: file['name'], file: file, 'size': (file['size'] / 1048576).toFixed(2), path: this.path, isUploading: false, uploaded: false, errorUploading: false}));
      this.addedFiles = [...this.addedFiles, ...newAddedFiles];
    }
    event.target.value = '';
  }

  public closeUploadWindow() {
    // this.isUploadWindowOpened = false;
    this.addedFiles = [];

  }


  public toggleSelectedFile(file, type) {
  type === 'file' ?  file.isSelected = !file.isSelected : file.isFolderSelected = !file.isFolderSelected;
  this.selected = this.folderItems.filter(item => item.isSelected);
  this.selectedFolderForAction = this.folderItems.filter(item => item.isFolderSelected);
  this.selectedItems = [...this.selected, ...this.selectedFolderForAction];
  }

  filesPicked(files) {
    Array.prototype.forEach.call(files, file => {
      this.addedFiles.push(file.webkitRelativePath);
    });
  }

  public dissableAll(event) {
    this.allDisable = event;
  }

  public onFolderClick(event) {
    this.clearSelection();
    this.selectedFolder = event.flatNode;
    this.folderItems = event.element ? event.element.children : event.children;
    if (this.folderItems) {
      this.folders = this.folderItems.filter(v => v.children).sort((a, b) => a.item > b.item ? 1 : -1);
      const files = this.folderItems.filter(v => !v.children).sort((a, b) => a.item > b.item ? 1 : -1);
      this.folderItems = [...this.folders, ...files];
      this.path = event.path;
      this.pathInsideBucket = this.path.indexOf('/') !== -1 ?  this.path.slice(this.path.indexOf('/') + 1) + '/' : '';
      this.bucketName = this.path.substring(0, this.path.indexOf('/')) || this.path;
      this.folderItems.forEach(item => item.isSelected = false);
    }
  }

  private clearSelection() {
    this.folderItems.forEach(item => item.isSelected = false);
    this.folderItems.forEach(item => item.isFolderSelected = false);
    this.selected = this.folderItems.filter(item => item.isSelected);
    this.selectedFolderForAction = this.folderItems.filter(item => item.isFolderSelected);
  }

  public deleteAddedFile(file) {
    this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
  }

  private uploadNewFile(file) {
    const path = file.path.indexOf('/') !== -1 ?  this.path.slice(this.path.indexOf('/') + 1) : '';
    const fullPath = path ? `${path}/${file.name}` : file.name;
    const formData = new FormData();
    formData.append('file', file.file);
    formData.append('object', fullPath);
    formData.append('bucket', this.bucketName);
    formData.append('endpoint', this.endpoint);
    file.isUploading = true;
    this.bucketBrowserService.uploadFile(formData)
      .subscribe(() => {
        this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
        file.isUploading = false;
        file.uploaded = true;
        this.toastr.success('File successfully uploaded!', 'Success!');
      }, error => {
        this.toastr.error(error.message || 'File uploading error!', 'Oops!');
        file.errorUploading = true;
        file.isUploading = false;
      }
    );
  }

  public createFolder(folder) {
    this.allDisable = true;
    this.folderTreeComponent.addNewItem(folder, '', false);
  }

  public fileAction(action) {
    // this.selected = this.folderItems.filter(item => item.isSelected);
    const selected = this.folderItems.filter(item => item.isSelected);
    const folderSelected = this.folderItems.filter(item => item.isFolderSelected);

    if (action === 'download') {
      const path = encodeURIComponent(`${this.pathInsideBucket}${this.selected[0].item}`);
      selected[0]['isDownloading'] = true;
      this.bucketBrowserService.downloadFile(`/${this.bucketName}/object/${path}/endpoint/${this.endpoint}/download`)
        .subscribe(data =>  {
        FileUtils.downloadBigFiles(data, selected[0].item);
        selected[0]['isDownloading'] = false;
        this.folderItems.forEach(item => item.isSelected = false);
        }, error => {
            this.toastr.error(error.message || 'File downloading error!', 'Oops!');
            selected[0]['isDownloading'] = false;
          }
        );
    }

    if (action === 'delete') {
      const itemsForDeleting = [...folderSelected, ...selected];
      const objects = itemsForDeleting.map(obj => obj.object.object);
      const dataForServer = [];
      objects.forEach(object => {
        dataForServer.push(...this.bucketDataService.serverData.map(v => v.object).filter(v => v.indexOf(object) === 0));
      });
      const url = `/${this.bucketName}/endpoint/${this.endpoint}/delete`;
      console.log(dataForServer);
      this.dialog.open(BucketConfirmationDialogComponent, {data: {items: itemsForDeleting} , width: '550px'})
        .afterClosed().subscribe((res) => {
        !res && this.clearSelection();
        res && this.bucketBrowserService.deleteFile(dataForServer, url).subscribe(() => {
            this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
            this.toastr.success('File successfully deleted!', 'Success!');
            this.clearSelection();
          }, error => {
          this.toastr.error(error.message || 'File deleting error!', 'Oops!');
          this.clearSelection();
        });
      });


    }
  }
}



