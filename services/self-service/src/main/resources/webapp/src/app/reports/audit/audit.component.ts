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


import {Component, OnInit, OnDestroy, ViewChild} from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import {HealthStatusService} from '../../core/services';
import { DICTIONARY} from '../../../dictionary/global.dictionary';
import {AuditToolbarComponent} from './audit-toolbar/audit-toolbar.component';
import {AuditGridComponent} from './audit-grid/audit-grid.component';

@Component({
  selector: 'datalab-reporting',
  template: `
    <div class="base-retreat">
      <audit-toolbar (rebuildAudit)="rebuildAuditGrid()" (setRangeOption)="setRangeOption($event)">
      </audit-toolbar>
      <mat-divider></mat-divider>
      <datalab-audit-grid (resetDateFilter)="resetDatepicker()"></datalab-audit-grid>
    </div>

  `,
  styles: [`
    footer {
      position: fixed;
      left: 0;
      bottom: 0;
      width: 100%;
      padding: 5px 15px;
      background: #a1b7d1;
      color: #ffffff;
      text-align: right;
      font-size: 18px;
      box-shadow: 0 9px 18px 15px #f5f5f5;
    }
  `]
})
export class AuditComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  @ViewChild(AuditGridComponent, { static: true }) auditGrid: AuditGridComponent;
  @ViewChild(AuditToolbarComponent, { static: true }) auditToolbar: AuditToolbarComponent;

  constructor(
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.buildAuditReport();
  }

  public buildAuditReport(): void {
    this.auditGrid.buildAuditGrid();
  }

  public rebuildAuditGrid(): void {
    this.auditGrid.refreshAuditPage();
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe();
  }

  public setRangeOption(event): void {
    this.auditGrid.setAvaliblePeriod(event);
  }

  public resetDatepicker(): void {
    this.auditToolbar.clearRangePicker();
  }
}
