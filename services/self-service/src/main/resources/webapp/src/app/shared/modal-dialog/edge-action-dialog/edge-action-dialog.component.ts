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

import {Component, Inject, OnDestroy} from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';


@Component({
  selector: 'edge-action-dialog',
  template: `
  <div id="dialog-box" *ngIf="data.type">
    <header class="dialog-header">
      <h4 class="modal-title"><span class="action">{{data.type | titlecase}}</span> edge node</h4>
      <button type="button" class="close" (click)="this.dialogRef.close()">&times;</button>
    </header>
      <div mat-dialog-content class="content message mat-dialog-content" >
        <div *ngIf="data.item.length > 1; else oneNode">
          <h3 class="strong">Select the edge nodes you want to {{data.type}}</h3>
          <ul class="endpoint-list scrolling-content">
            <li *ngIf="data.item.length>1" class="endpoint-list-item header-item">
              <span class="strong all item-wrapper" (click)="chooseAll()">
                <div class="empty-checkbox" [ngClass]="{'checked': isAllChecked || isSomeSelected}">
                  <span class="checked-checkbox" *ngIf="isAllChecked"></span>
                  <span class="line-checkbox" *ngIf="isSomeSelected"></span>
                </div>
                <span class="pl-5">{{data.type | titlecase}} all</span>
              </span>
            </li>
            <div class="scrolling-content" id="scrolling">
              <li *ngFor="let endpoint of data.item" class="endpoint-list-item">
                <span class="strong item-wrapper" (click)="endpointAction(endpoint)">
                  <div class="empty-checkbox" [ngClass]="{'checked': endpoint.checked}">
                    <span class="checked-checkbox" *ngIf="endpoint.checked"></span>
                  </div>
                  <!--                    <input type="checkbox" [(ngModel)]="endpoint.checked" (change)="endpointAction()">      -->
                  <span class="pl-5">{{endpoint.name}}</span>
                </span>
              </li>
            </div>
          </ul>
        </div>
        <ng-template #oneNode>
          Edge node <span class="strong">{{data.item[0].name}}</span> will be {{data.type === 'stop' ? 'stopped' : data.type === 'start' ? 'started' : 'recreated'}}
        </ng-template>

      <p class="m-top-20 action-text"><span class="strong">Do you want to proceed?</span></p>

      <div class="text-center m-top-30 m-bott-30">
        <button type="button" class="butt" mat-raised-button (click)="this.dialogRef.close()">No</button>
        <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(endpointsNewStatus)" [disabled]="!endpointsNewStatus.length">Yes</button>
      </div>
      </div>
  </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400; margin: 0; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
    h3.strong{ margin-top: 10px;}
    .endpoint-list{text-align: left; margin-top: 15px}
    .endpoint-list-item{padding: 5px 20px}
    .action{text-transform: capitalize}
    .action-text { text-align: center; }
    .scrolling-content{overflow-y: auto; max-height: 200px; }
    .item-wrapper { font-size: 15px; font-weight: 300; font-family: "Open Sans",sans-serif; cursor: pointer; display: flex; align-items: center; padding-left: 10px}
    .item-wrapper .empty-checkbox {margin-top: 0}
    .all{font-size: 16px; padding-left: 0; font-weight: 500}
    .scrolling-content{overflow-y: auto; max-height: 200px;}
  `]
})

export class EdgeActionDialogComponent implements OnDestroy {
  public endpointsNewStatus: Array<object> = [];
  public isAllChecked: boolean;
  public isSomeSelected: boolean;
  constructor(
    public dialogRef: MatDialogRef<EdgeActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) {
    if (this.data.item.length === 1) {
      this.endpointsNewStatus = this.data.item;
    }
  }

  public endpointAction(target?) {
    if (target) target.checked = !target.checked;
    this.endpointsNewStatus = this.data.item.filter(endpoint => endpoint.checked);
    this.isAllChecked = this.endpointsNewStatus.length === this.data.item.length;
    this.isSomeSelected = this.endpointsNewStatus.length && !this.isAllChecked;
  }

  public chooseAll() {
    if (!this.isAllChecked) {
      this.data.item.forEach(endpoint => endpoint.checked = true);
    } else {
      this.clearCheckedNodes();
    }
    this.endpointAction();
  }

  public clearCheckedNodes() {
    this.data.item.forEach(endpoint => endpoint.checked = false);
  }

  ngOnDestroy(): void {
    this.clearCheckedNodes();
    this.isAllChecked = false;
  }
}
