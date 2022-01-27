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

import { Component, Input, Output, OnInit, EventEmitter  } from '@angular/core';

import { CLOCK_TYPE, TimeFormat } from './ticker.component';
type TimeFormatAlias = TimeFormat;

@Component({
  selector: 'time-cover',
  template: `
    <div class="time-cover">
      <mat-toolbar class="container">
        <div class="selected-time">
          <span [class]="currentTemplate === VIEW_HOURS ? 'active': ''"
                (click)="setCurrentTemplate(VIEW_HOURS)">{{ pickTime.hour }}:</span>
          <span [class]="currentTemplate === VIEW_MINUTES ? 'active': ''"
                (click)="setCurrentTemplate(VIEW_MINUTES)">{{ formatMinute() }}</span>
        </div>
        <div class="selected-meridiem">
          <span (click)="setMeridiem('AM')" [class]="pickTime.meridiem === 'AM' ? 'active' : ''">AM</span>
          <span (click)="setMeridiem('PM')" [class]="pickTime.meridiem === 'PM' ? 'active' : ''">PM</span>
        </div>
      </mat-toolbar>
      <div class="cover-block">
        <ticker class="animation" [pickTime]="pickTime" (pickTimeChange)="emitpickTimeSelection($event)" [(currentTemplate)]="currentTemplate" (viewChange)="setCurrentTemplate($event)"></ticker>
        <div class="actions">
          <button mat-raised-button class="butt mini" (click)="revert()">Cancel</button>
          <button mat-raised-button class="butt mini butt-success" (click)="assignTime()">Assign</button>
        </div>
      </div>
    </div>`,
  styleUrls: ['./time-picker.component.scss']
})
export class TimeCoverComponent implements OnInit {

  @Input() pickTime: TimeFormatAlias;
  @Output() pickTimeChange: EventEmitter<TimeFormatAlias> = new EventEmitter();

  @Output() onReset: EventEmitter<null> = new EventEmitter();
  @Output() onConfirm: EventEmitter<TimeFormatAlias> = new EventEmitter();

  public VIEW_HOURS = CLOCK_TYPE.HOURS;
  public VIEW_MINUTES = CLOCK_TYPE.MINUTES;
  public currentTemplate: CLOCK_TYPE = this.VIEW_HOURS;

  constructor() {}

  ngOnInit() {
    if (!this.pickTime) {
      this.pickTime = {
        hour: 0,
        minute: 0,
        meridiem: 'AM'
      };
    }
  }

  public formatMinute(): string {
    if (this.pickTime.minute < 10) {
      return '0' + String(this.pickTime.minute);
    } else {
      return String(this.pickTime.minute);
    }
  }

  public setCurrentTemplate(type: CLOCK_TYPE) {
    this.currentTemplate = type;
  }

  public setMeridiem(meridiem: 'PM' | 'AM') {
    console.log('setMeridiem: ', meridiem);

    this.pickTime.meridiem = meridiem;
  }

  public revert() {
    this.onReset.emit();
  }

  public assignTime() {
    this.onConfirm.emit(this.pickTime);
  }

  public emitpickTimeSelection(event) {
    this.pickTimeChange.emit(this.pickTime);
  }
}
