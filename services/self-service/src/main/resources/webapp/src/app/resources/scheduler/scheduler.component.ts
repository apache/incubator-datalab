/***************************************************************************

Copyright (c) 2018, EPAM SYSTEMS INC

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

import { Component, OnInit, ViewChild, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { FormGroup, FormControl, FormArray, FormBuilder } from '@angular/forms';

import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MomentDateAdapter } from '@angular/material-moment-adapter';

import * as _moment from 'moment';
import { HTTP_STATUS_CODES } from './../../core/util';

import { SchedulerService } from './../../core/services';
import { SchedulerModel, WeekdaysModel } from './scheduler.model';

@Component({
  selector: 'dlab-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SchedulerComponent implements OnInit {
  public model: SchedulerModel;
  public selectedWeekDays: WeekdaysModel = new WeekdaysModel(false, false, false, false, false, false, false);
  public notebook: any;
  public errorMessage: boolean = false;
  public date_format: string = 'YYYY-MM-DD';
  public timeFormat: string = 'HH:mm';
  public weekdays: string[] = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  public schedulerForm: FormGroup;

  public startTime: any;
  public endTime: any;
  @ViewChild('bindDialog') bindDialog;
  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    private formBuilder: FormBuilder,
    private schedulerService: SchedulerService
  ) {}

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  public open(param, notebook): void {
    this.notebook = notebook;

    if (!this.bindDialog.isOpened)
      this.model = new SchedulerModel(
        (response: Response) => {
          if (response.status === HTTP_STATUS_CODES.OK) {
            this.close();
            this.buildGrid.emit();
          }
        },
        error => {},
        () => {
          this.bindDialog.open(param);
          this.formInit();

          this.selectedWeekDays.setDegault();
          this.getExploratorySchedule();
        },
        this.schedulerService
      );
  }

  public onDaySelect($event, day) {
    this.errorMessage = false;
    this.selectedWeekDays[day.toLowerCase()] = $event.checked;
  }

  public scheduleInstance_btnClick(data) {
    const selectedDays = Object.keys(this.selectedWeekDays);

    if (!selectedDays.some(el => this.selectedWeekDays[el])) {
      this.errorMessage = true;
      return false;
    }
    const parameters = {
      begin_date: _moment(data.startDate).format(this.date_format),
      finish_date: _moment(data.finishDate).format(this.date_format),
      start_time: this.convertTimeFormat(this.startTime),
      end_time: this.convertTimeFormat(this.endTime),
      days_repeat: selectedDays.filter(el => Boolean(this.selectedWeekDays[el])).map(day => day.toUpperCase()),
      timezone_offset: _moment().format('Z')
    };
    this.model.confirmAction(this.notebook, parameters);
  }

  public close(): void {
    if (this.bindDialog.isOpened) this.bindDialog.close();

    this.resetDialog();
  }

  private formInit(start?, end?) {
    this.schedulerForm = this.formBuilder.group({
      startDate: start ? _moment(start).format() : _moment(new Date()).format(),
      finishDate: end ? _moment(end).format() : _moment(new Date()).add(1, 'days').format()
    });
  }

  private getExploratorySchedule() {
    this.schedulerService.getExploratorySchedule(this.notebook.name).subscribe(
      (params: any) => {
        if (params) {
          params.days_repeat.filter(
            key => (this.selectedWeekDays[key.toLowerCase()] = true)
          );

          this.startTime = this.convertTimeFormat(params.start_time);
          this.endTime = this.convertTimeFormat(params.end_time);

          this.formInit(params.begin_date, params.finish_date);
        }
      },
      error => {}
    );
  }

  private convertTimeFormat(time24: any) {

    let result;
    if (typeof time24 === 'string') {
      let spl = time24.split(':');

      result = {
        hour: +spl[0] % 12 || 12,
        minute: +spl[1],
        meridiem: +spl[0] < 12 || +spl[0] === 24 ? 'AM' : 'PM'
      };
    } else {
      let hours = time24.hour;
      let minutes = (time24.minute < 10) ? time24.minute + '0' : time24.minute;

      if (time24.meridiem == 'PM' && time24.hour < 12) hours = time24.hour + 12;
      if (time24.meridiem == 'AM' &&  time24.hour == 12) hours = time24.hour - 12;
      hours = hours < 10 ? hours + '0' : hours;

      result = `${hours}:${minutes}`;
    }
    return result;
  }

  private resetDialog() {
    this.errorMessage = false;
    this.startTime = this.convertTimeFormat('00:00');
    this.endTime = this.convertTimeFormat('00:00');
  }
}
