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
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from '@angular/material/dialog';
import {AuditService} from '../../../core/services/audit.service';

@Component({
  selector: 'dlab-audit-grid',
  templateUrl: './audit-grid.component.html',
  styleUrls: ['./audit-grid.component.scss', '../../../resources/resources-grid/resources-grid.component.scss'],

})
export class AuditGridComponent implements OnInit {
  public auditData: Array<object>;
  public displayedColumns: string[] = ['date', 'user', 'action', 'project', 'resource-type', 'resource', 'buttons'];
  public displayedFilterColumns: string[] = ['action-filter', 'user-filter', 'actions-filter',  'project-filter', 'resource-type-filter', 'resource-filter', 'filter-buttons'];
  public collapseFilterRow: boolean = false;
  public filterConfiguration: FilterAuditModel = new FilterAuditModel([], [], [], [], [], '', '');
  public filterAuditData: FilterAuditModel = new FilterAuditModel([], [], [], [], [], '', '');
  public itemsPrPage: Number[] = [25, 50, 100];
  public showItemsPrPage: number;
  public firstItem: number = 1;
  public lastItem: number;
  public allItems: number;
  private copiedFilterAuditData: FilterAuditModel;
  public isNavigationDisabled;


  constructor(
    public dialogRef: MatDialogRef<AuditInfoDialogComponent>,
    public dialog: MatDialog,
    private auditService: AuditService,
  ) {
  }

  ngOnInit() {}

  public buildAuditGrid(filter?) {
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

  public getAuditData(filter?) {
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
    });
  }

  public refreshAuditPage() {
    this.filterAuditData = this.copiedFilterAuditData;
    this.getAuditData();
  }

  public setAvaliblePeriod(period) {
    this.filterAuditData.date_start = period.start_date;
    this.filterAuditData.date_end = period.end_date;
  }

  toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }

  onUpdate($event): void {
    this.filterAuditData[$event.type] = $event.model;
  }

  openActionInfo(element) {
    this.dialog.open(AuditInfoDialogComponent, { data: {data: element.info, action: element.action}, panelClass: 'modal-xl-m' });
  }

  public setItemsPrPage(item: number) {
    window.localStorage.setItem('audit_per_page', item.toString());
    this.firstItem = 1;
    if (this.lastItem !== item) {
      this.lastItem = item;
      this.buildAuditGrid();
    }
  }

  private changePage(action) {
    if (action === 'first') {
      this.firstItem = 1;
      this.lastItem = this.showItemsPrPage;
    } else if (action === 'previous') {
      this.firstItem = this.firstItem - this.showItemsPrPage;
      this.lastItem = this.lastItem % this.showItemsPrPage === 0 ?
        this.lastItem - this.showItemsPrPage : this.lastItem - (this.lastItem % this.showItemsPrPage);
    } else if (action === 'next') {
      this.firstItem = this.firstItem + this.showItemsPrPage;
      this.lastItem = (this.lastItem + this.showItemsPrPage) > this.allItems ?
        this.allItems : this.lastItem + this.showItemsPrPage;
    } else if (action === 'last') {
      this.firstItem = this.allItems % this.showItemsPrPage === 0 ?
        this.allItems - this.showItemsPrPage : this.allItems - (this.allItems % this.showItemsPrPage) + 1;
      this.lastItem = this.allItems;
    }
  }

  public loadItemsForPage(action) {
    this.changePage(action);
    this.buildAuditGrid();
  }

  public resetFilterConfigurations() {
    this.filterAuditData = FilterAuditModel.getDefault();
    this.buildAuditGrid(true);
  }

  public didFilterChanged() {
    this.isNavigationDisabled = JSON.stringify(this.copiedFilterAuditData) !== JSON.stringify(this.filterAuditData);
    return this.isNavigationDisabled;
  }
}


@Component({
  selector: 'audit-info-dialog',
  template: `
      <div id="dialog-box">
          <header class="dialog-header">
              <h4 class="modal-title">{{data.action | convertaction}}</h4>
              <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
          </header>
          <div mat-dialog-content class="content audit-info-content" [ngClass]="{'pb-40': actionList[0].length > 1}">
            <mat-list *ngIf="actionList[0].length > 1;else message">
              <mat-list-item class="list-header">
                <div class="info-item-title">Action</div>
                <div class="info-item-data"> Description </div>
              </mat-list-item>
              <div class="scrolling-content mat-list-wrapper" id="scrolling">
                <mat-list-item class="list-item" *ngFor="let action of actionList">
                  <div *ngIf="(data.action === 'upload' && action[0] === 'File(s)') || (data.action === 'download' && action[0] === 'File(s)');else multiAction" class="info-item-title">File</div>
                  <ng-template #multiAction>
                     <div class="info-item-title">{{action[0]}}</div>
                  </ng-template>
                  <div class="info-item-data" *ngIf="action[0] === 'File(s)'">
                    <div class="file-description ellipsis" *ngFor="let description of action[1]?.split(',')" [matTooltip]="description" matTooltipPosition="above">
                      {{description}}
                    </div>
                  </div>
                  <div class="info-item-data" *ngIf="action[0] !== 'File(s)'">
                     <div *ngFor="let description of action[1]?.split(',')">{{description}}</div>
                  </div>
                </mat-list-item>
              </div>
            </mat-list>
            <ng-template #message>
              <div class="message-wrapper">
                <p>{{data.data}}.</p>
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
    .list-item{color: #718ba6; height: auto; line-height: 20px;}
    .info-item-title{width: 40%; padding: 10px 0}
    .list-header {padding-top: 5px;}
    .info-item-data{width: 60%; text-align: left; padding: 10px 0}
    .file-description{ overflow: hidden; display: block; direction: rtl;}





  `]
})
export class AuditInfoDialogComponent {
  actionList;
  constructor(
    public dialogRef: MatDialogRef<AuditInfoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.actionList = data.data.split('\n').map(v => v.split(':')).filter(v => v[0] !== '');
  }

}
