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

import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';

export enum CLOCK_TYPE { HOURS = 1, MINUTES = 2 }

export interface TimeFormat {
  hour: number;
  minute: number;
  meridiem: 'PM' | 'AM';
}

@Component({
  selector: 'ticker',
  template: `
  <div class="ticker-wrap">
    <div class="ticker">
      <div class="ticker-container">
        <button mat-mini-fab class="ticker-center"></button>
        <mat-toolbar [ngStyle]="getPointerStyle()" class="pointer">
          <button mat-mini-fab class="ticker-selected"></button>
        </mat-toolbar>
        <div *ngFor="let step of steps; let i = index" [class]="getTimeValueClass(step, i)" >
          <button
            mat-mini-fab
            [color]="selectedTimePart === step ? color : ''"
            (click)="changeTimeValue(step)"
          >
            {{ step }}
          </button>
        </div>
      </div>
    </div>
  </div>`,
  styleUrls: ['./time-picker.component.scss']
})
export class TickerComponent implements OnChanges {
  @Input() public pickTime: TimeFormat;
  @Output() public pickTimeChange: EventEmitter<TimeFormat> = new EventEmitter();

  @Input() public currentTemplate: CLOCK_TYPE;
  @Output() public viewChange = new EventEmitter<CLOCK_TYPE>();

  @Input() public color: string;

  public steps = new Array<number>();
  public selectedTimePart;

  private format: number = 12;
  private degrees: number;

  ngOnChanges() {
    this.degrees = 360 / this.format;
    this.setupUI();
  }

  private setupUI() {
    this.steps = new Array<number>();
    switch (this.currentTemplate) {
      case CLOCK_TYPE.HOURS:
        for (let i = 1; i <= this.format; i++) {
          this.steps.push(i);
          this.selectedTimePart = this.pickTime.hour || 0;
          if (this.selectedTimePart > this.format) {
            this.selectedTimePart -= this.format;
          }
        }
        break;

      case CLOCK_TYPE.MINUTES:
        for (let i = 5; i <= 55; i += 5) {
          this.steps.push(i);
        }
        this.steps.push(0);
        this.selectedTimePart = this.pickTime.minute || 0;
        break;
    }
  }

  public getPointerStyle() {
    let divider = 1;
    switch (this.currentTemplate) {
      case CLOCK_TYPE.HOURS:
        divider = this.format;
        break;

      case CLOCK_TYPE.MINUTES:
        divider = 60;
        break;
    }

    let degrees = 0;
    if (this.currentTemplate === CLOCK_TYPE.HOURS) {
      degrees = Math.round(this.pickTime.hour * (360 / divider)) - 180;
    } else {
      degrees = Math.round(this.pickTime.minute * (360 / divider)) - 180;
    }

    const style = {
      '-webkit-transform': 'rotate(' + degrees + 'deg)',
      '-ms-transform': 'rotate(' + degrees + 'deg)',
      transform: 'rotate(' + degrees + 'deg)'
    };

    return style;
  }

  public getTimeValueClass(step: number, index: number) {
    let classes = 'ticker-step ticker-deg' + this.degrees * (index + 1);
    if (this.selectedTimePart === step) classes += ' mat-primary';

    return classes;
  }

  public changeTimeValue(step: number) {
    if (this.currentTemplate === CLOCK_TYPE.HOURS) {
      this.pickTime.hour = step;
      this.viewChange.emit(CLOCK_TYPE.MINUTES);
    } else {
      this.pickTime.minute = step;
      this.viewChange.emit(CLOCK_TYPE.HOURS);
    }
    this.pickTimeChange.emit(this.pickTime);
  }
}
