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


import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';

import { BillingReportService, HealthStatusService }  from './../core/services';
import { ReportingGridComponent } from './reporting-grid/reporting-grid.component';
import { ToolbarComponent } from './toolbar/toolbar.component';

import { DICTIONARY, ReportingConfigModel } from '../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-reporting',
  template: `
  <dlab-navbar [healthStatus]="healthStatus" [billingEnabled]="billingEnabled"></dlab-navbar>
  <dlab-toolbar (rebuildReport)="rebuildBillingReport($event)" (exportReport)="exportBillingReport()" (setRangeOption)="setRangeOption($event)"></dlab-toolbar>
  <dlab-reporting-grid (filterReport)="filterReport($event)" (resetRangePicker)="resetRangePicker($event)"></dlab-reporting-grid>
  <footer *ngIf="data">
    Total {{ data[DICTIONARY.billing.costTotal] }} {{ data[DICTIONARY.billing.currencyCode] }}
  </footer>
  `,
  styles: [`
    footer {
      position: fixed;
      left: 0px;
      bottom: 0px;
      width: 100%;
      background: #a1b7d1;
      color: #ffffff;
      text-align: right;
      padding: 5px 15px;
      font-size: 18px;
      box-shadow: 0 9px 18px 15px #f5f5f5;
    }
  `]
})
export class ReportingComponent implements OnInit, OnDestroy {
  readonly DICTIONARY = DICTIONARY;

  @ViewChild(ReportingGridComponent) reportingGrid: ReportingGridComponent;
  @ViewChild(ToolbarComponent) reportingToolbar: ToolbarComponent;

  reportData: ReportingConfigModel = ReportingConfigModel.getDefault();
  filterConfiguration: ReportingConfigModel = ReportingConfigModel.getDefault();
  data: any;
  healthStatus: any;
  billingEnabled: boolean;

  constructor(
    private billingReportService: BillingReportService,
    private healthStatusService: HealthStatusService,) { }

  ngOnInit() {
    this.rebuildBillingReport();
    this.getEnvironmentHealthStatus();
  }

  ngOnDestroy() {
    this.clearStorage();
  }

  getGeneralBillingData() {
    localStorage.removeItem('report_config');

    this.billingReportService.getGeneralBillingData(this.reportData)
      .subscribe(data => {
        this.data = data;
        this.reportingGrid.reportData = this.data.lines;
        this.reportingGrid.full_report = this.data.full_report;

        this.reportingToolbar.reportData = this.data;
        if (!localStorage.getItem('report_period')) {
          localStorage.setItem('report_period' , JSON.stringify({start_date: this.data[DICTIONARY.billing.dateFrom], end_date: this.data[DICTIONARY.billing.dateTo]}));
          this.reportingToolbar.setDateRange();
        }

        if (localStorage.getItem('report_config')) {
          this.filterConfiguration = JSON.parse(localStorage.getItem('report_config'));
          this.reportingGrid.setConfiguration(this.filterConfiguration);
        } else {
          this.getDefaultFilterConfiguration(this.data);
        }
     });
  }

  rebuildBillingReport($event?): void {
    this.clearStorage();
    this.resetRangePicker();
    this.reportData.defaultConfigurations();

    this.getEnvironmentHealthStatus();
    this.getGeneralBillingData();
  }

  exportBillingReport($event): void {
    this.billingReportService.downloadReport(this.reportData)
      .subscribe(data => this.downloadFile(data));
  }

  downloadFile(data: any) {
    const fileName = data.headers.get('content-disposition').match(/filename="(.+)"/)[1];

    let parsedResponse = data.text();
    let blob = new Blob([parsedResponse], { type: 'text/csv' });
    let url = window.URL.createObjectURL(blob);

    if (navigator.msSaveOrOpenBlob) {
        navigator.msSaveBlob(blob, fileName);
    } else {
        let a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }
    window.URL.revokeObjectURL(url);
  }

  getDefaultFilterConfiguration(data): void {
    const users = [], types = [], shapes = [], services = [];

    data.lines.forEach((item: any) => {
      if (item.user && users.indexOf(item.user) === -1)
        users.push(item.user);

      if (item[DICTIONARY.billing.resourceType] && types.indexOf(item[DICTIONARY.billing.resourceType]) === -1)
        types.push(item[DICTIONARY.billing.resourceType]);

      if (item[DICTIONARY.billing.instance_size]) {
        if (item[DICTIONARY.billing.instance_size].indexOf('Master') > -1) {
          for (let shape of item[DICTIONARY.billing.instance_size].split('\n')) {
              shape = shape.replace('Master: ', '');
              shape = shape.replace(/Slave:\s+\d+ x /, '');
              shape = shape.replace(/\s+/g, '');

              shapes.indexOf(shape) === -1 && shapes.push(shape);
          }
        } else if (item[DICTIONARY.billing.instance_size].match(/\d x \S+/)) {
          let parsedShape = item[DICTIONARY.billing.instance_size].match(/\d x \S+/)[0].split(' x ')[1];
          if (shapes.indexOf(parsedShape) === -1) {
            shapes.push(parsedShape);
          }
        } else {
          shapes.indexOf(item[DICTIONARY.billing.instance_size]) === -1 && shapes.push(item[DICTIONARY.billing.instance_size]);
        }
      }

      if (item[DICTIONARY.billing.service] && services.indexOf(item[DICTIONARY.billing.service]) === -1)
        services.push(item[DICTIONARY.billing.service]);
    });
    this.filterConfiguration = new ReportingConfigModel(users, services, types, shapes, '', '', '');
    this.reportingGrid.setConfiguration(this.filterConfiguration);
    localStorage.setItem('report_config' , JSON.stringify(this.filterConfiguration));
  }

  filterReport(event: ReportingConfigModel): void {
    this.reportData = event;
    this.getGeneralBillingData();
  }

  resetRangePicker() {
    this.reportingToolbar.clearRangePicker();
  }

  clearStorage(): void {
    localStorage.removeItem('report_config');
    localStorage.removeItem('report_period');
  }

  setRangeOption(dateRangeOption: any): void {
    this.reportData.date_start = dateRangeOption.start_date;
    this.reportData.date_end = dateRangeOption.end_date;
    this.getGeneralBillingData();
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => {
        this.healthStatus = result.status;
        this.billingEnabled = result.billingEnabled;
      });
  }
}
