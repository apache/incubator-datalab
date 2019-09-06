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


import { Component, OnInit, ViewChild, ViewEncapsulation, ChangeDetectorRef, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { FormControl } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { debounceTime } from 'rxjs/operators';

import { InstallLibrariesModel } from './install-libraries.model';
import { LibrariesInstallationService } from '../../../core/services';
import { SortUtils, HTTP_STATUS_CODES } from '../../../core/util';


@Component({
  selector: 'install-libraries',
  templateUrl: './install-libraries.component.html',
  styleUrls: ['./install-libraries.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class InstallLibrariesComponent implements OnInit {

  public model: InstallLibrariesModel;
  public notebook: any;
  public filteredList: any;
  public groupsList: Array<string>;
  public notebookLibs: Array<any> = [];
  public notebookFailedLibs: Array<any> = [];

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
  private clearCheckInstalling = undefined;

  @ViewChild('groupSelect') group_select;
  @ViewChild('resourceSelect') resource_select;

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
    this.libSearch.disable();
    this.libSearch.valueChanges.pipe(
      debounceTime(1000))
      .subscribe(newValue => {
        this.query = newValue || '';
        this.filterList();
      });
    this.open(this.data);
  }

  uploadLibGroups(): void {
    this.librariesInstallationService.getGroupsList(this.notebook.name, this.model.computational_name)
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

      this.libSearch.enable();
      this.uploadLibGroups();
      this.getInstalledLibsByResource();
    }
    this.filterList();
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
    const dialogRef: MatDialogRef<ErrorMessageDialogComponent> = this.dialog.open(
      ErrorMessageDialogComponent, { data: item.error, width: '550px', panelClass: 'error-modalbox' });
  }

  public isInstallingInProgress(data): void {
    this.notebookFailedLibs = data.filter(lib => lib.status.some(inner => inner.status === 'failed'));
    this.installingInProgress = data.filter(lib => lib.status.some(inner => inner.status === 'installing')).length > 0;

    if (this.installingInProgress || this.notebookFailedLibs.length) {
      if (this.clearCheckInstalling === undefined)
        this.clearCheckInstalling = window.setInterval(() => this.getInstalledLibrariesList(), 10000);
    } else {
      clearInterval(this.clearCheckInstalling);
      this.clearCheckInstalling = undefined;
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
        this.notebookLibs = data ? data : [];
        this.changeDetector.markForCheck();
        this.isInstallingInProgress(this.notebookLibs);
      });
  }

  private getInstalledLibsByResource() {
    this.librariesInstallationService.getInstalledLibsByResource(this.notebook.name, this.model.computational_name)
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
    this.resource_select && this.resource_select.setDefaultOptions(this.getResourcesList(),
      'Select resource', 'destination', 'title', 'array');
    this.group_select && this.group_select.setDefaultOptions([], '', 'group_lib', null, 'array');
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
    this.destination = null;
    this.groupsList = [];

    this.libSearch.disable();
    clearTimeout(this.clear);
    clearInterval(this.clearCheckInstalling);
    this.clearCheckInstalling = undefined;
    this.selectorsReset();
  }
}

@Component({
  selector: 'error-message-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title">Library installation error</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div class="content">{{ data }}</div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">Close</button>
  </div>
  `,
  styles: []
})
export class ErrorMessageDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ErrorMessageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
