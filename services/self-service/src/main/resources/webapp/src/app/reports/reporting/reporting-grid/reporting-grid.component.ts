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

import {
  Component,
  OnInit,
  Output,
  EventEmitter,
  ViewChild,
  Input,
  HostListener,
  ChangeDetectorRef,
  ChangeDetectionStrategy
} from '@angular/core';
import { ReportingConfigModel } from '../../../../dictionary/global.dictionary';
import {BehaviorSubject, Subject } from 'rxjs';
import {CompareUtils} from '../../../core/util/compareUtils';

export interface IFullReport {
  currency: string;
  from: Array<number>;
  is_full: boolean;
  name: string;
  reportHeaderCompletable: true;
  report_lines:  Array<any>;
  sbn: string;
  to: Array<number>;
  total_cost: number;
}

@Component({
  selector: 'datalab-reporting-grid',
  templateUrl: './reporting-grid.component.html',
  styleUrls: ['./reporting-grid.component.scss',
    '../../../resources/resources-grid/resources-grid.component.scss'],
  // changeDetection: ChangeDetectionStrategy.OnPush

})
export class ReportingGridComponent implements OnInit {
  tableEl = {};
  filterConfiguration: ReportingConfigModel;
  // filteredReportData: ReportingConfigModel = new ReportingConfigModel([], [], [], [], [], '', '', '', []);
  collapseFilterRow: boolean = false;
  reportData: Array<any> = [];
  fullReport: IFullReport;
  isFiltered: boolean = false;
  active: object = {};
  displayedColumns: string[] = [
    'name', 'user', 'project',
    'type', 'status', 'shape',
    'service', 'empty', 'charge'
  ];
  displayedFilterColumns: string[] = [
    'name-filter', 'user-filter', 'project-filter',
    'type-filter', 'status-filter', 'shape-filter',
    'service-filter', 'empty-filter', 'actions'
  ];
  filtered: any;
  isMaxRight: Subject<boolean> = new BehaviorSubject(false);
  isFilterSelected: boolean;
  isFilterChanged: boolean;
  public isScrollButtonsVisible: boolean;
  public previousItem: string;
  public previousDirection: string;
  userAgentIndex: number;

  @ViewChild('nameFilter') filter;
  @ViewChild('tableWrapper') tableWrapper;
  @ViewChild('wrapper') wrapper;

  @ViewChild('pageWrapper') pageWrapper;
  @ViewChild('table') table;
  @Output() filterReport: EventEmitter<{}> = new EventEmitter();
  @Output() resetRangePicker: EventEmitter<boolean> = new EventEmitter();
  @Input() filteredReportData: ReportingConfigModel;

  @Input() previousFilterData: ReportingConfigModel;
  @HostListener('window:resize', ['$event'])
  onResize(event) {
    this.isScrollButtonsVisible = this.tableWrapper.nativeElement.offsetWidth - this.table._elementRef.nativeElement.offsetWidth < 0;
    this.checkMaxRight();
  }

  @HostListener('scroll', ['$event'])
  scrollTable($event: Event) {
    this.checkMaxRight();
  }

  constructor(private changeDetector: ChangeDetectorRef) { }

  ngOnInit() {
    this.userAgentIndex = window.navigator.userAgent.indexOf('Firefox');

    window.setTimeout(() => {
      this.isScrollButtonsVisible = this.tableWrapper.nativeElement.offsetWidth - this.table._elementRef.nativeElement.offsetWidth < 0;
      this.checkMaxRight();
      this.tableEl = this.table._elementRef.nativeElement;
    }, 1000);
    this.checkFilters();
  }

  onUpdate($event): void {
    this.filteredReportData[$event.type] = $event.model;
    this.checkFilters();
  }

  private checkFilters() {
    this.isFilterChanged = CompareUtils.compareFilters(this.filteredReportData, this.previousFilterData);
    this.isFilterSelected = Object.keys(this.filteredReportData)
      .some(v => this.filteredReportData[v] && this.filteredReportData[v].length > 0);
  }

  refreshData(fullReport, report) {
    this.reportData = [...report];
    this.fullReport = fullReport;
    this.checkFilters();
  }

  setFullReport(data): void {
    if (!data) {
      this.displayedColumns = this.displayedColumns.filter(el => el !== 'user');
      this.displayedFilterColumns = this.displayedFilterColumns.filter(el => el !== 'user-filter');
    }
  }

  sortBy(sortItem, direction) {
    if (this.previousItem === sortItem && this.previousDirection === direction) return;

    let report: Array<object>;
    this.previousItem = sortItem;
    this.previousDirection = direction;

    if (direction === 'down') {
      report = this.reportData.sort((a, b) => {
        if (a[sortItem] === null) a[sortItem] = '';
        if (b[sortItem] === null) b[sortItem] = '';

        if ((a[sortItem] > b[sortItem])) return 1;
        if ((a[sortItem] < b[sortItem])) return -1;
        return 0;
      });
    }

    if (direction === 'up') {
      report = this.reportData.sort((a, b) => {
        if (a[sortItem] === null) a[sortItem] = '';
        if (b[sortItem] === null) b[sortItem] = '';

        if ((a[sortItem] < b[sortItem])) return 1;
        if ((a[sortItem] > b[sortItem])) return -1;
        return 0;
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
    this.checkFilters();
  }

  filter_btnClick(): void {
    this.filterReport.emit(this.filteredReportData);
    this.isFiltered = true;
    this.checkFilters();
    this.removeSorting();
  }

  resetFiltering(): void {
    this.filteredReportData.defaultConfigurations();
    this.removeSorting();
    this.filter.nativeElement.value = '';
    this.filterReport.emit(this.filteredReportData);
    this.resetRangePicker.emit(true);
    this.checkFilters();
  }

  shapeSplit(shape) {
    return shape.split(/(?=Slave)/g);
  }

  public scrollTo(direction: string) {
    if (direction === 'left') {
      this.tableWrapper.nativeElement.scrollLeft = 0;
      this.pageWrapper.nativeElement.scrollLeft = 0;
    } else {
      this.tableWrapper.nativeElement.scrollLeft = this.tableWrapper.nativeElement.offsetWidth;
      this.pageWrapper.nativeElement.scrollLeft = this.pageWrapper.nativeElement.offsetWidth;
    }
  }

  public checkMaxRight() {
    const arg = this.tableWrapper.nativeElement.offsetWidth +
      this.tableWrapper.nativeElement.scrollLeft + 2 <= this.table._elementRef.nativeElement.offsetWidth;
    return this.isMaxRight.next(arg);

  }

  public onFilterNameUpdate(targetElement: any) {
    this.filteredReportData.datalabId = targetElement;
    this.checkFilters();
  }
}
