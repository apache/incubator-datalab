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
  rangeOptions = {'YTD': 'Year To Date', 'QTD': 'Quarter To Date', 'MTD': 'Month To Date', 'reset': 'All Period Report'};
  today: string = moment().format('YYYY-MM-D');

  @Output() rebuildReport: EventEmitter<{}> = new EventEmitter();
  @Output() setRangeOption: EventEmitter<string> = new EventEmitter();

  ngOnInit() { }

  rebuild($event): void {
    this.rebuildReport.emit($event);
  }

  calculateRange(option: string): void {
    let rangeValue;

    switch(option) {
      case 'YTD':
          rangeValue = moment().startOf('year').format('YYYY-MM-D');
          break;
      case 'QTD':
          rangeValue = moment().quarter(moment().quarter()).startOf('quarter').format('YYYY-MM-D');
          break;
      case 'MTD':
          rangeValue = moment().startOf('months').format('YYYY-MM-D');
          break;
      default:
          rangeValue = '';
    }
    //   console.log('week', moment().subtract(1, 'week').format('YYYY-MM-D'));
    this.setRangeOption.emit(rangeValue);
  }
}
