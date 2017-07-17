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

import { Component, OnInit, ViewChild, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';

import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';

import { InstallLibrariesModel } from './';
import { LibrariesInstallationService} from '../../../core/services';
import { ErrorMapUtils, HTTP_STATUS_CODES } from '../../../core/util';

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
  public uploading: boolean = false;
  public libs_uploaded: boolean = false;

  public processError: boolean = false;
  public errorMessage: string = '';

  public isInstalled: boolean = false;
  public isFilteringProc: boolean = false;
  public isInSelectedList: boolean = false;
  public installingInProgress: boolean = false;
  public libSearch: FormControl = new FormControl();
  public groupsListMap = {'r_pkg': 'R packages', 'pip2': 'Python 2', 'pip3': 'Python 3', 'os_pkg': 'Apt/Yum'};


  private readonly CHECK_GROUPS_TIMEOUT: number = 5000;
  private clear: number;
  private clearCheckInstalling = undefined;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('tabGroup') tabGroup;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(private librariesInstallationService: LibrariesInstallationService) {
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
    this.bindDialog.onClosing = () => this.close();
  }
  
  uploadLibraries(): void {
     this.librariesInstallationService.getGroupsList(this.notebook.name)
      .subscribe(
        response => this.libsUploadingStatus(response),
        error => {
          this.processError = true;
          this.errorMessage = JSON.parse(error.message).message;
        });
  }

  public filterList(): void {
    (this.query.length >= 2 && this.group) ? this.getFilteredList() : this.filteredList = null;
  }

  public isDuplicated(item) {
    const select = {group: this.group, name: item.key, version: item.value};

    this.isInSelectedList = this.model.selectedLibs.filter(el => JSON.stringify(el) === JSON.stringify(select)).length > 0;

    if (this.notebook.libs && this.notebook.libs.length)
      this.isInstalled = this.notebook.libs.findIndex(libr =>
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
      this.notebookLibs = notebook.libs || [];
      this.model = new InstallLibrariesModel(notebook, (response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.getInstalledLibrariesList();
          this.resetDialog();
        }
      },
      (response: Response) => {
        this.processError = true;
        this.errorMessage = ErrorMapUtils.setErrorMessage(response);
      },
      () => {
        this.bindDialog.open(param);
        this.isInstallingInProgress(this.notebookLibs);

        if (!this.notebook.libs || !this.notebook.libs.length)
          this.tabGroup.selectedIndex = 1;

        this.uploadLibraries();
      },
      this.librariesInstallationService);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();

    this.resetDialog();
    this.buildGrid.emit();
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
  private getInstalledLibrariesList() {
    this.model.getInstalledLibrariesList()
      .subscribe((data: any) => {
        this.notebookLibs = data ? data : [];
        this.isInstallingInProgress(this.notebookLibs);
      });
  }

  private libsUploadingStatus(groupsList): void {
    if (groupsList.length) {
      this.groupsList = groupsList;
      this.libs_uploaded = true;
      this.uploading = false;
    } else {
      this.libs_uploaded = false;
      this.uploading = true;
      this.clear = window.setTimeout(() => this.uploadLibraries(), this.CHECK_GROUPS_TIMEOUT);
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

  private resetDialog(): void {
    this.group = '';
    this.query = '';

    this.processError = false;
    this.isFilteringProc = false;
    this.isInstalled = false;
    this.isInSelectedList = false;

    this.errorMessage = '';
    this.model.selectedLibs = [];
    this.filteredList = null ;
    this.tabGroup.selectedIndex = 0;
    clearTimeout(this.clear);
    clearInterval(this.clearCheckInstalling);
    this.clearCheckInstalling = undefined;
  }
}
