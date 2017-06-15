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

import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import * as moment from 'moment';

@Component({
  selector: 'dlab-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.css']
})
export class ToolbarComponent implements OnInit {
  reportData: any;
  rangeOptions = ['YTD', 'QTD', 'MTD'];
  today: string = moment().format('YYYY-MMM-D');

  @Output() rebuildReport: EventEmitter<{}> = new EventEmitter();

  ngOnInit() { }

  rebuild($event): void {
    this.rebuildReport.emit($event);
  }

  setRangeOption(opt: string): void {
    console.log('setRangeOption: ', opt, this.today);
  }
}
