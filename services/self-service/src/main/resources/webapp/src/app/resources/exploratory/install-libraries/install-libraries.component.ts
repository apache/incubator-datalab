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

import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';

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
  public filteredList = [];
  public selectedLibs = [];
  // public installedLibs = [];
  public query: string = '';

  public libs_uploaded: boolean = false;
  public uploading: boolean = false;

  public group: string;

  private data: any;
  private readonly CHECK_GROUPS_TIMEOUT: number = 5000;

  @ViewChild('bindDialog') bindDialog;
                      
  public groups_map = [
    {value: 'r_pkg', label: 'R packages'},
    {value: 'pip2', label: 'Python 2'},
    {value: 'pip3', label: 'Python 3'},
    {value: 'os_pkg', label: 'Apt/Yum'}
  ];

  // TODO: remove after testing
  // installedLibs = [{name: "htop", version: '2.0.1-1ubuntu1', status: "installed"}, {name: "htop2", version: '2.0.1-1ubuntu2', status: "failed"}, {name: "htop3", version: '2.0.1-1ubuntu3', status: "installing"}];

  constructor(private librariesInstallationService: LibrariesInstallationService) { }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  getFilteredList() {
    this.data = this.librariesInstallationService
    .getAvailableLibrariesList(this.notebook.image, this.group, this.query)
    .subscribe(resp => console.log(resp));
  }

  uploadLibraries() {
    this.uploading = true;

     this.librariesInstallationService.getGroupsList()
      .subscribe(
        response => this.libsUploadingStatus(response.status, response),
        error => this.libsUploadingStatus(error.status, error));
  }

  private libsUploadingStatus(status: number, data) {

    if (status === HTTP_STATUS_CODES.OK && data.groups) {
      this.uploading = false;
      this.libs_uploaded = true
    } else {
      console.log("CHECK_GROUPS_TIMEOUT");
      setTimeout(() => this.uploadLibraries(), this.CHECK_GROUPS_TIMEOUT);
    }
  }

  // this.filteredList = this.states.filter(el => el.toLowerCase().indexOf(this.query.toLowerCase()) > -1);
  public filter() {
    if (this.query.length >= 3 && this.group) {
        this.getFilteredList();
    } else {
        this.filteredList = [];
    }
  }

  public select(item){
    this.selectedLibs.push(item);
    console.log(this.selectedLibs);
    
    this.query = '';
    this.filteredList = [];
  }

  public remove(item) {
    this.selectedLibs.splice(this.selectedLibs.indexOf(item), 1);
  }

  public open(param, notebook): void {
    this.notebook = notebook;
    this.bindDialog.open(param);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
    this.resetDialog();
  }

  private resetDialog(): void {
    this.group = '';
    this.query = '';
    this.selectedLibs = [];
  }
}
