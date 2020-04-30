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

import {Component, OnInit, Output, EventEmitter, ViewChild, Input} from '@angular/core';
import { ReportingConfigModel } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-reporting-grid',
  templateUrl: './reporting-grid.component.html',
  styleUrls: ['./reporting-grid.component.scss',
    '../../../resources/resources-grid/resources-grid.component.scss'],

})
export class ReportingGridComponent implements OnInit {

  filterConfiguration: ReportingConfigModel;
  filteredReportData: ReportingConfigModel = new ReportingConfigModel([], [], [], [], [], '', '', '', []);
  collapseFilterRow: boolean = true;
  reportData: Array<any> = [];
  fullReport: Array<any>;
  isFiltered: boolean = false;
  active: object = {};

  @ViewChild('nameFilter', { static: false }) filter;

  @Output() filterReport: EventEmitter<{}> = new EventEmitter();
  @Output() resetRangePicker: EventEmitter<boolean> = new EventEmitter();
  displayedColumns: string[] = ['name', 'user', 'project', 'type', 'status', 'shape', 'service', 'charge'];
  displayedFilterColumns: string[] = ['name-filter', 'user-filter', 'project-filter', 'type-filter', 'status-filter', 'shape-filter', 'service-filter', 'actions'];
  filtered: any;

  ngOnInit() {}

  onUpdate($event): void {
    this.filteredReportData[$event.type] = $event.model;
  }

  refreshData(fullReport, report) {
    this.reportData = [...report];
    this.fullReport = fullReport;
  }

  setFullReport(data): void {
    if (!data) {
      this.displayedColumns = this.displayedColumns.filter(el => el !== 'user');
      this.displayedFilterColumns = this.displayedFilterColumns.filter(el => el !== 'user-filter');
    }
  }

  sortBy(sortItem, direction) {
  let report: Array<object>;
  if (direction === 'down') {
    report = this.reportData.sort((a, b) => {
      if (a[sortItem] === null) a = '';
      if (b[sortItem] === null) b = '';
     return (a[sortItem] > b[sortItem]) ? 1 : -1;
    });
  }
  if (direction === 'up') {
    report = this.reportData.sort((a, b) => {
      if (a[sortItem] === null) a = '';
      if (b[sortItem] === null) b = '';
      return (a[sortItem] < b[sortItem]) ? 1 : -1 ;
    });
  }
  this.refreshData(this.fullReport, report);
  this.removeSorting();
  this.active[sortItem + direction] = true;
  }

  removeSorting() {
    for (const item in this.active) {
      this.active[item] = false;
    }
  }

  toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }

  setConfiguration(reportConfig: ReportingConfigModel): void {
    this.filterConfiguration = reportConfig;
  }

  filter_btnClick(): void {
    this.filterReport.emit(this.filteredReportData);
    this.isFiltered = true;
    this.removeSorting();
  }

  resetFiltering(): void {
    this.filteredReportData.defaultConfigurations();
    this.removeSorting();
    this.filter.nativeElement.value = '';
    this.filterReport.emit(this.filteredReportData);
    this.resetRangePicker.emit(true);
  }

  shapeSplit(shape) {
    return shape.split(/(?=Slave)/g);
  }
}
