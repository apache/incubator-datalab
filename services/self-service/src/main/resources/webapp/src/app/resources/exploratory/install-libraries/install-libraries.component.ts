/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import { Component, OnInit, ViewChild, Output, EventEmitter, ViewEncapsulation, ChangeDetectorRef } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';
import { MdDialog, MdDialogRef, MdDialogConfig } from '@angular/material';

import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';

import { InstallLibrariesModel } from './';
import { LibrariesInstallationService} from '../../../core/services';
import { ErrorMapUtils, SortUtil, HTTP_STATUS_CODES } from '../../../core/util';

@Component({
  selector: 'install-libraries',
  templateUrl: './install-libraries.component.html',
  styleUrls: ['./install-libraries.component.css'],
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

  public processError: boolean = false;
  public errorMessage: string = '';

  public isInstalled: boolean = false;
  public isFilteringProc: boolean = false;
  public isInSelectedList: boolean = false;
  public installingInProgress: boolean = false;
  public libSearch: FormControl = new FormControl();
  public groupsListMap = {'r_pkg': 'R packages', 'pip2': 'Python 2', 'pip3': 'Python 3', 'os_pkg': 'Apt/Yum', 'others': 'Others'};

  private readonly CHECK_GROUPS_TIMEOUT: number = 5000;
  private clear: number;
  private clearCheckInstalling = undefined;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('tabGroup') tabGroup;
  @ViewChild('groupSelect') group_select;
  @ViewChild('resourceSelect') resource_select;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    public dialog: MdDialog,
    private librariesInstallationService: LibrariesInstallationService,
    private changeDetector : ChangeDetectorRef) {
    this.model = InstallLibrariesModel.getDefault(librariesInstallationService);
  }

  ngOnInit() {
    this.libSearch.valueChanges
      .debounceTime(1000)
      .distinctUntilChanged()
      .subscribe(newValue => {
        this.query = newValue;
        this.filterList();
      });
    this.bindDialog.onClosing = () => {
      this.resetDialog();
      this.buildGrid.emit();
    };
  }

  uploadLibGroups(): void {
     this.librariesInstallationService.getGroupsList(this.notebook.name, this.model.computational_name)
      .subscribe(
        response => {
          this.libsUploadingStatus(response);
          this.changeDetector.detectChanges();

          this.resource_select && this.resource_select.setDefaultOptions(this.getResourcesList(), this.destination.name, 'destination', 'name', 'array');
          this.group_select && this.group_select.setDefaultOptions(this.groupsList, 'Select group', 'group_lib', null, 'list', this.groupsListMap);
        },
        error => {
          this.processError = true;
          this.errorMessage = JSON.parse(error.message).message;
        });
  }

  private getResourcesList() {
    this.notebook.type = 'EXPLORATORY';
    return [this.notebook].concat(this.notebook.resources
      .filter(item => item.status === 'running')
      .map(item => {
        item['name'] = item.computational_name;
        item['type'] = 'СOMPUTATIONAL';
        return item;
      }));
  }

  public filterList(): void {
    (this.query.length >= 2 && this.group) ? this.getFilteredList() : this.filteredList = null;
  }

  public onUpdate($event) {
    if ($event.model.type === 'group_lib') {
      this.group = $event.model.value;
    } else if ($event.model.type === 'destination') {
      this.resetDialog(true);

      this.destination = $event.model.value;
      if (this.destination && this.destination.type === 'СOMPUTATIONAL') this.model.computational_name = this.destination.name;

      this.uploadLibGroups();
      this.getInstalledLibsByResource();
    }

    if (this.destination && this.destination.type === 'EXPLORATORY') this.model.computational_name = null;
    this.filterList();
  }

  public isDuplicated(item) {
    const select = {group: this.group, name: item.key, version: item.value};

    this.isInSelectedList = this.model.selectedLibs.filter(el => JSON.stringify(el) === JSON.stringify(select)).length > 0;

    if (this.destination && this.destination.libs)
      this.isInstalled = this.destination.libs.findIndex(libr =>
        select.name === libr.name && select.group === libr.group && select.version === libr.version
      ) >= 0;

    return this.isInSelectedList || this.isInstalled;
  }

  public selectLibrary(item): void {
    this.model.selectedLibs.push({group: this.group, name: item.key, version: item.value});

    this.query = '';
    this.filteredList = null;
  }

  public removeSelectedLibrary(item): void {
    this.model.selectedLibs.splice(this.model.selectedLibs.indexOf(item), 1);
  }

  public open(param, notebook): void {
    if (!this.bindDialog.isOpened)
      this.notebook = notebook;
      this.model = new InstallLibrariesModel(notebook, (response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.getInstalledLibrariesList();
          this.resetDialog();
        }
      },
      (error: any) => {
        this.processError = true;
        this.errorMessage = error.message;
      },
      () => {
        this.bindDialog.open(param);

        this.getInstalledLibrariesList(true);
        this.changeDetector.detectChanges();

        this.resource_select && this.resource_select.setDefaultOptions(this.getResourcesList(), 'Select resource', 'destination', 'name', 'array');
        this.group_select && this.group_select.setDefaultOptions([], '', 'group_lib', null, 'array');
      },
      this.librariesInstallationService);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();

    this.buildGrid.emit();
    this.resetDialog();
  }

  public showErrorMessage(item): void {
    const dialogRef: MdDialogRef<ErrorMessageDialog> = this.dialog.open(ErrorMessageDialog, { data: item.error, width: '550px' });
  }

  public isInstallingInProgress(data): void {
    this.notebookFailedLibs = data
      .filter(el => el.status === 'failed')
      .map(el => { return {group: el.group, name: el.name, version: el.version}});
    this.installingInProgress = data.findIndex(libr => libr.status === 'installing') >= 0;

    if (this.installingInProgress || this.notebookFailedLibs.length) {
      if (this.clearCheckInstalling === undefined)
        this.clearCheckInstalling = window.setInterval(() => this.getInstalledLibrariesList(), 10000);
    } else {
      clearInterval(this.clearCheckInstalling);
      this.clearCheckInstalling = undefined;
    }
  }

  private getInstalledLibrariesList(init?: boolean) {
    this.model.getInstalledLibrariesList(this.notebook)
      .subscribe((data: any) => {
        this.notebookLibs = data ? data : [];
        this.changeDetector.markForCheck();
        this.isInstallingInProgress(this.notebookLibs);

        if (init && !this.notebookLibs.length) this.tabGroup.selectedIndex = 1;
      });
  }

  private getInstalledLibsByResource() {
    this.librariesInstallationService.getInstalledLibsByResource(this.notebook.name, this.model.computational_name)
      .subscribe((data: any) => this.destination.libs = data);
  }

  private libsUploadingStatus(groupsList): void {
    if (groupsList.length) {
      this.groupsList = SortUtil.libGroupsSort(groupsList);
      this.libs_uploaded = true;
      this.uploading = false;
    } else {
      this.libs_uploaded = false;
      this.uploading = true;
      this.clear = window.setTimeout(() => this.uploadLibGroups(), this.CHECK_GROUPS_TIMEOUT);
    }
  }

  private getFilteredList(): void {
    this.isFilteringProc = true;
    this.model.getLibrariesList(this.group, this.query)
      .subscribe(libs => {
        this.filteredList = libs;
        this.isFilteringProc = false;
      });
  }

  private resetDialog(nActive?): void {
    this.group = '';
    this.query = '';

    this.processError = false;
    this.isFilteringProc = false;
    this.isInstalled = false;
    this.isInSelectedList = false;

    this.errorMessage = '';
    this.model.selectedLibs = [];
    this.filteredList = null ;
    this.destination = null;
    this.groupsList = [];

    if (!nActive) this.tabGroup.selectedIndex = 0;
    clearTimeout(this.clear);
    clearInterval(this.clearCheckInstalling);
    this.clearCheckInstalling = undefined;
  }
}

@Component({
  selector: 'error-message-dialog',
  template: `<div class="content">{{ dialogRef.config.data }}</div>`,
  styles: [`.content { color: #f1696e; padding: 20px 25px; font-size: 14px; font-weight: 400 }`]
})
export class ErrorMessageDialog {
  constructor(public dialogRef: MdDialogRef<ErrorMessageDialog>) { }
}