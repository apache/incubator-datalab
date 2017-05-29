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
import { Response } from '@angular/http';

import 'rxjs/add/operator/startWith';
import 'rxjs/add/operator/map';

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
  public query: string = '';
  public group: string;
  public uploading: boolean = false;
  public libs_uploaded: boolean = false;

  public processError: boolean = false;
  public errorMessage: string = '';

  public isInstalled: boolean = false;
  public isFilteringProc: boolean = false;
  public isInSelectedList: boolean = false;
  public groupsListMap = {'r_pkg': 'R packages', 'pip2': 'Python 2', 'pip3': 'Python 3', 'os_pkg': 'Apt/Yum'};


  private readonly CHECK_GROUPS_TIMEOUT: number = 5000;

  @ViewChild('bindDialog') bindDialog;
  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(private librariesInstallationService: LibrariesInstallationService) {
    this.model = InstallLibrariesModel.getDefault(librariesInstallationService);
  }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  uploadLibraries(): void {
     this.librariesInstallationService.getGroupsList(this.notebook.image)
      .subscribe(
        response => this.libsUploadingStatus(response.status, response),
        error => this.libsUploadingStatus(error.status, error));
  }

  public installLibs(): void {
    this.model.confirmAction();
  }

  public reInstallSpecificLib(retry: any): void {
    this.model.confirmAction(retry);
  }

  public filterList(): void {
    (this.query.length >= 3 && this.group) ? this.getFilteredList() : this.filteredList = null;
  }

  public isDuplicated(item) {
    const select = {group: this.group, name: item.key, version: item.value};

    this.isInSelectedList = this.model.selectedLibs.filter(el => JSON.stringify(el) === JSON.stringify(select)).length > 0;

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

      this.model = new InstallLibrariesModel(notebook, (response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.close();
          this.buildGrid.emit();
        }
      },
      (response: Response) => {
        this.processError = true;
        this.errorMessage = ErrorMapUtils.setErrorMessage(response);
      },
      () => {
        this.bindDialog.open(param);
        this.uploadLibraries();
      },
      this.librariesInstallationService);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();

    this.resetDialog();
  }

  private libsUploadingStatus(status: number, groupsList): void {
    if (groupsList.length) {
      this.groupsList = groupsList;
      this.libs_uploaded = true;
      this.uploading = false;
    } else {
      this.libs_uploaded = false;
      this.uploading = true;
      setTimeout(() => this.uploadLibraries(), this.CHECK_GROUPS_TIMEOUT);
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
    this.errorMessage = '';
    this.model.selectedLibs = [];
    this.filteredList = null ;
  }
}
