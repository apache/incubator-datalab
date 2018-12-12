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

import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

@Component({
  selector: 'notification-dialog',
  template: `
  <header>
    <h4><i class="material-icons">priority_high</i>Warning</h4>
    <a class="ani" (click)="dialogRef.close()"><i class="material-icons">close</i></a>
  </header>
  <div mat-dialog-content class="content info message">
    <div *ngIf="data.type === 'list'; else info">
      <div *ngIf="data.template.notebook.length > 0">
        Following notebook server
        <span *ngIf="data.template.notebook.length > 1">s </span>
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
    <ng-template #info><span [innerHTML]="data.template"></span></ng-template>
  </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400 }
    .info { color: #35afd5; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
  `]
})
export class NotificationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<NotificationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
