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

import {Component, EventEmitter, Inject, OnInit, Output} from '@angular/core';
import {FilterAuditModel} from '../filter-audit.model';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from '@angular/material/dialog';
import {AuditService} from '../../../core/services/audit.service';
import {SortUtils} from '../../../core/util';
import {CompareUtils} from '../../../core/util/compareUtils';

export interface AuditItem {
  action: string;
  info: string;
  project: string;
  resourceName: string;
  timestamp: number;
  type: string;
  user: string;
  _id: string;
}

@Component({
  selector: 'datalab-audit-grid',
  templateUrl: './audit-grid.component.html',
  styleUrls: ['./audit-grid.component.scss', '../../../resources/resources-grid/resources-grid.component.scss'],

})
export class AuditGridComponent implements OnInit {
  public auditData: Array<AuditItem>;
  public displayedColumns: string[] = ['date', 'user', 'action', 'project', 'resource-type', 'resource', 'buttons'];
  public displayedFilterColumns: string[] = ['date-filter', 'user-filter', 'actions-filter',  'project-filter', 'resource-type-filter', 'resource-filter', 'filter-buttons'];
  public collapseFilterRow: boolean = false;
  public filterConfiguration: FilterAuditModel = new FilterAuditModel([], [], [], [], [], '', '');
  public filterAuditData: FilterAuditModel = new FilterAuditModel([], [], [], [], [], '', '');
  public itemsPrPage: Number[] = [25, 50, 100];
  public showItemsPrPage: number;
  public firstItem: number = 1;
  public lastItem: number;
  public allItems: number;
  private copiedFilterAuditData: FilterAuditModel;
  public isNavigationDisabled: boolean;
  public isFilterSelected: boolean;

  @Output() resetDateFilter: EventEmitter<any> = new EventEmitter();

  constructor(
    public dialogRef: MatDialogRef<AuditInfoDialogComponent>,
    public dialog: MatDialog,
    private auditService: AuditService,
  ) { }

  ngOnInit() {}

  public buildAuditGrid(filter?: boolean): void {
    if (!this.showItemsPrPage) {
      if (window.localStorage.getItem('audit_per_page')) {
        this.showItemsPrPage = +window.localStorage.getItem('audit_per_page');
      } else {
        this.showItemsPrPage = 50;
      }
      this.lastItem = this.showItemsPrPage;
    }
   this.getAuditData(filter);
  }

  public getAuditData(filter?: boolean): void {
    const page = filter ? 1 : Math.ceil(this.lastItem / this.showItemsPrPage);
    this.copiedFilterAuditData = JSON.parse(JSON.stringify(this.filterAuditData));
    this.auditService.getAuditData(this.filterAuditData, page, this.showItemsPrPage).subscribe(auditData => {
      if (filter) this.changePage('first');
      this.auditData = auditData[0].audit;
      this.allItems = auditData[0]['page_count'];
      this.filterConfiguration = new FilterAuditModel(
        auditData[0].user_filter.filter(v => v),
        auditData[0].resource_name_filter.filter(v => v),
        auditData[0].resource_type_filter.filter(v => v),
        auditData[0].project_filter.filter(v => v),
        [],
        '',
        ''
      );
      this.checkFilters();
    });
  }

  public refreshAuditPage(): void {
    this.filterAuditData = this.copiedFilterAuditData;
    this.getAuditData();
  }

  public setAvaliblePeriod(period): void {
    this.filterAuditData.date_start = period.start_date;
    this.filterAuditData.date_end = period.end_date;
    this.buildAuditGrid(true);
  }

  public toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }

  public onUpdate($event): void {
    this.filterAuditData[$event.type] = $event.model;
    this.checkFilters();
  }

  private checkFilters(): void {
    this.isNavigationDisabled = CompareUtils.compareFilters(this.filterAuditData, this.copiedFilterAuditData);
    this.isFilterSelected = Object.keys(this.filterAuditData).some(v => this.filterAuditData[v].length > 0);
  }

  public openActionInfo(element: AuditItem): void {
    if (element.type === 'GROUP' && element.info.indexOf('role') !== -1) {
      this.dialog.open(AuditInfoDialogComponent,
        { data: { element, dialogSize: 'big' }, panelClass: 'modal-xl-m' });
    } else {
      this.dialog.open(AuditInfoDialogComponent,
        { data:  {element, dialogSize: 'small' }, panelClass: 'modal-md' });
    }
  }

  public setItemsPrPage(item: number): void {
    window.localStorage.setItem('audit_per_page', item.toString());
    this.firstItem = 1;
    if (this.lastItem !== item) {
      this.lastItem = item;
      this.buildAuditGrid();
    }
  }

  private changePage(action: string): void {
    if (action === 'first') {
      this.firstItem = 1;
      this.lastItem = this.showItemsPrPage;
    } else if (action === 'previous') {
      this.firstItem = this.firstItem - this.showItemsPrPage;
      this.lastItem = this.lastItem % this.showItemsPrPage === 0
        ? this.lastItem - this.showItemsPrPage
        : this.lastItem - (this.lastItem % this.showItemsPrPage);
    } else if (action === 'next') {
      this.firstItem = this.firstItem + this.showItemsPrPage;
      this.lastItem = (this.lastItem + this.showItemsPrPage) > this.allItems
        ? this.allItems
        : this.lastItem + this.showItemsPrPage;
    } else if (action === 'last') {
      this.firstItem = this.allItems % this.showItemsPrPage === 0
        ? this.allItems - this.showItemsPrPage
        : this.allItems - (this.allItems % this.showItemsPrPage) + 1;
      this.lastItem = this.allItems;
    }
  }

  public loadItemsForPage(action: string): void {
    this.changePage(action);
    this.buildAuditGrid();
  }

  public resetFilterConfigurations(): void {
    this.filterAuditData = FilterAuditModel.getDefault();
    this.resetDateFilter.emit();
    this.buildAuditGrid(true);
  }
}

@Component({
  selector: 'audit-info-dialog',
  template: `
      <div id="dialog-box">
          <header class="dialog-header">
              <h4 class="modal-title">{{data.element.action | convertaction}}</h4>
              <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
          </header>
          <div
            mat-dialog-content
            class="content audit-info-content"
            [ngClass]="{'pb-40': actionList[0].length > 1}"
          >
            <mat-list
              *ngIf="actionList[0].length > 1 && data.element.action !== 'FOLLOW_LINK'
                    || data.element.info.indexOf('Update quota') !== -1;else message"
            >
              <ng-container *ngIf="data.element.info.indexOf('Update quota') === -1;else quotas">

                <mat-list-item class="list-header">
                  <div class="info-item-title" [ngClass]="{'same-column-width': data.dialogSize === 'small'}">Action</div>
                  <div class="info-item-data" [ngClass]="{'same-column-width': data.dialogSize === 'small'}"> Description </div>
                </mat-list-item>

                <div class="scrolling-content mat-list-wrapper" id="scrolling">
                  <mat-list-item class="list-item" *ngFor="let action of actionList">
                    <div
                      *ngIf="(data.element.action === 'upload' && action[0] === 'File(s)')
                          || (data.element.action === 'download' && action[0] === 'File(s)');else multiAction"
                      class="info-item-title"
                    >
                      File
                    </div>
                    <ng-template #multiAction>
                       <div class="info-item-title" [ngClass]="{'same-column-width': data.dialogSize === 'small'}">{{action[0]}}</div>
                    </ng-template>
                    <div
                      class="info-item-data"
                      [ngClass]="{'same-column-width': data.dialogSize === 'small'}"
                      *ngIf="action[0] === 'File(s)'"
                    >
                      <div
                        class="file-description ellipsis"
                        *ngFor="let description of action[1]?.split(',')"
                        [matTooltip]="description"
                        matTooltipPosition="above"
                      >
                        {{description}}
                      </div>
                    </div>
                    <div class="info-item-data" [ngClass]="{'same-column-width': data.dialogSize === 'small'}" *ngIf="action[0] !== 'File(s)'">
                       <div
                          *ngFor="let description of action[1]?.split(',')"
                          [matTooltip]="description"
                          class="ellipsis"
                          [ngStyle]="description.length < 20 ? {'width' :'fit-content'} : {'width':'100%'}"
                          matTooltipPosition="above"
                          matTooltipClass="mat-tooltip-description"
                      >
                        {{description}}
                        </div>
                    </div>
                  </mat-list-item>
                </div>
              </ng-container>

              <ng-template #quotas>
                <mat-list-item class="list-header">
                  <div class="same-column-width" >Action</div>
                  <div class="info-item-title"> Previous value </div>
                  <div class="info-item-quota"> New value </div>
                </mat-list-item>
                <div class="scrolling-content mat-list-wrapper" id="scrolling">
                  <mat-list-item class="list-item" *ngFor="let action of updateBudget">
                    <div class="same-column-width">{{action[0]}}</div>
                    <div class="info-item-title">
                      {{action[1]}}
                    </div>
                    <div class="info-item-quota">
                      {{action[2]}}
                    </div>
                  </mat-list-item>
                </div>
              </ng-template>
            </mat-list>
            <ng-template #message>
              <div class="message-wrapper">
                <p *ngIf="data.element.type !== 'COMPUTE'; else computation">
                  <span *ngIf="data.element.info.indexOf('Scheduled') !== -1;else notScheduledNotebook">{{data.element.action | titlecase}} by scheduler.</span>
                  <ng-template #notScheduledNotebook>
                    <span *ngIf="data.element.type === 'WEB_TERMINAL'">{{data.element.info}} <span class="strong">{{data.element.resourceName}}</span>.</span>
                    <span *ngIf="data.element.type !== 'WEB_TERMINAL' && data.element.type !== 'EDGE_NODE'">{{data.element.info}}.</span>
                    <span *ngIf="data.element.type === 'EDGE_NODE' && data.element.action === 'CREATE'">
                      Create edge node for endpoint <span class="strong">{{data.element.resourceName}}</span>, requested in project <span class="strong">{{data.element.project}}</span>.
                    </span>
                  </ng-template>
                </p>
                <ng-template #computation>
                  <p *ngIf="data.element.info.indexOf('Scheduled') !== -1;else notScheduled"> {{data.element.action | titlecase}} by scheduler, requested for notebook <span class="strong">{{data.element.info.split(' ')[data.element.info.split(' ').length - 1] }}</span>.</p>
                  <ng-template #notScheduled>
                    <p>
                      <span *ngIf="data.element.action === 'FOLLOW_LINK'">{{info.action | titlecase}} compute <span class="strong">{{info.cluster}}</span>&nbsp;<span>{{info.clusterType}}</span> link, requested for notebook <span class="strong">{{info.notebook}}</span>.</span>
                      <span *ngIf="data.element.action !== 'FOLLOW_LINK'">{{data.element.action | titlecase}} compute <span class="strong">{{data.element.resourceName}}</span>, requested for notebook <span class="strong">{{data.element.info.split(' ')[data.element.info.split(' ').length - 1] }}</span>.</span>
                    </p>
                    </ng-template>
                </ng-template>
              </div>
          </ng-template></div>
      </div>
  `,

  styles: [`
    .content { color: #718ba6; padding: 20px 30px; font-size: 14px; font-weight: 400; margin: 0; }
    .pb-40 {  padding-bottom: 40px; }
    .info .confirm-dialog { color: #607D8B; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
    .scrolling-content{overflow-y: auto; max-height: 200px; }
    label{cursor: pointer}
    .message-wrapper{min-height: 70px;; display: flex; align-items: center}
    .mat-list-wrapper{padding-top: 5px;}
    .list-item{color: #718ba6; height: auto; line-height: 20px; font-size: 14px}
    .list-item:not(:last-child) { border-bottom:1px solid #edf1f5;}
    .info-item-title{width: 40%; padding: 10px 0;font-size: 14px;}
    .info-item-quota{width: 30%; padding: 10px 0;font-size: 14px;}
    .list-header {padding-top: 5px;}
    .info-item-data{width: 60%; text-align: left; padding: 10px 0; font-size: 14px; cursor: default;}
    .file-description{ overflow: hidden; display: block; direction: rtl; font-size: 14px;}
    .same-column-width{width: 50%; padding: 10px 0; font-size: 14px;}
  `]
})
export class AuditInfoDialogComponent {
  actionList: string[][];
  updateBudget: string[][] = [];
  info;
  constructor(
    public dialogRef: MatDialogRef<AuditInfoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    if (data.element.action === 'FOLLOW_LINK' && data.element.type === 'COMPUTE') {
      this.info = JSON.parse(data.element.info);
    }
    if (data.element.info.indexOf('Update quota') !== -1) {
      this.updateBudget = data.element.info.split('\n').reduce((acc, v, i, arr) => {
        const row = v.split(':').map((el, index) => {
          if (el.indexOf('->') !== -1) {
            el = el.split('->');
          } else if (index === 1 && el.indexOf('->') === -1) {
            el = ['', el];
          }
          return el;
        });
        acc.push(SortUtils.flatDeep(row, 1));
        return acc;
      }, []);
      // this.data.element.info.replace(/->/g, ' ').split('\n').forEach( (val, j) => {
      //   this.updateBudget[j] = [];
      //   val.split(' ')
      //     .forEach((v, i, arr) => {
      //       if (arr[0] === 'Update') {
      //         if (i === 1) {
      //           this.updateBudget[j].push(`${arr[0]} ${arr[1]}`);
      //         }
      //         if (i > 1) {
      //           this.updateBudget[j].push(arr[i]);
      //         }
      //       } else {
      //         this.updateBudget[j].push(arr[i]);
      //       }
      //
      //     });
      // });
    }
    this.actionList = data.element.info.split('\n').map(v => v.split(':')).filter(v => v[0] !== '');
  }
}
