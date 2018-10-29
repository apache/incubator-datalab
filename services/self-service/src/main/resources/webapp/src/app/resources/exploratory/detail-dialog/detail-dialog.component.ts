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

import { Component, ViewChild } from '@angular/core';

import { DateUtils } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'detail-dialog',
  templateUrl: 'detail-dialog.component.html'
})

export class DetailDialogComponent {
  readonly DICTIONARY = DICTIONARY;

  notebook: any;
  upTimeInHours: number;
  upTimeSince: string = '';
  tooltip: boolean = false;

  @ViewChild('bindDialog') bindDialog;

  public open(param, notebook): void {
    this.tooltip = false;
    this.notebook = notebook;

    this.upTimeInHours = (notebook.time) ? DateUtils.diffBetweenDatesInHours(this.notebook.time) : 0;
    this.upTimeSince = (notebook.time) ? new Date(this.notebook.time).toString() : '';

    this.bindDialog.open(param);
  }

  public isEllipsisActive($event): void {
    if ($event.target.offsetWidth < $event.target.scrollWidth)
      this.tooltip = true;
  }
}
