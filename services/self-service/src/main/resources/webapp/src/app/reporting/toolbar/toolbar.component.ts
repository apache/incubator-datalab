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
  today: string = moment().format('YYYY-MM-D');

  @Output() rebuildReport: EventEmitter<{}> = new EventEmitter();
  @Output() setRangeOption: EventEmitter<string> = new EventEmitter();

  ngOnInit() { }

  rebuild($event): void {
    this.rebuildReport.emit($event);
  }

  calculateRange(opt: string): void {
    console.log('setRangeOption: ', opt);
    let rangeValue;
    if(opt === 'YTD') {
      console.log('full year', moment().subtract(1, 'year').format('YYYY-MM-D'));
      console.log('start year', moment().startOf('year').format('YYYY-MM-D'));

      rangeValue = moment().startOf('year').format('YYYY-MM-D');
    } else if (opt === 'QTD') {
      console.log('quarter', moment().quarter(moment().quarter()).startOf('quarter').format('YYYY-MM-D'));

      rangeValue = moment().quarter(moment().quarter()).startOf('quarter').format('YYYY-MM-D');
    } else if (opt === 'MTD') {
      console.log('all months', moment().subtract(1, 'months').format('YYYY-MM-D'));
      console.log('start months', moment().startOf('months').format('YYYY-MM-D'));

      rangeValue = moment().startOf('months').format('YYYY-MM-D')
    } else if (opt === 'WTD') {
      console.log('week', moment().subtract(1, 'week').format('YYYY-MM-D'));

      rangeValue = moment().subtract(1, 'week').format('YYYY-MM-D')
    }
    console.log('rangeValue', rangeValue);

    this.setRangeOption.emit(rangeValue);
  }
}
