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


import {Component, OnInit, OnDestroy, ViewChild, Inject} from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import {HealthStatusService} from '../../core/services';
import { DICTIONARY} from '../../../dictionary/global.dictionary';
import {AuditToolbarComponent} from './audit-toolbar/audit-toolbar.component';
import {AuditGridComponent} from './audit-grid/audit-grid.component';
import {AuditService} from '../../core/services/audit.service';
import {Endpoint} from '../../administration/project/project.component';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';


@Component({
  selector: 'dlab-reporting',
  template: `
  <div class="base-retreat">
<!--    <dlab-toolbar (rebuildReport)="rebuildBillingReport()"-->
<!--                  (exportReport)="exportBillingReport()"-->
<!--                  (setRangeOption)="setRangeOption($event)">-->
<!--    </dlab-toolbar>-->
<!--    <mat-divider></mat-divider>-->
<!--    <dlab-reporting-grid (filterReport)="filterReport($event)" (resetRangePicker)="resetRangePicker()"></dlab-reporting-grid>-->
    <audit-toolbar (rebuildAudit)="rebuildAuditGrid()" (setRangeOption)="setRangeOption($event)">
    </audit-toolbar>
    <mat-divider></mat-divider>
    <dlab-audit-grid (resetDateFilter)="resetDatepicker()"></dlab-audit-grid>
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
export class AuditComponent implements OnInit, OnDestroy {
  readonly DICTIONARY = DICTIONARY;

  @ViewChild(AuditGridComponent, { static: true }) auditGrid: AuditGridComponent;
  @ViewChild(AuditToolbarComponent, { static: true }) auditToolbar: AuditToolbarComponent;

  public availablePeriod: any;

  constructor(
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.buildAuditReport();
  }

  ngOnDestroy() {
  }

  public buildAuditReport() {
    this.auditGrid.buildAuditGrid();
  }

  public rebuildAuditGrid() {
    this.auditGrid.refreshAuditPage();
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe();
  }

  public setRangeOption(event) {
    this.auditGrid.setAvaliblePeriod(event);
  }

  public resetDatepicker() {
    this.auditToolbar.clearRangePicker();
  }
}
