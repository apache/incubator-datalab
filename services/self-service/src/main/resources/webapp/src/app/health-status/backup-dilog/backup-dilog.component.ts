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
import { DICTIONARY } from '../../../dictionary/global.dictionary';

import { BackupOptionsModel } from '../environment-status.model';

@Component({
  selector: 'dlab-backup-dilog',
  templateUrl: './backup-dilog.component.html',
  styleUrls: ['./backup-dilog.component.scss']
})
export class BackupDilogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public backupOptions: BackupOptionsModel = new BackupOptionsModel([], [], [], [], false, false);
  public valid: boolean = true;

  @ViewChild('bindDialog') bindDialog;
  @Output() backupOpts: EventEmitter<{}> = new EventEmitter();

  ngOnInit() {
    this.backupOptions.setDegault();
    this.bindDialog.onClosing = () => this.backupOptions.setDegault();
  }

  public open(param): void {
    this.bindDialog.open(param);
  }

  public onHoldChanged($event, key) {
    this.backupOptions[key] instanceof Array
      ? (this.backupOptions[key][0] = $event.checked ? 'all' : 'skip')
      : (this.backupOptions[key] = !this.backupOptions[key]);

    this.checkValidity();
  }

  public applyOptions() {
    this.backupOpts.emit(this.backupOptions);
    this.backupOptions.setDegault();
    this.bindDialog.close();
  }

  private checkValidity() {
    let items = [];

    Object.keys(this.backupOptions).forEach(el => {
      if (this.backupOptions[el] instanceof Array) {
        if (this.backupOptions[el][0] && this.backupOptions[el][0] !== 'skip') items.push(this.backupOptions[el][0]);
      } else {
        if (this.backupOptions[el]) items.push(this.backupOptions[el]) ;
      }
    });

    this.valid = items.length > 0;
  }
}
