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

import { Component, OnInit, ViewChild, Inject, OnDestroy } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { ApplicationSecurityService, ManageUngitService, StorageService } from '../../core/services';

import { FolderTreeComponent } from './folder-tree/folder-tree.component';
import { BucketBrowserService } from '../../core/services/bucket-browser.service';
import { FileUtils, HelpUtils } from '../../core/util';
import { BucketDataService } from './bucket-data.service';
import { BucketConfirmationDialogComponent } from './bucket-confirmation-dialog/bucket-confirmation-dialog.component';
import { HttpEventType } from '@angular/common/http';
import { CopyPathUtils } from '../../core/util/copyPathUtils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'datalab-bucket-browser',
  templateUrl: './bucket-browser.component.html',
  styleUrls: ['./bucket-browser.component.scss', './upload-window.component.scss']
})
export class BucketBrowserComponent implements OnInit, OnDestroy {
  public readonly uploadingQueueLength: number = 4;
  public readonly maxFileSize: number = 4294967296;
  public readonly refreshTokenLimit = 1500000;

  private unsubscribe$ = new Subject();
  private isTokenRefreshing = false;
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
  public isSelectionOpened: any;
  public isFilterVisible: boolean;
  public buckets;
  public isFileUploading: boolean;
  public cloud: string;

  @ViewChild(FolderTreeComponent, { static: true }) folderTreeComponent;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<BucketBrowserComponent>,
    private manageUngitService: ManageUngitService,
    private _fb: FormBuilder,
    private bucketBrowserService: BucketBrowserService,
    public bucketDataService: BucketDataService,
    private auth: ApplicationSecurityService,
    private storage: StorageService,
  ) { }

  ngOnInit() {
    this.bucketName = this.data.bucket;
    this.endpoint = this.data.endpoint;
    this.bucketDataService.refreshBucketdata(this.bucketName, this.endpoint);
    this.bucketStatus = this.data.bucketStatus;
    this.buckets = this.data.buckets;
    this.cloud = this.getCloud();
    // this.cloud = 'azure';
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public getTokenValidTime(): number {
    const token = JSON.parse(atob(this.storage.getToken().split('.')[1]));
    return token.exp * 1000 - new Date().getTime();
  }

  private refreshToken(): void {
    this.isTokenRefreshing = true;
    this.auth.refreshToken()
      .pipe(
        takeUntil(this.unsubscribe$)
      )
      .subscribe(tokens => {
        this.storage.storeTokens(tokens);
        this.isTokenRefreshing = false;
        this.sendFile();
      });
  }

  public showItem(item): void {
    const flatItem = this.folderTreeComponent.nestedNodeMap.get(item);
    this.folderTreeComponent.showItem(flatItem);
  }

  public closeUploadWindow(): void {
    this.addedFiles = [];
  }

  public toggleSelectedFile(file, type): void {
    type === 'file'
      ? file.isSelected = !file.isSelected
      : file.isFolderSelected = !file.isFolderSelected;
    this.selected = this.folderItems.filter(item => item.isSelected);
    this.selectedFolderForAction = this.folderItems.filter(item => item.isFolderSelected);
    this.selectedItems = [...this.selected, ...this.selectedFolderForAction];
    this.isActionsOpen = false;
  }

  filesPicked(files): void {
    Array.prototype.forEach.call(files, file => {
      this.addedFiles.push(file.webkitRelativePath);
    });
  }

  public dissableAll(event): void {
    this.allDisable = event;
  }

  public handleFileInput(event): void {
    const fullFilesList = Object['values'](event.target.files);
    if (fullFilesList.length > 0) {
      const files = fullFilesList.filter(v => v.size < this.maxFileSize);
      const toBigFile = fullFilesList.length !== files.length;
      const toMany = files.length > 50;
      if (toMany) {
        files.length = 50;
      }

      if (toBigFile || toMany) {
        this.dialog.open(BucketConfirmationDialogComponent, {
          data: {
            items: { toBig: toBigFile, toMany: toMany }, type: 'upload_limitation'
          }, width: '550px'
        })
          .afterClosed().subscribe((res) => {
            if (res) {
              this.checkQueue(files);
            }
          });
      } else {
        this.checkQueue(files);
      }
    }
    event.target.value = '';
  }

  private checkQueue(files) {
    if (this.refreshTokenLimit > this.getTokenValidTime()) {
      this.isTokenRefreshing = true;
      this.auth.refreshToken()
        .pipe(
          takeUntil(this.unsubscribe$)
        )
        .subscribe(v => {
          this.uploadingQueue(files);
          this.isTokenRefreshing = false;
        });
    } else {
      this.uploadingQueue(files);
    }
  }

  private async uploadingQueue(files) {
    if (files.length) {
      let askForAll = true;
      let skipAll = false;

      const folderFiles = this.folderItems.reduce((existFiles, item) => {
        if (!item.children) {
          existFiles.push(item.item);
        }
        return existFiles;
      }, []);

      for (const file of files) {
        const existFile = folderFiles.find(v => v === file['name']);
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
    }, 10);
  }

  async openResolveDialog(existFile) {
    const dialog = this.dialog.open(BucketConfirmationDialogComponent, {
      data: { items: existFile, type: 'resolve_conflicts' }, width: '550px'
    });
    return dialog.afterClosed().toPromise().then(result => {
      return Promise.resolve(result);
    });
  }

  public onFolderClick(event): void {
    this.searchValue = '';
    this.clearSelection();
    this.selectedFolder = event.flatNode;
    if (this.isSelectionOpened) {
      this.isSelectionOpened = false;
    }
    this.folderItems = event.element ? event.element.children : event.children;
    if (this.folderItems) {
      this.folders = this.folderItems.filter(v => v.children);
      const files = this.folderItems.filter(v => !v.children).sort((a, b) => a.item > b.item ? 1 : -1);
      this.folderItems = [...this.folders, ...files];
      this.objectPath = event.pathObject;
      this.path = event.path;
      this.originFolderItems = this.folderItems.map(v => v);
      this.pathInsideBucket = this.path.indexOf('/') !== -1
        ? this.path.slice(this.path.indexOf('/') + 1) + '/'
        : '';
      this.folderItems.forEach(item => item.isSelected = false);
    }
  }

  public filterObjects(): void {
    this.folderItems = this.originFolderItems.filter(v => v.item.toLowerCase().indexOf(this.searchValue.toLowerCase()) !== -1);
  }

  private clearSelection(): void {
    this.folderItems.forEach(item => item.isSelected = false);
    this.folderItems.forEach(item => item.isFolderSelected = false);
    this.selected = this.folderItems.filter(item => item.isSelected);
    this.selectedFolderForAction = this.folderItems.filter(item => item.isFolderSelected);
    this.selectedItems = [];
  }

  public deleteAddedFile(file): void {
    if (file.subscr && file.request) {
      this.dialog.open(BucketConfirmationDialogComponent, { data: { items: file, type: 'cancel' }, width: '550px' })
        .afterClosed().subscribe((res) => {
          res && file.subscr.unsubscribe();
          res && this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
          this.isFileUploading = this.addedFiles.some(v => v.status === 'uploading');
          this.sendFile();
        }, () => {
          this.isFileUploading = this.addedFiles.some(v => v.status === 'uploading');
          this.sendFile();
        });
    } else {
      this.addedFiles.splice(this.addedFiles.indexOf(file), 1);
      this.isFileUploading = this.addedFiles.some(v => v.status === 'uploading');
      this.sendFile();
    }
  }

  private uploadNewFile(file): void {
    const path = file.path.indexOf('/') !== -1 ? this.path.slice(this.path.indexOf('/') + 1) : '';
    const fullPath = path ? `${path}/${file.name}` : file.name;
    const formData = new FormData();
    formData.append('size', file.file.size);
    formData.append('object', fullPath);
    formData.append('bucket', this.bucketName);
    formData.append('endpoint', this.endpoint);
    formData.append('file', file.file);
    file.status = 'waiting';

    file.request = this.bucketBrowserService.uploadFile(formData);
    this.sendFile(file);
  }

  public sendFile(file?): void {
    const waitUploading = this.addedFiles.filter(v => v.status === 'waiting');
    const uploading = this.addedFiles.filter(v => v.status === 'uploading');
    this.isQueueFull = !!waitUploading.length;
    this.isFileUploading = this.addedFiles.some(v => v.status === 'uploading');
    // console.log((this.getTokenValidTime() / 1000 / 60 ).toFixed(0) + ' minutes');
    if ((this.refreshTokenLimit > this.getTokenValidTime()) && !this.isTokenRefreshing) {
      this.refreshToken();
    }

    if (waitUploading.length && uploading.length < this.uploadingQueueLength) {
      if (!file) {
        file = waitUploading[0];
      }

      file.status = 'uploading';
      this.isFileUploading = this.addedFiles.some(v => v.status === 'uploading');
      this.isQueueFull = this.addedFiles.some(v => v.status === 'waiting');
      file.subscr = file.request
        .subscribe(
          (event: any) => {
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
              this.sendFile(this.addedFiles.find(v => v.status === 'waiting'));
              this.bucketDataService.refreshBucketdata(this.bucketName, this.endpoint);
            }
          },
          error => {
            window.clearInterval(file.interval);
            file.status = 'failed';
            delete file.request;
            this.sendFile(this.addedFiles.find(v => v.status === 'waiting'));
          }
        );
    }
  }

  public refreshBucket(): void {
    this.path = '';
    this.bucketDataService.refreshBucketdata(this.bucketName, this.endpoint);
    this.isSelectionOpened = false;
  }

  public openBucket($event): void {
    this.bucketName = $event.name;
    this.endpoint = $event.endpoint;
    this.path = '';
    this.bucketDataService.refreshBucketdata(this.bucketName, this.endpoint);
    this.isSelectionOpened = false;
    this.cloud = this.getCloud();
  }

  private getCloud(): string {
    return this.buckets.filter(v => v.children.some(bucket => {
      return bucket.name === this.bucketName;
    }))[0].cloud.toLowerCase();
  }

  public createFolder(folder): void {
    this.allDisable = true;
    this.folderTreeComponent.addNewItem(folder, '', false);
  }

  public fileAction(action): void {
    const selected = this.folderItems.filter(item => item.isSelected);
    const folderSelected = this.folderItems.filter(item => item.isFolderSelected);
    if (action === 'download') {
      this.clearSelection();
      this.isActionsOpen = false;
      const path = encodeURIComponent(`${this.pathInsideBucket}${selected[0].item}`);
      selected[0]['isDownloading'] = true;
      this.folderItems.forEach(item => item.isSelected = false);
      this.bucketBrowserService.downloadFile(`/${this.bucketName}/object/${path}/endpoint/${this.endpoint}/download`)
        .pipe(
          takeUntil(this.unsubscribe$)
        )
        .subscribe(
          event => {
            if (event['type'] === HttpEventType.DownloadProgress) {
              selected[0].progress = Math.round(100 * event['loaded'] / selected[0].object.size);
            }
            if (event['type'] === HttpEventType.Response) {
              FileUtils.downloadBigFiles(event['body'], selected[0].item);
              setTimeout(() => {
                selected[0]['isDownloading'] = false;
                selected[0].progress = 0;
              }, 1000);
            }
          },
          error => {
            this.toastr.error(error.message || 'File downloading error!', 'Oops!');
            selected[0]['isDownloading'] = false;
          }
        );
    }

    if (action === 'delete') {
      const itemsForDeleting = [...folderSelected, ...selected];
      const objects = itemsForDeleting.map(obj => obj.object.object);
      let dataForServer = [];
      objects.forEach(object => {
        dataForServer.push(...this.bucketDataService.serverData.map(v => v.object).filter(v => v.indexOf(object) === 0));
      });
      dataForServer = [...dataForServer, ...objects].filter((v, i, arr) => i === arr.indexOf(v));
      this.dialog.open(BucketConfirmationDialogComponent, { data: { items: itemsForDeleting, type: 'delete' }, width: '550px' })
        .afterClosed().subscribe((res) => {
          !res && this.clearSelection();
          res && this.bucketBrowserService.deleteFile({
            bucket: this.bucketName, endpoint: this.endpoint, 'objects': dataForServer
          })
            .pipe(
              takeUntil(this.unsubscribe$)
            )
            .subscribe(() => {
              this.bucketDataService.refreshBucketdata(this.bucketName, this.endpoint);
              this.toastr.success('Objects successfully deleted!', 'Success!');
              this.clearSelection();
            }, error => {
              this.toastr.error(error.message || 'Objects deleting error!', 'Oops!');
              this.clearSelection();
            });
        });

    }
  }

  public toogleActions(): void {
    this.isActionsOpen = !this.isActionsOpen;
  }

  public closeActions(): void {
    this.isActionsOpen = false;
  }

  public copyPath(): void {
    const selected = this.folderItems.filter(item => item.isSelected || item.isFolderSelected)[0];
    const pathToItem = `${this.pathInsideBucket}${selected.item}${selected.isFolderSelected ? '/' : ''}`;

    const cloud = this.getCloud();
    const protocol = HelpUtils.getBucketProtocol(cloud);
    if (cloud !== 'azure') {
      CopyPathUtils.copyPath(protocol + selected.object.bucket + '/' + pathToItem);
    } else {
      const bucketName = selected.object.bucket;
      const accountName = this.bucketName.replace(selected.object.bucket, '').slice(0, -1);
      const azureBucket = bucketName + '@' + accountName + '.blob.core.windows.net' + '/' + pathToItem;
      CopyPathUtils.copyPath(protocol + azureBucket);
    }
    this.clearSelection();
    this.isActionsOpen = false;
    this.toastr.success('Object path successfully copied!', 'Success!');
  }

  public toggleBucketSelection(): void {
    this.isSelectionOpened = !this.isSelectionOpened;
  }

  public closeFilterInput(): void {
    this.isFilterVisible = false;
    this.searchValue = '';
    this.filterObjects();
  }
}
