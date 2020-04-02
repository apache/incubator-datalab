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


import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import {ApplicationSecurityService, BillingReportService, HealthStatusService} from '../core/services';
import { ReportingGridComponent } from './reporting-grid/reporting-grid.component';
import { ToolbarComponent } from './toolbar/toolbar.component';

import { FileUtils } from '../core/util';
import { DICTIONARY, ReportingConfigModel } from '../../dictionary/global.dictionary';
import {ProgressBarService} from '../core/services/progress-bar.service';

@Component({
  selector: 'dlab-reporting',
  template: `
  <div class="base-retreat">
    <dlab-toolbar (rebuildReport)="rebuildBillingReport()"
                  (exportReport)="exportBillingReport()"
                  (setRangeOption)="setRangeOption($event)">
    </dlab-toolbar>
    <mat-divider></mat-divider>
    <dlab-reporting-grid (filterReport)="filterReport($event)" (resetRangePicker)="resetRangePicker()"></dlab-reporting-grid>
  </div>

  `,
  styles: [`
    footer {
      position: fixed;
      left: 0;
      bottom: 0;
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

  @ViewChild(ReportingGridComponent, { static: false }) reportingGrid: ReportingGridComponent;
  @ViewChild(ToolbarComponent, { static: true }) reportingToolbar: ToolbarComponent;

  reportData: ReportingConfigModel = ReportingConfigModel.getDefault();
  filterConfiguration: ReportingConfigModel = ReportingConfigModel.getDefault();
  data: any;
  billingEnabled: boolean;
  admin: boolean;

  constructor(
    private billingReportService: BillingReportService,
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
    private progressBarService: ProgressBarService,
    private applicationSecurityService: ApplicationSecurityService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.buildBillingReport();
  }

  ngOnDestroy() {
    this.clearStorage();
  }

  getGeneralBillingData() {
    setTimeout(() => {this.progressBarService.startProgressBar(); } , 0);
    this.billingReportService.getGeneralBillingData(this.reportData)
      .subscribe(data => {
        this.data = data;
        this.reportingGrid.refreshData(this.data, this.data.report_lines);
        this.reportingGrid.setFullReport(this.data.is_full);

        this.reportingToolbar.reportData = this.data;
        if (!localStorage.getItem('report_period')) {
          localStorage.setItem('report_period', JSON.stringify({
            start_date: this.data['from'],
            end_date: this.data['to']
          }));
          this.reportingToolbar.setDateRange();
        }

        if (localStorage.getItem('report_config')) {
          this.filterConfiguration = JSON.parse(localStorage.getItem('report_config'));
          this.reportingGrid.setConfiguration(this.filterConfiguration);
        } else {
          this.getDefaultFilterConfiguration(this.data);
        }
        this.progressBarService.stopProgressBar();
      }, () => this.progressBarService.stopProgressBar());
  }

  rebuildBillingReport(): void {
    this.checkAutorize();
    this.buildBillingReport();

  }

  buildBillingReport() {
    this.clearStorage();
    this.resetRangePicker();
    this.reportData.defaultConfigurations();
    this.getGeneralBillingData();
  }

  private checkAutorize() {
    this.applicationSecurityService.isLoggedIn().subscribe( () => {
        this.getEnvironmentHealthStatus();
      }
    );
  }

  exportBillingReport(): void {
    this.billingReportService.downloadReport(this.reportData)
      .subscribe(
        data => FileUtils.downloadFile(data),
        () => this.toastr.error('Billing report export failed!', 'Oops!'));
  }

  getDefaultFilterConfiguration(data): void {
    const users = [], types = [], shapes = [], services = [], statuses = [], projects = [];

    data.report_lines.forEach((item: any) => {
      if (item.user && users.indexOf(item.user) === -1)
        users.push(item.user);

      if (item.status && statuses.indexOf(item.status.toLowerCase()) === -1)
        statuses.push(item.status.toLowerCase());

      if (item.project && projects.indexOf(item.project) === -1)
        projects.push(item.project);

      if (item['resource_type'] && types.indexOf(item['resource_type']) === -1)
        types.push(item['resource_type']);

      if (item.shape && types.indexOf(item.shape)) {
        shapes.push(item.shape);
      }

      if (item.product && services.indexOf(item.product) === -1)
        services.push(item.product);
    });

    if (!this.reportingGrid.filterConfiguration || !localStorage.getItem('report_config')) {
      this.filterConfiguration = new ReportingConfigModel(users, services, types, statuses, shapes, '', '', '', projects);
      this.reportingGrid.setConfiguration(this.filterConfiguration);
      localStorage.setItem('report_config', JSON.stringify(this.filterConfiguration));
    }
  }

  filterReport(event: ReportingConfigModel): void {
    this.reportData = event;
    this.getGeneralBillingData();
  }

  resetRangePicker() {
    this.reportingToolbar.clearRangePicker();
  }

  setRangeOption(dateRangeOption: any): void {
    this.reportData.date_start = dateRangeOption.start_date;
    this.reportData.date_end = dateRangeOption.end_date;
    this.getGeneralBillingData();
  }

  private clearStorage(): void {
    localStorage.removeItem('report_config');
    localStorage.removeItem('report_period');
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => {
        this.billingEnabled = result.billingEnabled;
        this.admin = result.admin;
      });
  }
}
