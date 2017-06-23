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

import { Component, OnInit, Output, EventEmitter, ViewEncapsulation, ElementRef } from '@angular/core';

import { NgDateRangePickerOptions } from 'ng-daterangepicker';
import * as moment from 'moment';

@Component({
  selector: 'dlab-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class ToolbarComponent implements OnInit {
  reportData: any;
  availablePeriodFrom: string;
  availablePeriodTo: string;

  rangeOptions = {'YTD': 'Year To Date', 'QTD': 'Quarter To Date', 'MTD': 'Month To Date', 'reset': 'All Period Report'};
  options: NgDateRangePickerOptions;
  hElement: HTMLElement;
  rangeLabels: NodeListOf<Element>;
  fromtoLabels: NodeListOf<Element>;

  @Output() rebuildReport: EventEmitter<{}> = new EventEmitter();
  @Output() setRangeOption: EventEmitter<{}> = new EventEmitter();

  constructor(private elRef: ElementRef) {
    this.options = {
      theme: 'default',
      range: 'tm',
      dayNames: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      presetNames: ['This Month', 'Last Month', 'This Week', 'Last Week', 'This Year', 'Last Year', 'From', 'To'],
      dateFormat: 'dd MMM y',
      outputFormat: 'YYYY/MM/DD',
      startOfWeek: 1
    };

    this.hElement = this.elRef.nativeElement;
    this.rangeLabels = this.hElement.getElementsByClassName('value-txt');
  }

  ngOnInit() {
    this.clearRangePicker();
     if (localStorage.getItem('report_period')) {
        const availableRange = JSON.parse(localStorage.getItem('report_period'));
        this.availablePeriodFrom = availableRange.start_date;
        this.availablePeriodTo = availableRange.end_date;
     }
  }

  setDateRange() {
    const availableRange = JSON.parse(localStorage.getItem('report_period'));

    this.availablePeriodFrom = availableRange.start_date;
    this.availablePeriodTo = availableRange.end_date;
  }

  clearRangePicker(): void {
    for(let label = 0; label < this.rangeLabels.length; ++label)
      this.rangeLabels[label].className += ' untouched';
  }

  onChange(dateRange: string): void {
    for (let label = 0; label < this.rangeLabels.length; ++label)
      if (this.rangeLabels[label].classList.contains('untouched')) {
        this.rangeLabels[label].classList.remove('untouched');
    }

    const reportDateRange = dateRange.split('-');
    this.setRangeOption.emit({start_date: reportDateRange[0].split('/').join('-')});
  }

  rebuild($event): void {
    this.rebuildReport.emit($event);
  }

  calculateRange(option: string): void {
    let rangeValue;

    switch(option) {
      case 'YTD':
          rangeValue = moment().startOf('year').format('YYYY-MM-DD');
          break;
      case 'QTD':
          rangeValue = moment().quarter(moment().quarter()).startOf('quarter').format('YYYY-MM-DD');
          break;
      case 'MTD':
          rangeValue = moment().startOf('months').format('YYYY-MM-DD');
          break;
      default:
          rangeValue = '';
    }
    this.setRangeOption.emit(rangeValue);
  }
}
