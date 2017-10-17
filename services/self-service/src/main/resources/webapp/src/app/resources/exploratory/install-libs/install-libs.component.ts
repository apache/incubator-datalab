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

@Component({
  selector: 'dlab-manage-libs',
  templateUrl: './install-libs.component.html',
  styleUrls: ['./install-libs.component.css']
})
export class InstallLibsComponent implements OnInit {
  notebook: any;

  @ViewChild('bindDialog') bindDialog;
  constructor() { }

  ngOnInit() { }

  public open(params, notebook): void {
    this.notebook = notebook;

    console.log(this.notebook);
    this.bindDialog.open(params);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }
}
