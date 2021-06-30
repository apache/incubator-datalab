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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';


@Component({
  selector: 'edge-action-dialog',
  template: `
  <div id="dialog-box">
    <header class="dialog-header">
      <h4 class="modal-title"><span class="action">{{data.type | titlecase}}</span> Odahu cluster</h4>
      <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
    </header>
      <div mat-dialog-content class="content message mat-dialog-content">
          <h3>Odahu cluster <span class="strong">{{data.item.name}} </span>will be {{label[data.type]}}</h3>
      <p class="m-top-20 action-text"><span class="strong">Do you want to proceed?</span></p>

      <div class="text-center m-top-30 m-bott-30">
        <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
        <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
      </div>
      </div>
  </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400; margin: 0; }
    .info .confirm-dialog { color: #607D8B; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    h3 { font-weight: 300; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
    .action{text-transform: capitalize}
    .action-text { text-align: center; }
    label { font-size: 15px; font-weight: 500; font-family: "Open Sans",sans-serif; cursor: pointer; display: flex; align-items: center;}
    label input {margin-top: 2px; margin-right: 5px;}
  `]
})

export class OdahuActionDialogComponent {
  public label = {
    stop: 'stopped',
    start: 'started',
    terminate: 'terminated',
  };

  constructor(
    public dialogRef: MatDialogRef<OdahuActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) {
  }
}
