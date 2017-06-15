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

import { Component, OnInit, ViewChild } from '@angular/core';

import { BillingReportService }  from './../core/services';
import { ReportingFilterConfigurationModel }  from './reporting-data.model';
import { ReportingGridComponent } from './reporting-grid/reporting-grid.component';
import { ToolbarComponent } from './toolbar/toolbar.component';

@Component({
  selector: 'dlab-reporting',
  template: `
  <dlab-navbar></dlab-navbar>
  <dlab-toolbar (rebuildReport)="getGeneralBillingData()"></dlab-toolbar>
  <dlab-reporting-grid (filterReport)="filterReport($event)"></dlab-reporting-grid>
  <footer>
    Total {{data?.cost_total}} {{data?.currency_code}}
  </footer>
  `,
  styles: [`
    footer {
      position: fixed;
      left: 0px;
      bottom: 0px;
      height: 30px;
      width: 100%;
      background: #f9fafb;
      color: #36b1d8;
      text-align: right;
      padding: 30px 40px;
      font-size: 20px;
    }
  `]
})
export class ReportingComponent implements OnInit {

  @ViewChild(ReportingGridComponent) reportingGrid: ReportingGridComponent;
  @ViewChild(ToolbarComponent) reportingToolbar: ToolbarComponent;

  reportDataConfig: ReportingFilterConfigurationModel = new ReportingFilterConfigurationModel([],[],[],[],'','');
  data: any;

  constructor(private billingReportService: BillingReportService) { }

  ngOnInit() {
    this.getGeneralBillingData();
  }

  getGeneralBillingData() {
    this.billingReportService.getGeneralBillingData(this.reportDataConfig)
      .subscribe(data => {
        this.data = data;
        this.reportingGrid.reportData = this.data.lines;
        this.reportingToolbar.reportData = this.data;

        this.getDefaultFilterConfiguration(this.data);
     });
  }

  getDefaultFilterConfiguration(data): void {
    console.log(data);
    const users = [], types = [], shapes = [], services = [];

    data.lines.forEach((item: any) => {
      if (item.user && users.indexOf(item.user) === -1)
        users.push(item.user);

      if (item.dlab_resource_type && types.indexOf(item.dlab_resource_type) === -1)
        types.push(item.dlab_resource_type);

      if (item.shape && shapes.indexOf(item.shape) === -1)
        shapes.push(item.shape);

      if (item.product && services.indexOf(item.product) === -1)
        services.push(item.product);
    });
    console.log('users: ' + users);
    console.log('types: ' + types);
    console.log('shapes: ' + shapes);
    console.log('services: ' + services);
    this.reportingGrid.setConfiguration(users, types, shapes, services);
  }

  filterReport(event: ReportingFilterConfigurationModel): void {
    console.log('ReportingComponent ',event);
    this.reportDataConfig = event;
    this.getGeneralBillingData();
  }
}
