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

import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

@Component({
  selector: 'notification-dialog',
  template: `
  <div id="dialog-box">
    <header class="dialog-header">
      <h4 class="modal-title"><i class="material-icons">priority_high</i>Warning</h4>
      <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
    </header>
    <div mat-dialog-content class="content message">
      <div *ngIf="data.type === 'list'" class="info">
        <div *ngIf="data.template.notebook.length > 0">
          Following notebook server<span *ngIf="data.template.notebook.length>1">s </span>
          <span *ngFor="let item of data.template.notebook">
            <b>{{ item.exploratory_name }}</b>
            <span *ngIf="data.template.notebook.length > 1">, </span>
          </span> will be stopped and all computational resources will be stopped/terminated
        </div>

        <div *ngIf="data.template.cluster.length > 0">
          <p *ngFor="let item of data.template.cluster">
              Computational resource<span *ngIf="data.template.cluster.length > 1">s </span>
              <b>{{ item.computational_name }}</b> on <b>{{ item.exploratory_name }}</b>
              will be stopped
          </p>
        </div>
        <strong>by a schedule in 15 minutes.</strong>
      </div>
      <div *ngIf="data.type === 'message'"><span [innerHTML]="data.template"></span></div>
      <div *ngIf="data.type === 'confirmation'" class="confirm-dialog">
        <p><strong>{{ data.item.name }}</strong> will be decommissioned.</p>
        <p class="m-top-20"><strong>Do you want to proceed?</strong></p>
      
        <div class="text-center m-top-30 m-bott-10">
          <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
          <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
        </div>
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
  `]
})
export class NotificationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<NotificationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    console.log(data);
    
  }
}
