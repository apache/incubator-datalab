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

import { Component, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';

import 'rxjs/add/operator/startWith';
import 'rxjs/add/operator/map';

import { LibrariesInstallationService} from '../../../core/services';
import { HTTP_STATUS_CODES } from '../../../core/util';

@Component({
  selector: 'install-libraries',
  templateUrl: './install-libraries.component.html',
  styleUrls: ['./install-libraries.component.css']
})
export class InstallLibrariesComponent implements OnInit {

  public notebook: any;
  public filteredList: any;
  public selectedLibs = [];
  public groupsList: Array<string>;

  public query: string = '';
  public group: string;
  public uploading: boolean = false;
  public libs_uploaded: boolean = false;
  private readonly CHECK_GROUPS_TIMEOUT: number = 5000;
  public groupsListMap = {'r_pkg': 'R packages', 'pip2': 'Python 2', 'pip3': 'Python 3','os_pkg': 'Apt/Yum'};

  @ViewChild('bindDialog') bindDialog;
  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();


  constructor(private librariesInstallationService: LibrariesInstallationService) { }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  uploadLibraries() {
     this.librariesInstallationService.getGroupsList(this.notebook.image)
      .subscribe(
        response => this.libsUploadingStatus(response.status, response),
        error => this.libsUploadingStatus(error.status, error));
  }

  getFilteredList() {
    this.librariesInstallationService
    .getAvailableLibrariesList({"image": this.notebook.image, "group": this.group, "start_with": this.query})
    .subscribe(libs => {
      this.filteredList = libs;
    });
  }

  installLibs() {
    this.librariesInstallationService
      .installLibraries({
        notebook_name: this.notebook.name,
        libs: this.selectedLibs
      })
      .subscribe(response => {
        this.close();
      });
  }

  private libsUploadingStatus(status: number, data) {
    if (data.length) {
      this.groupsList = data;
      this.libs_uploaded = true
      this.uploading = false;
    } else {
      this.uploading = true;
      setTimeout(() => this.uploadLibraries(), this.CHECK_GROUPS_TIMEOUT);
    }
  }

  public filterList() {
    (this.query.length >= 3 && this.group) ? this.getFilteredList() : this.filteredList = null;
  }

  public selectLibrary(item){
    this.selectedLibs.push({group: this.group, name: item.key, version: item.value});

    this.query = '';
    this.filteredList = null;
  }

  public removeSelectedLibrary(item) {
    this.selectedLibs.splice(this.selectedLibs.indexOf(item), 1);
  }

  public isEmpty(obj) {
    if(obj) return Object.keys(obj).length === 0;
  }

  public open(param, notebook): void {
    this.notebook = notebook;

    this.uploadLibraries();
    this.bindDialog.open(param);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
    this.buildGrid.emit();
    this.resetDialog();
  }

  private resetDialog(): void {
    this.group = '';
    this.query = '';
    this.selectedLibs = [];
  }
}
