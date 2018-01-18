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

import { Component, OnInit, Inject, Input } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

@Component({
  selector: 'dlab-time-picker',
  template: `
  <button mat-icon-button (click)="openDatePickerDialog($event)">
    <mat-icon>access_time</mat-icon>
  </button>
  <mat-input-container>
    <input matInput class="timeInput" placeholder="Select time" id="time-picker" [value]="time" />
  </mat-input-container>
  `,
  styles: [
    `

  `
  ]
})
export class TimePickerComponent implements OnInit {
  @Input() pickTime: any;

  constructor(private dialog: MatDialog) {}

  ngOnInit() {
    console.log('TIME PICK INIT');
  }

  public openDatePickerDialog($event) {
    const dialogRef: MatDialogRef<TimePickerDialogComponent> = this.dialog.open(
      TimePickerDialogComponent,
      { data: {} }
    );

    dialogRef.afterClosed().subscribe(val => console.log('CLOSED ', val));
  }

  private get time(): string {
    return !this.pickTime ? '' : `${this.pickTime.hour}:${this.pickTime.minute} ${this.pickTime.meriden}`;
  }
}

@Component({
  selector: 'time-picker-dialog',
  template: `
  <div mat-dialog-content class="content time-dialog">
  hello!!!
  </div>
  `,
  styles: [
    `
  .content { color: #36afd5; padding: 20px 50px; font-size: 14px; font-weight: 400 }
`
  ]
})
export class TimePickerDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<TimePickerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) private data: any
  ) {}
}
