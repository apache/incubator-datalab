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
import { DICTIONARY } from './../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-manage-env-dilog',
  templateUrl: './manage-environment-dilog.component.html',
  styleUrls: ['./manage-environment-dilog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ManageEnvironmentComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  public usersList: Array<string> = [];

  @ViewChild('bindDialog') bindDialog;
  @Output() manageEnv: EventEmitter<{}> = new EventEmitter();

  ngOnInit() {}

  public open(param, data): void {
    this.usersList = data;
    this.bindDialog.open(param);
  }

  public applyAction(action, user) {
    this.manageEnv.emit({action, user});
  }
}
