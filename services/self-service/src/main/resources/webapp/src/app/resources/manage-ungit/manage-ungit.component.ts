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

import { Component, OnInit, ViewChild } from '@angular/core';
import { ManageUngitService } from './../../core/services';
import { GitCredentialsModel } from './git-creds.model';

@Component({
  selector: 'dlab-manage-ungit',
  templateUrl: './manage-ungit.component.html',
  styleUrls: ['./manage-ungit.component.css',
              '../exploratory/install-libraries/install-libraries.component.css']
})
export class ManageUngitComponent implements OnInit {

  gitCredentials: Array<GitCredentialsModel>;
  @ViewChild('bindDialog') bindDialog;

  constructor(private manageUngitService: ManageUngitService) { }

  ngOnInit() {
    this.getGitCredentials();
  }

  public open(param): void {
    if (!this.bindDialog.isOpened)
      this.bindDialog.open(param);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  private getGitCredentials(): void {
    this.manageUngitService.getGitCreds()
      .subscribe((response: any) => {
          console.log(response);
          this.gitCredentials = response.git_creds;
        },
        error => {
          console.log(error);
        });
  }
}
