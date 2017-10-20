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

import { LibrariesInstallationService} from '../../../core/services';
import { ManageLibrariesModel } from './';

@Component({
  selector: 'dlab-manage-libs',
  templateUrl: './manage-libraries.component.html',
  styleUrls: ['./manage-libraries.component.css']
})
export class ManageLibsComponent implements OnInit {
  public notebookLibs: Array<any> = [];
  public groupsListMap = {'r_pkg': 'R packages', 'pip2': 'Python 2', 'pip3': 'Python 3', 'os_pkg': 'Apt/Yum', 'others': 'Others'};

  private notebook: any;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();
  @ViewChild('bindDialog') bindDialog;
  
  constructor(private librariesInstallationService: LibrariesInstallationService) {
    // this.model = ManageLibrariesModel.getDefault(librariesInstallationService);
  }

  ngOnInit() { }


  public open(param, notebook): void {
    if (!this.bindDialog.isOpened)
      this.notebook = notebook;
      this.notebookLibs = notebook.libs || [];
      // this.model = new ManageLibrariesModel(notebook, (response: Response) => {
      //   if (response.status === HTTP_STATUS_CODES.OK) {
      //     this.getInstalledLibrariesList();
      //     this.resetDialog();
      //   }
      // },
      // (error: any) => {
      //   this.processError = true;
      //   this.errorMessage = error.message;
      // },
      // () => {
        this.bindDialog.open(param);
      //   this.isInstallingInProgress(this.notebookLibs);

      //   if (!this.notebook.libs || !this.notebook.libs.length)
      //     this.tabGroup.selectedIndex = 1;

      //   this.uploadLibraries();
      // },
      // this.librariesInstallationService);
  }


  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close(); 
  }
}
