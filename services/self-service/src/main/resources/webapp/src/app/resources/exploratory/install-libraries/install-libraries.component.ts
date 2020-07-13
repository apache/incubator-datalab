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


import {Component, OnInit, ViewChild, ViewEncapsulation, ChangeDetectorRef, Inject, OnDestroy} from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormControl } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { debounceTime } from 'rxjs/operators';

import { InstallLibrariesModel } from './install-libraries.model';
import { LibrariesInstallationService } from '../../../core/services';
import { SortUtils, HTTP_STATUS_CODES } from '../../../core/util';
import {FilterLibsModel} from './filter-libs.model';


@Component({
  selector: 'install-libraries',
  templateUrl: './install-libraries.component.html',
  styleUrls: ['./install-libraries.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class InstallLibrariesComponent implements OnInit, OnDestroy {

  public model: InstallLibrariesModel;
  public notebook: any;
  public filteredList: any;
  public groupsList: Array<string>;
  public notebookLibs: Array<any> = [];
  public notebookFailedLibs: Array<any> = [];
  public loadLibsTimer: any;

  public query: string = '';
  public group: string;
  public destination: any;
  public uploading: boolean = false;
  public libs_uploaded: boolean = false;
  public validity_format: string = '';

  public isInstalled: boolean = false;
  public isInSelectedList: boolean = false;
  public installingInProgress: boolean = false;
  public libSearch: FormControl = new FormControl();
  public groupsListMap = {
    'r_pkg': 'R packages',
    'pip2': 'Python 2',
    'pip3': 'Python 3',
    'os_pkg': 'Apt/Yum',
    'others': 'Others',
    'java': 'Java'
  };

  private readonly CHECK_GROUPS_TIMEOUT: number = 5000;
  private clear: number;

  public filterConfiguration: FilterLibsModel = new FilterLibsModel('', [], [], [], []);
  public filterModel: FilterLibsModel = new FilterLibsModel('', [], [], [], []);
  public filtered: boolean;
  public filtredNotebookLibs: Array<any> = [];

  @ViewChild('groupSelect', { static: false }) group_select;
  @ViewChild('resourceSelect', { static: false }) resource_select;
  public isLibInfoOpened = {  };

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialog: MatDialog,
    public dialogRef: MatDialogRef<InstallLibrariesComponent>,
    private librariesInstallationService: LibrariesInstallationService,
    private changeDetector: ChangeDetectorRef
  ) {
    this.model = InstallLibrariesModel.getDefault(librariesInstallationService);
  }

  ngOnInit() {
    this.open(this.data);
    this.uploadLibGroups();
    this.libSearch.valueChanges.pipe(
      debounceTime(1000))
      .subscribe(newValue => {
        this.query = newValue || '';
        this.filterList();
      });
    this.getInstalledLibsByResource();
  }

  ngOnDestroy() {
    window.clearTimeout(this.loadLibsTimer);
    window.clearTimeout(this.clear);
  }

  uploadLibGroups(): void {
    this.libs_uploaded = false;
    this.uploading = true;
    this.librariesInstallationService.getGroupsList(this.notebook.project, this.notebook.name, this.model.computational_name)
      .subscribe(
        response => {
          this.libsUploadingStatus(response);
          this.changeDetector.detectChanges();

          this.resource_select && this.resource_select.setDefaultOptions(
            this.getResourcesList(),
            this.destination.title, 'destination', 'title', 'array');

          this.group_select && this.group_select.setDefaultOptions(
            this.groupsList, 'Select group', 'group_lib', null, 'list', this.groupsListMap);
        },
        error => this.toastr.error(error.message || 'Groups list loading failed!', 'Oops!'));
  }

  private getResourcesList() {
    this.notebook.type = 'EXPLORATORY';
    this.notebook.title = `${this.notebook.name} <em class="capt">notebook</em>`;
    return [this.notebook].concat(this.notebook.resources
      .filter(item => item.status === 'running')
      .map(item => {
        item['name'] = item.computational_name;
        item['title'] = `${item.computational_name} <em class="capt">cluster</em>`;
        item['type'] = 'СOMPUTATIONAL';
        return item;
      }));
  }

  public filterList(): void {
    this.validity_format = '';
    (this.query.length >= 2 && this.group) ? this.getFilteredList() : this.filteredList = null;
  }

  public filterGroups(groupsList) {
    const PREVENT_TEMPLATES = ['rstudio', 'rstudio with tensorflow'];
    const CURRENT_TEMPLATE = this.notebook.template_name.toLowerCase();
    const templateCheck = PREVENT_TEMPLATES.some(template => CURRENT_TEMPLATE.indexOf(template) !== -1);

    const filteredGroups = templateCheck ? groupsList.filter(group => group !== 'java') : groupsList;
    return SortUtils.libGroupsSort(filteredGroups);
  }

  public onUpdate($event) {
    if ($event.model.type === 'group_lib') {
      this.group = $event.model.value;
    } else if ($event.model.type === 'destination') {
      this.resetDialog();
      this.destination = $event.model.value;
      this.destination && this.destination.type === 'СOMPUTATIONAL'
        ? this.model.computational_name = this.destination.name
        : this.model.computational_name = null;

      this.uploadLibGroups();
      this.getInstalledLibsByResource();
    }
    this.filterList();
  }

  public onFilterUpdate($event) {
    this.filterModel[$event.type] = $event.model;
  }

  public isDuplicated(item) {
    const select = { group: this.group, name: item.name, version: item.version };

    this.isInSelectedList = this.model.selectedLibs.filter(el => JSON.stringify(el) === JSON.stringify(select)).length > 0;

    if (this.destination && this.destination.libs)
      this.isInstalled = this.destination.libs.findIndex(libr => {
        return select.group !== 'java'
          ? select.name === libr.name && select.group === libr.group && select.version === libr.version
          : select.name === libr.name && select.group === libr.group;
      }) >= 0;

    return this.isInSelectedList || this.isInstalled;
  }

  public selectLibrary(item): void {
    this.model.selectedLibs.push({ group: this.group, name: item.name, version: item.version });
    this.query = '';
    this.libSearch.setValue('');
    this.filteredList = null;
  }

  public removeSelectedLibrary(item): void {
    this.model.selectedLibs.splice(this.model.selectedLibs.indexOf(item), 1);
  }

  public open(notebook): void {
    this.notebook = notebook;
    this.destination = this.getResourcesList()[0];
    this.model = new InstallLibrariesModel(notebook,
      response => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.getInstalledLibrariesList();
          this.resetDialog();
        }
      },
      error => this.toastr.error(error.message || 'Library installation failed!', 'Oops!'),
      () => {
        this.getInstalledLibrariesList(true);
        this.changeDetector.detectChanges();
        this.selectorsReset();
      },
      this.librariesInstallationService);
 }

  public showErrorMessage(item): void {
    const dialogRef: MatDialogRef<ErrorLibMessageDialogComponent> = this.dialog.open(
      ErrorLibMessageDialogComponent, { data: item.error, width: '550px', panelClass: 'error-modalbox' });
  }

  public isInstallingInProgress(): void {
    this.installingInProgress = this.notebookLibs.some(lib => lib.filteredStatus.some(status => status.status === 'installing'));
      if (this.installingInProgress) {
        clearTimeout(this.loadLibsTimer);
        this.loadLibsTimer = window.setTimeout(() => this.getInstalledLibrariesList(), 10000);
      }
    }

  public reinstallLibrary(item, lib) {
    const retry = [{ group: lib.group, name: lib.name, version: lib.version }];

    if (this.getResourcesList().find(el => el.name === item.resource).type === 'СOMPUTATIONAL') {
      this.model.confirmAction(retry, item.resource);
    } else {
      this.model.confirmAction(retry);
    }
  }

  private getInstalledLibrariesList(init?: boolean) {
    this.model.getInstalledLibrariesList(this.notebook)
      .subscribe((data: any) => {
        if ( !this.filtredNotebookLibs.length || data.length !== this.notebookLibs.length) {
          this.filtredNotebookLibs = [...data];
        }
        this.filtredNotebookLibs = data.filter(lib =>
          this.filtredNotebookLibs.some(v =>
            (v.name + v.version === lib.name + v.version) && v.resource === lib.resource));
        this.notebookLibs = data ? data : [];
        this.notebookLibs.forEach(lib => {
          lib.filteredStatus = lib.status;
          if (lib.version && lib.version !== 'N/A')
            lib.version = 'v.' +  lib.version;
          }
        );
        this.filterLibs();
        this.changeDetector.markForCheck();
        this.filterConfiguration.group = this.createFilterList(this.notebookLibs.map(v => this.groupsListMap[v.group]));
        this.filterConfiguration.resource = this.createFilterList(this.notebookLibs.map(lib => lib.status.map(status => status.resource)));
        this.filterConfiguration.resourceType = this.createFilterList(this.notebookLibs.map(lib =>
          lib.status.map(status => status.resourceType)));
        this.filterConfiguration.status = this.createFilterList(this.notebookLibs.map(lib => lib.status.map(status => status.status)));
        this.isInstallingInProgress();
      });
  }

  public createFilterList(array): [] {
    return array.flat().filter((v, i, arr) => arr.indexOf(v) === i);
  }

  private getInstalledLibsByResource() {
    this.librariesInstallationService.getInstalledLibsByResource(this.notebook.project, this.notebook.name, this.model.computational_name)
      .subscribe((data: any) => this.destination.libs = data);
  }

  private libsUploadingStatus(groupsList): void {
    if (groupsList.length) {
      this.groupsList = this.filterGroups(groupsList);
      this.libs_uploaded = true;
      this.uploading = false;
    } else {
      this.libs_uploaded = false;
      this.uploading = true;
      this.clear = window.setTimeout(() => this.uploadLibGroups(), this.CHECK_GROUPS_TIMEOUT);
    }
  }

  private getFilteredList(): void {
    this.validity_format = '';

    if (this.group === 'java') {
      this.model.getDependencies(this.query)
        .subscribe(
          lib => this.filteredList = [lib],
          error => {
            if (error.status === HTTP_STATUS_CODES.NOT_FOUND
              || error.status === HTTP_STATUS_CODES.BAD_REQUEST
              || error.status === HTTP_STATUS_CODES.INTERNAL_SERVER_ERROR) {
              this.validity_format = error.message;
              this.filteredList = null;
            }
          });
    } else {
      this.model.getLibrariesList(this.group, this.query)
        .subscribe(libs => this.filteredList = libs);
    }
  }

  private selectorsReset(): void {
    this.destination = this.getResourcesList()[0];
    this.uploadLibGroups();
    this.getInstalledLibsByResource();
  }

  private resetDialog(): void {
    this.group = '';
    this.query = '';
    this.libSearch.setValue('');
    this.isInstalled = false;
    this.isInSelectedList = false;
    this.uploading = false;
    this.model.selectedLibs = [];
    this.filteredList = null;
    this.groupsList = [];

    clearTimeout(this.clear);
    clearTimeout(this.loadLibsTimer);
    this.selectorsReset();
  }

  public toggleFilterRow(): void {
    this.filtered = !this.filtered;
  }

  public filterLibs(): void {
    this.filtredNotebookLibs = this.notebookLibs.filter((lib) => {
      const isName = this.filterModel.name ?
        lib.name.toLowerCase().indexOf(this.filterModel.name.toLowerCase().trim()) !== -1
        || lib.version.indexOf(this.filterModel.name.toLowerCase().trim()) !== -1 : true;
      const isGroup = this.filterModel.group.length ? this.filterModel.group.includes(this.groupsListMap[lib.group]) : true;
      lib.filteredStatus = lib.status.filter(status => {
        const isResource = this.filterModel.resource.length ? this.filterModel.resource.includes(status.resource) : true;
        const isResourceType = this.filterModel.resourceType.length ? this.filterModel.resourceType.includes(status.resourceType) : true;
        const isStatus = this.filterModel.status.length ? this.filterModel.status.includes(status.status) : true;
        return isResource && isResourceType && isStatus;
      });
      return isName && isGroup && lib.filteredStatus.length;
    });
  }

  public resetFilterConfigurations(): void {
    this.notebookLibs.forEach(v => v.filteredStatus = v.status);
    this.filtredNotebookLibs = [...this.notebookLibs];
    this.filterModel.resetFilterLibs();
  }

  public openLibInfo(lib) {
    this.dialog.open(
      LibInfoDialogComponent, { data: lib, width: '550px', panelClass: 'error-modalbox' });
  }

  showlibinfo(lib: any) {
    console.log(lib);
    this.isLibInfoOpened[lib.name] = !this.isLibInfoOpened[lib.name];
  }
}

@Component({
  selector: 'error-message-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title">Library installation error</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div class="content lib-error" >
    {{ data }}
  </div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">Close</button>
  </div>
  `,
  styles: [    `
      .lib-error { max-height: 200px; overflow-x: auto; word-break: break-all; padding: 20px 30px !important; margin: 20px 0;}
  `
  ]
})
export class ErrorLibMessageDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ErrorLibMessageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    console.log(data);
  }
}

@Component({
  selector: 'lib-info-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title">Installed dependency</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <mat-list class="resources">

    <mat-list-item class="list-header">
      <div class="object">Name</div>
      <div class="size">Version</div>
    </mat-list-item>

    <div class="scrolling-content delete-list" id="scrolling">

      <mat-list-item *ngFor="let object of [1,2,3]" class="delete-item">
        <div class="object">
         {{data.name}}
        </div>
        <div class="size">v2.3.4</div>
      </mat-list-item>

    </div>
  </mat-list>
<!--  <div class="text-center">-->
<!--    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">Close</button>-->
<!--  </div>-->
  `,
  styles: [    `
    .mat-list-base {
      padding: 40px 30px;
    }

    .object {
      width: 70%;
      display: flex;
      align-items: center;
      padding-right: 10px;
    }

    .size {
      width: 30%;
    }
    .scrolling-content.delete-list {
      max-height: 200px;
      overflow-y: auto;
      padding-top: 11px;
    }

    .empty-list {
      display: flex;
      width: 100%;
      justify-content: center;
      color: #35afd5;
      padding: 15px;
    }

    .list-header {
      border-top: 1px solid #edf1f5;
      border-bottom: 1px solid #edf1f5;
      color: #577289;
      width: 100%;
    }
  `
  ]
})
export class LibInfoDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ErrorLibMessageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    console.log(data);
  }
}
