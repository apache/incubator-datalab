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

import { Component, OnInit, Inject, Input, Output, EventEmitter } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { CLOCK_TYPE, TimeFormat } from './ticker.component';
import {SchedulerCalculations} from '../../resources/scheduler/scheduler.calculations';
type TimeFormatAlias = TimeFormat;

@Component({
  selector: 'datalab-time-picker',
  template: `
    <div class="time-picker">
      <mat-form-field class="time-select">
        <input 
          matInput
          placeholder="{{ label }}"
          [value]="(selectedTime | localDate : 'shortTime') || null"
          (input)="checkEmpty($event.target['value'])"
          [disabled]="disable"
        />
        <mat-icon 
          matSuffix 
          [ngClass]="{'not-allowed': disable}" 
          (click)="openDatePickerDialog($event)" 
          disabled="disable"
        >
          access_time
        </mat-icon>
      </mat-form-field>
    </div>`,
  styleUrls: ['./time-picker.component.scss']
})
export class TimePickerComponent implements OnInit {
  @Input() pickTime: TimeFormatAlias;
  @Input() label: string = 'Select time';
  @Input() disable: boolean = false;
  @Input() milisecTime: number;
  @Output() pickTimeChange: EventEmitter<TimeFormatAlias> = new EventEmitter();
  @Output() milisecTimeChange: EventEmitter<TimeFormatAlias> = new EventEmitter();

  constructor(private dialog: MatDialog) { }

  ngOnInit() { }

  public get selectedTime(): string | number {
    return !this.pickTime ? '' : this.milisecTime;
  }

  public getFullMinutes() {
    return (Number(this.pickTime.minute) < 10) ? ('0' + this.pickTime.minute) : this.pickTime.minute;
  }

  public openDatePickerDialog($event) {
    const dialogRef = this.dialog.open(TimePickerDialogComponent, {
      data: {
        time: {
          hour: this.pickTime ? this.pickTime.hour : 0,
          minute: this.pickTime ? this.pickTime.minute : 0,
          meridiem: this.pickTime ? this.pickTime.meridiem : 'AM'
        }
      }
    });

    dialogRef.afterClosed().subscribe((result: TimeFormatAlias | -1) => {
      if (result === undefined) return;
      if (result !== -1) {
        this.pickTime = result;
        this.milisecTime = SchedulerCalculations.setTimeInMiliseconds(this.pickTime);
        this.emitpickTimeSelection();
      }
    });
    return false;
  }

  checkEmpty(searchValue: string ) {
    if (!searchValue.length) {
      this.pickTime = null;
      this.emitpickTimeSelection();
    }
  }

  private emitpickTimeSelection() {
    this.pickTimeChange.emit(this.pickTime);
  }
}

@Component({
  selector: 'time-picker-dialog',
  template: `
    <div mat-dialog-content class="timepicker-dialog">
      <time-cover [pickTime]="pickTime" (onReset)="cancel()" (onConfirm)="confirm()"></time-cover>
    </div>`,
  styles: [
    `.content { color: #36afd5; padding: 20px 50px; font-size: 14px; font-weight: 400 }`
  ]
})
export class TimePickerDialogComponent {
  public pickTime: TimeFormatAlias;
  private VIEW_HOURS = CLOCK_TYPE.HOURS;
  private VIEW_MINUTES = CLOCK_TYPE.MINUTES;
  private currentView: CLOCK_TYPE = this.VIEW_HOURS;

  constructor(
    @Inject(MAT_DIALOG_DATA) private data: { time: TimeFormatAlias; color: string },
    @Inject(MAT_DIALOG_DATA) public color: string,
    private dialogRef: MatDialogRef<TimePickerDialogComponent>
  ) {
    this.pickTime = data.time;
  }

  public cancel() {
    this.dialogRef.close(-1);
  }

  public confirm() {
    this.dialogRef.close(this.pickTime);
  }
}
