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

import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

import { DICTIONARY, ReportingConfigModel } from '../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-reporting-grid',
  templateUrl: './reporting-grid.component.html',
  styleUrls: ['./reporting-grid.component.scss',
              '../../resources/resources-grid/resources-grid.component.css']
})
export class ReportingGridComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  filterConfiguration: ReportingConfigModel;
  filteredReportData: ReportingConfigModel = new ReportingConfigModel([], [], [], [], [], '', '', '');
  collapseFilterRow: boolean = false;
  reportData: ReportingConfigModel[];
  isFiltered: boolean = false;
  full_report: boolean = false;

  @Output() filterReport: EventEmitter<{}> = new EventEmitter();
  @Output() resetRangePicker: EventEmitter<boolean> = new EventEmitter();

  public filteringColumns: Array<any> = [
    { title: 'User', name: 'user', className: 'th_user', filtering: true, role: 'admin'},
    { title: 'Environment name', name: 'dlab_id', className: 'env_name', filtering: true },
    { title: 'Resource Type', name: 'resource_type', className: 'th_type', filtering: true },
    { title: 'Status', name: 'status', className: 'th_rstatus', filtering: true },
    { title: DICTIONARY.instance_size, name: DICTIONARY.billing.instance_size, className: 'th_shape', filtering: true },
    { title: DICTIONARY.service, name: DICTIONARY.billing.service_filter_key, className: 'service', filtering: true },
    { title: 'Service Charges', name: 'charges', className: 'th_charges', filtering: false }
  ];

  ngOnInit() { }

  onUpdate($event): void {
    this.filteredReportData[$event.type] = $event.model;
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
  }

  resetFiltering(): void {
    this.filteredReportData.defaultConfigurations();
    
    this.filterReport.emit(this.filteredReportData);
    this.resetRangePicker.emit(true);
  }
}
