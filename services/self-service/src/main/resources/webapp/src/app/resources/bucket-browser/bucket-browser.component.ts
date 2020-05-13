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
import {BucketConfirmationDialogComponent} from './bucket-confirmation-dialog/bucket-confirmation-dialog.component';
import {logger} from 'codelyzer/util/logger';

@Component({
  selector: 'dlab-bucket-browser',
  templateUrl: './bucket-browser.component.html',
  styleUrls: ['./bucket-browser.component.scss']
})
export class BucketBrowserComponent implements OnInit {
  public addedFiles = [];
  public folderItems = [];
  public originFolderItems = [];
  public objectPath;
  public path = '';
  public pathInsideBucket = '';
  public bucketName = '';
  public endpoint = '';
  public isUploadWindowOpened = false;
  public selectedFolder: any;
  public selectedFolderForAction: any;
  public selected: any[];
  public bucketStatus;
  public allDisable: boolean;
  public isActionsOpen: boolean;
  public folders: any[];
  public selectedItems;
  public searchValue: string;

  @ViewChild(FolderTreeComponent, {static: true}) folderTreeComponent;
  public isSelectionOpened: any;
  isFilterVisible: boolean;



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

  public async handleFileInput(event) {
    if (event.target.files.length > 0) {
      let askForAll = true;
      let skipAll = false;

      const folderFiles = this.folderItems.filter(v => !v.children).map(v => v.item);
      for (const file of  Object['values'](event.target.files)) {
      const existFile = folderFiles.filter(v => v === file['name'])[0];
        const uploadItem = {
          name: file['name'],
          file: file,
          'size': (file['size'] / 1048576).toFixed(2),
          path: this.path,
          isUploading: false,
          uploaded: false,
          errorUploading: false
        };
        if (existFile && askForAll) {
          const result = await this.openResolveDialog(existFile);
          if (result) {
            askForAll = !result.forAll;
            if (result.forAll && !result.replaceObject) {
              skipAll = true;
            }
            if (result.replaceObject) {
              this.addedFiles.push(uploadItem);
              this.uploadNewFile(uploadItem);
            }
          }
        } else if (!existFile || (existFile && !askForAll && !skipAll)) {
          this.addedFiles.push(uploadItem);
          this.uploadNewFile(uploadItem);
        }
        }
    }
    event.target.value = '';
    setTimeout(() => {
      const element = document.querySelector('#upload-list');
      element && element.scrollIntoView({ block: 'end', behavior: 'smooth' });
    }, 0);
  }

  async openResolveDialog(existFile) {
     const dialog = this.dialog.open(BucketConfirmationDialogComponent, {
       data: {items: existFile, type: 'upload-dublicat'} , width: '550px'
     });
     return dialog.afterClosed().toPromise().then(result => {
      return Promise.resolve(result);
    });
  }

  public closeUploadWindow() {
    this.addedFiles = [];
  }


  public toggleSelectedFile(file, type) {
  type === 'file' ?  file.isSelected = !file.isSelected : file.isFolderSelected = !file.isFolderSelected;
  this.selected = this.folderItems.filter(item => item.isSelected);
  this.selectedFolderForAction = this.folderItems.filter(item => item.isFolderSelected);
  this.selectedItems = [...this.selected, ...this.selectedFolderForAction];
  this.isActionsOpen = false;

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
    this.searchValue = '';
    this.clearSelection();
    this.selectedFolder = event.flatNode;
    this.folderItems = event.element ? event.element.children : event.children;
    if (this.folderItems) {
      this.folders = this.folderItems.filter(v => v.children).sort((a, b) => a.item > b.item ? 1 : -1);
      const files = this.folderItems.filter(v => !v.children).sort((a, b) => a.item > b.item ? 1 : -1);
      this.folderItems = [...this.folders, ...files];
      this.objectPath = event.pathObject;
      this.path = event.path;
      this.originFolderItems = this.folderItems.map(v => v);
      this.pathInsideBucket = this.path.indexOf('/') !== -1 ?  this.path.slice(this.path.indexOf('/') + 1) + '/' : '';
      this.bucketName = this.path.substring(0, this.path.indexOf('/')) || this.path;
      this.folderItems.forEach(item => item.isSelected = false);
    }
  }

  public filterObjects() {
    this.folderItems = this.originFolderItems.filter(v => v.item.indexOf(this.searchValue) !== -1);
  }

  private clearSelection() {
    this.folderItems.forEach(item => item.isSelected = false);
    this.folderItems.forEach(item => item.isFolderSelected = false);
    this.selected = this.folderItems.filter(item => item.isSelected);
    this.selectedFolderForAction = this.folderItems.filter(item => item.isFolderSelected);
    this.selectedItems = [];
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
        // this.toastr.success('File successfully uploaded!', 'Success!');
      }, error => {
        // this.toastr.error(error.message || 'File uploading error!', 'Oops!');
        file.errorUploading = true;
        file.isUploading = false;
      }
    );
  }

  public refreshBucket() {
    this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
  }

  public createFolder(folder) {
    this.allDisable = true;
    this.folderTreeComponent.addNewItem(folder, '', false);
  }

  public fileAction(action) {
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

      this.dialog.open(BucketConfirmationDialogComponent, {data: {items: itemsForDeleting, type: 'delete'} , width: '550px'})
        .afterClosed().subscribe((res) => {
        !res && this.clearSelection();
        res && this.bucketBrowserService.deleteFile({
          bucket: this.bucketName, endpoint: this.endpoint, 'objects': dataForServer
        }).subscribe(() => {
            this.bucketDataService.refreshBucketdata(this.data.bucket, this.data.endpoint);
            this.toastr.success('Objects successfully deleted!', 'Success!');
            this.clearSelection();
          }, error => {
          this.toastr.error(error.message || 'Objects deleting error!', 'Oops!');
          this.clearSelection();
        });
      });


    }
  }

  public toogleActions() {
    this.isActionsOpen = !this.isActionsOpen;
  }

  public closeActions() {
    this.isActionsOpen = false;
  }

  public copyPath() {
    const selBox = document.createElement('textarea');
    const selected = this.folderItems.filter(item => item.isSelected || item.isFolderSelected)[0];
    selBox.style.position = 'fixed';
    selBox.style.left = '0';
    selBox.style.top = '0';
    selBox.style.opacity = '0';
    selBox.value = selected.object.bucket + '/' + selected.object.object;
    document.body.appendChild(selBox);
    selBox.focus();
    selBox.select();
    document.execCommand('copy');
    document.body.removeChild(selBox);
    this.clearSelection();
    this.isActionsOpen = false;
    this.toastr.success('Object path successfully copied!', 'Success!');
  }

  public toggleBucketSelection() {
    this.isSelectionOpened = !this.isSelectionOpened;
  }

  public closeFilterInput() {
    this.isFilterVisible = false;
    this.searchValue = '';
    this.filterObjects();
  }
}



