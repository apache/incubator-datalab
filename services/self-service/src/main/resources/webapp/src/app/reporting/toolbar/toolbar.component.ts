/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Component, OnInit, AfterViewInit, Output, EventEmitter, ViewEncapsulation, ViewChild } from '@angular/core';
import { NgDateRangePickerOptions } from 'ng-daterangepicker';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import {skip} from 'rxjs/operators';
import {Subscription} from 'rxjs';
import {HealthStatusService} from '../../core/services';
import {GeneralEnvironmentStatus} from '../../administration/management/management.model';

@Component({
  selector: 'dlab-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ToolbarComponent implements OnInit, AfterViewInit {
  readonly DICTIONARY = DICTIONARY;
  value: any;
  reportData: any;
  availablePeriodFrom: [];
  availablePeriodTo: [];
  subscriptions: Subscription = new Subscription();
  healthStatus: GeneralEnvironmentStatus;

  rangeOptions = { 'YTD': 'Year To Date', 'QTD': 'Quarter To Date', 'MTD': 'Month To Date', 'reset': 'All Period Report' };
  options: NgDateRangePickerOptions;
  rangeLabels: any;

  @Output() rebuildReport: EventEmitter<{}> = new EventEmitter();
  @Output() exportReport: EventEmitter<{}> = new EventEmitter();
  @Output() setRangeOption: EventEmitter<{}> = new EventEmitter();

  constructor(private healthStatusService: HealthStatusService) {
    this.options = {
      theme: 'default',
      range: 'tm',
      dayNames: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      presetNames: ['This Month', 'Last Month', 'This Week', 'Last Week', 'This Year', 'Last Year', 'From', 'To'],
      dateFormat: 'dd MMM y',
      outputFormat: 'YYYY/MM/DD',
      startOfWeek: 1
    };
  }

  ngOnInit() {
    if (localStorage.getItem('report_period')) {
      const availableRange = JSON.parse(localStorage.getItem('report_period'));
      this.availablePeriodFrom = availableRange.start_date;
      this.availablePeriodTo = availableRange.end_date;    }
    this.subscriptions.add(this.healthStatusService.statusData.pipe(skip(1)).subscribe(result => {
      this.healthStatus = result;
    }));
  }

  ngAfterViewInit() {
    this.clearRangePicker();
  }

  setDateRange() {
    const availableRange = JSON.parse(localStorage.getItem('report_period'));

    this.availablePeriodFrom = availableRange.start_date;
    this.availablePeriodTo = availableRange.end_date;
  }

  clearRangePicker(): void {
    const rangeLabels = <NodeListOf<Element>>document.querySelectorAll('.value-txt');

    for (let label = 0; label < rangeLabels.length; ++label)
      rangeLabels[label].classList.add('untouched');
  }

  onChange(dateRange: string): void {
    const rangeLabels = <NodeListOf<Element>>document.querySelectorAll('.value-txt');

    for (let label = 0; label < rangeLabels.length; ++label)
      if (rangeLabels[label].classList.contains('untouched')) {
        rangeLabels[label].classList.remove('untouched');
      }

    const reportDateRange = dateRange.split('-');
    this.setRangeOption.emit({
      start_date: reportDateRange[0].split('/').join('-'),
      end_date: reportDateRange[1].split('/').join('-')
    });
  }

  rebuild($event): void {
    this.rebuildReport.emit($event);
  }

  export($event): void {
    this.exportReport.emit($event);
  }
}
