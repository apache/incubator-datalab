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
import {ApplicationSecurityService, ManageUngitService} from '../../core/services';

import {FolderTreeComponent} from './folder-tree/folder-tree.component';
import {BucketBrowserService, TodoItemNode} from '../../core/services/bucket-browser.service';
import {FileUtils} from '../../core/util';
import {BucketDataService} from './bucket-data.service';
import {BucketConfirmationDialogComponent} from './bucket-confirmation-dialog/bucket-confirmation-dialog.component';
import {logger} from 'codelyzer/util/logger';
import {HttpEventType, HttpResponse} from '@angular/common/http';
import {CopyPathUtils} from '../../core/util/copyPathUtils';

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
  public selectedFolder: any;
  public selectedFolderForAction: any;
  public selected: any[];
  public bucketStatus;
  public allDisable: boolean;
  public isActionsOpen: boolean;
  public folders: any[];
  public selectedItems;
  public searchValue: string;
  public isQueueFull: boolean;
  date = new Date(2020, 2, 2, 10, 57, 2);

  @ViewChild(FolderTreeComponent, {static: true}) folderTreeComponent;
  public isSelectionOpened: any;
  isFilterVisible: boolean;
  public buckets;
  private isFileUploading: boolean;
  private isFileUploaded: boolean;




  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<BucketBrowserComponent>,
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
    private bucketBrowserService: BucketBrowserService,
    private bucketDataService: BucketDataService,
    private auth: ApplicationSecurityService
  ) {

  }

  ngOnInit() {
    this.bucketName = this.data.bucket;
    this.bucketDataService.refreshBucketdata(this.bucketName, this.data.endpoint);
    this.endpoint = this.data.endpoint;
    this.bucketStatus = this.data.bucketStatus;
    this.buckets = this.data.buckets;
  }

  public showItem(item) {
    const flatItem = this.folderTreeComponent.nestedNodeMap.get(item);
    this.folderTreeComponent.showItem(flatItem);
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

  public handleFileInput(event) {
    const fullFilesList = Object['values'](event.target.files);
    if (fullFilesList.length > 0) {
      const files = fullFilesList.filter(v => v.size < 4294967296);
      const toBigFile = fullFilesList.length !== files.length;
      const toMany = files.length > 50;
      if (files.length > 50) {
        files.length = 50;
      }
      if (toBigFile || toMany) {
        this.dialog.open(BucketConfirmationDialogComponent, {data: {
          items: {toBig: toBigFile, toMany: toMany}, type: 'uploading-error'
          } , width: '550px'})
          .afterClosed().subscribe((res) => {
            res && this.uploadingQueue(files);
        });
      } else {
        this.uploadingQueue(files);
      }
    }
    event.target.value = '';
  }

  private async uploadingQueue(files) {
    if (files.length) {
      let askForAll = true;
      let skipAll = false;
      // this.auth.refreshToken().subscribe();
      const folderFiles = this.folderItems.filter(v => !v.children).map(v => v.item);
      for (const file of files) {
        const existFile = folderFiles.filter(v => v === file['name'])[0];
        const uploadItem = {
          name: file['name'],
          file: file,
          size: file.size,
          path: this.path,
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

  public onFolderClick(event) {
    this.searchValue = '';
    this.clearSelection();
    this.selectedFolder = event.flatNode;
    if (this.isSelectionOpened) {
      this.isSelectionOpened = false;
    }
    this.folderItems = event.element ? event.element.children : event.children;
    if (this.folderItems) {
      this.folders = this.folderItems.filter(v => v.children).sort((a, b) => a.item > b.item ? 1 : -1);
      const files = this.folderItems.filter(v => !v.children).sort((a, b) => a.item > b.item ? 1 : -1);
      this.folderItems = [...this.folders, ...files];
      this.objectPath = event.pathObject;
      this.path = event.path;
      this.originFolderItems = this.folderItems.map(v => v);
      this.pathInsideBucket = this.path.indexOf('/') !== -1 ?  this.path.slice(this.path.indexOf('/') + 1) + '/' : '';
      this.folderItems.forEach(item => item.isSelected = false);
    }
  }

  public filterObjects() {
    this.folderItems = this.originFolderItems.filter(v => v.item.toLowerCase().indexOf(this.searchValue.toLowerCase()) !== -1);
  }

  private clearSelection() {
    this.folderItems.forEach(item => item.isSelected = false);
    this.folderItems.forEach(item => item.isFolderSelected = false);
    this.selected = this.folderItems.filter(item => item.isSelected);
    this.selectedFolderForAction = this.folderItems.filter(item => item.isFolderSelected);
    this.selectedItems = [];
  }

  public deleteAddedFile(file) {
    if ( file.subscr && file.request) {
      this.dialog.open(BucketConfirmationDialogComponent, {data: {items: file, type: 'cancel-uploading'} , width: '550px'})
        .afterClosed().subscribe((res) => {
          res && file.subscr.unsubscribe();
          res && this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
          this.isFileUploading = !!this.addedFiles.filter(v => v.status === 'uploading').length;
          this.sendFile();
      }, () => {
        this.isFileUploading = !!this.addedFiles.filter(v => v.status === 'uploading').length;
        this.sendFile();
      });
    } else {
      this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
      this.isFileUploading = !!this.addedFiles.filter(v => v.status === 'uploading').length;
      this.sendFile();
    }

  }

  private uploadNewFile(file) {
    const path = file.path.indexOf('/') !== -1 ?  this.path.slice(this.path.indexOf('/') + 1) : '';
    const fullPath = path ? `${path}/${file.name}` : file.name;
    const formData = new FormData();
    formData.append('file', file.file);
    formData.append('size', file.file.size);
    formData.append('object', fullPath);
    formData.append('bucket', this.bucketName);
    formData.append('endpoint', this.endpoint);
    file.status = 'waiting';
    file.request = this.bucketBrowserService.uploadFile(formData);
    this.sendFile(file);
  }

  public sendFile(file?) {
    const waitUploading = this.addedFiles.filter(v => v.status === 'waiting');
    const uploading = this.addedFiles.filter(v => v.status === 'uploading');
    this.isQueueFull = !!waitUploading.length;
    this.isFileUploading = !!this.addedFiles.filter(v => v.status === 'uploading').length;
    if (waitUploading.length && uploading.length < 10) {
      if (!file) {
        file = waitUploading[0];
      }
      file.status = 'uploading';
      this.isQueueFull = !!this.addedFiles.filter(v => v.status === 'waiting').length;
      file.subscr =  file.request.subscribe((event: any) => {
          if (event.type === HttpEventType.UploadProgress) {
             file.progress = Math.round(95 * event.loaded / event.total);
            if (file.progress === 95 && !file.interval) {
              file.interval = setInterval(() => {
                if (file.progress < 99) {
                  return file.progress++;
                }
              }, file.size < 1094967296 ? 12000 : 20000);
            }
          } else if (event['type'] === HttpEventType.Response) {
            window.clearInterval(file.interval);
            file.status = 'uploaded';
            delete file.request;
            this.sendFile(this.addedFiles.filter(v => v.status === 'waiting')[0]);
            this.bucketDataService.refreshBucketdata(this.bucketName, this.data.endpoint);
          }
        }, error => {
        window.clearInterval(file.interval);
          file.status = 'failed';
          delete file.request;
          this.sendFile(this.addedFiles.filter(v => v.status === 'waiting')[0]);
        }
      );
    }

  }

  public refreshBucket() {
    this.path = '';
    this.bucketDataService.refreshBucketdata(this.bucketName, this.data.endpoint);
    this.isSelectionOpened = false;
  }

  public openBucket($event) {
    this.bucketName = $event.name;
    this.path = '';
    this.bucketDataService.refreshBucketdata(this.bucketName, $event.endpoint);
    this.isSelectionOpened = false;
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
        .subscribe(event =>  {
            if (event['type'] === HttpEventType.DownloadProgress) {
              selected[0].progress = Math.round(100 * event['loaded'] / selected[0].object.size);
            }
            if (event['type'] === HttpEventType.Response) {
              FileUtils.downloadBigFiles(event['body'], selected[0].item);
              setTimeout(() => {
                selected[0]['isDownloading'] = false;
                selected[0].progress = 0;
              }, 1000);

              this.folderItems.forEach(item => item.isSelected = false);
            }

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
    const selected = this.folderItems.filter(item => item.isSelected || item.isFolderSelected)[0];
    CopyPathUtils.copyPath(selected.object.bucket + '/' + selected.object.object);
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

  public convertDate(date) {
    const utcDate = new Date(date);
    return new Date(utcDate.setTime( utcDate.getTime() - utcDate.getTimezoneOffset() * 60 * 1000 ));
  }

  // public toggleFileUploaded($event: any) {
  //   this.isFileUploaded = $event;
  // }
}



