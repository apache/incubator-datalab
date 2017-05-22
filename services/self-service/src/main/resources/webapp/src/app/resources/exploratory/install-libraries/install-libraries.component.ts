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

@Component({
  selector: 'install-libraries',
  templateUrl: './install-libraries.component.html',
  styleUrls: ['./install-libraries.component.css']
})
export class InstallLibrariesComponent implements OnInit {

  public filteredList = [];
  public selectedLibs = [];
  public installedLibs = [];
  public query: string = '';

  public libs_uploaded: boolean = true;
  public uploading: boolean = true;
  public group: string;

  private data: any;

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
    this.data = this.librariesInstallationService.getAvailableLibrariesList()
    // .subscribe(resp => console.log(resp))
    console.log(this.data);
  }

  public filter() {
    console.log(this.query);
    console.log(this.group);

    if (this.query.length > 0) {
        this.filteredList = this.data.filter(el => el.toLowerCase().indexOf(this.query.toLowerCase()) > -1);
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

  public remove(item){
    this.selectedLibs.splice(this.selectedLibs.indexOf(item), 1);
  }

  public open(param, notebook): void {
    this.bindDialog.open(param);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }
}
