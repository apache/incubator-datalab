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

import {Component, Inject, OnInit} from '@angular/core';
import {FilterAuditModel} from '../filter-audit.model';
import {NotificationDialogComponent} from '../../../shared/modal-dialog/notification-dialog';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from '@angular/material/dialog';
import {AuditService} from '../../../core/services/audit.service';

@Component({
  selector: 'dlab-audit-grid',
  templateUrl: './audit-grid.component.html',
  styleUrls: ['./audit-grid.component.scss'],

})
export class AuditGridComponent implements OnInit {
  public auditData: Array<object>;
  public displayedColumns: string[] = ['user', 'project', 'resource', 'action', 'date'];
  public displayedFilterColumns: string[] = ['user-filter', 'project-filter', 'resource-filter', 'action-filter', 'date-filter'];
  public collapseFilterRow: boolean = true;
  public filterConfiguration: FilterAuditModel = new FilterAuditModel([], [], [], [], '', '');
  public filterAuditData: FilterAuditModel = new FilterAuditModel([], [], [], [], '', '');

  constructor(
    public dialogRef: MatDialogRef<AuditInfoDialogComponent>,
    public dialog: MatDialog,
    private auditService: AuditService,
  ) {
  }

  ngOnInit() {}

  public refreshAudit() {
    this.auditService.getAuditData().subscribe(auditData => {
      this.auditData = auditData;
      this.createFilterData(this.auditData);
    });
  }

  public setAvaliblePeriod(period) {
    this.filterConfiguration.date_start = period.start_date;
    this.filterConfiguration.date_end = period.end_date;
  }

  public createFilterData (auditData) {
    const users = [];
    const resource = [];
    const project = [];
    const actions = [];
    auditData.forEach(auditItem => {
      if (!users.includes(auditItem.user)) {
        users.push(auditItem.user);
      }
      if (!resource.includes(auditItem.resourceName)) {
        resource.push(auditItem.resourceName);
      }
      if (!project.includes(auditItem.project)) {
        project.push(auditItem.project);
      }
      if (!actions.includes(auditItem.action)) {
        actions.push(auditItem.action);
      }
    });
    this.filterConfiguration = new FilterAuditModel(users, resource, project || [], actions, '', '');
  }

  toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }

  onUpdate($event): void {
    this.filterAuditData[$event.type] = $event.model;
  }

  openActionInfo(element) {
    // console.log('Open audit info ' + action.action);
    this.dialog.open(AuditInfoDialogComponent, { data: {data: element.info, action: element.action}, panelClass: 'modal-sm' });
  }
}

@Component({
  selector: 'audit-info-dialog',
  template: `
      <div id="dialog-box">
          <header class="dialog-header">
              <h4 class="modal-title">{{data.action}}</h4>
              <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
          </header>
          <div mat-dialog-content class="content">
            <ul info-items-list *ngIf="data.data.length>1;else message">
              <li class="info-item">
                  <span class="info-item-title">Group:</span>
                  <span class="info-item-data"> {{data.data.name}}</span>
              </li>
              <li class="info-item">
                <span class="info-item-title">Users:</span>
                <span class="info-item-data">
                    <span>{{data.data.objects}}</span>
                </span>
              </li>
            </ul>
            <ng-template #message>{{data.data[0]}}.</ng-template>
            <div class="text-center m-top-30 m-bott-10">
<!--               <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>-->
<!--               <button type="button" class="butt butt-success" mat-raised-button-->
<!--                       (click)="dialogRef.close(true)">Yes-->
<!--               </button>-->
             </div>
          </div>
      </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400; margin: 0; }
    .info { color: #35afd5; }
    .info .confirm-dialog { color: #607D8B; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
    .plur { font-style: normal; }
    .scrolling-content{overflow-y: auto; max-height: 200px; }
    .endpoint { width: 70%; text-align: left; color: #577289;}
    .status { width: 30%;text-align: left;}
    .label { font-size: 15px; font-weight: 500; font-family: "Open Sans",sans-serif;}
    .node { font-weight: 300;}
    .resource-name { width: 280px;text-align: left; padding: 10px 0;line-height: 26px;}
    .project { width: 30%;text-align: left; padding: 10px 0;line-height: 26px;}
    .resource-list{max-width: 100%; margin: 0 auto;margin-top: 20px; }
    .resource-list-header{display: flex; font-weight: 600; font-size: 16px;height: 48px; border-top: 1px solid #edf1f5; border-bottom: 1px solid #edf1f5; padding: 0 20px;}
    .resource-list-row{display: flex; border-bottom: 1px solid #edf1f5;padding: 0 20px;}
    .confirm-resource-terminating{text-align: left; padding: 10px 20px;}
    .confirm-message{color: #ef5c4b;font-size: 13px;min-height: 18px; text-align: center; padding-top: 20px}
    .checkbox{margin-right: 5px;vertical-align: middle; margin-bottom: 3px;}
    label{cursor: pointer}
    .bottom-message{padding-top: 15px;}
    .table-header{padding-bottom: 10px;}
    .info-item{display: flex; justify-content: space-between; padding: 10px 0; width: 100%}
    .info-item-title{width: 50%}
    .info-item-data{width: 50%; text-align: left;}


  `]
})
export class AuditInfoDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<AuditInfoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    console.log(data);
  }

}
