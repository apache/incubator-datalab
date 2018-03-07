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
import { FormGroup, FormControl, FormArray, FormBuilder, Validators } from '@angular/forms';

import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MomentDateAdapter } from '@angular/material-moment-adapter';

import * as _moment from 'moment';
import { HTTP_STATUS_CODES } from './../../core/util';

import { SchedulerService } from './../../core/services';
import { SchedulerModel, WeekdaysModel } from './scheduler.model';

interface IDay {
  name: string;
  value: string;
}

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
  public weekdays: string[] = [ 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday' ];
  public schedulerForm : FormGroup;
    
  public startTime: any;
  public endTime: any;
  @ViewChild('bindDialog') bindDialog;
  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(private formBuilder: FormBuilder, private schedulerService: SchedulerService) { }

  ngOnInit() {
    this.formInit();
    console.log(_moment().format());
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  public open(param, notebook): void {
    this.notebook = notebook;
    
    if (!this.bindDialog.isOpened)
    this.model = new SchedulerModel((response: Response) => {
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
    this.schedulerService);
  }

  public onDaySelect($event, day) {
    this.errorMessage = false;

    this.selectedWeekDays[day.toLowerCase()] = $event.checked;
    console.log(this.selectedWeekDays); 
  }

  public scheduleInstance_btnClick(data) {
    const selectedDays = Object.keys(this.selectedWeekDays);

    if (!selectedDays.some(el => this.selectedWeekDays[el] )) {
      this.errorMessage = true;
      return false;
    }
    const parameters = {
      begin_date: _moment(data.startDate).format(this.date_format),
      finish_date: _moment(data.endDate).format(this.date_format),
      start_time: _moment(this.startTime).format(this.timeFormat),
      end_time: _moment(this.endTime).format(this.timeFormat),
      days_repeat: selectedDays.filter(el => Boolean(this.selectedWeekDays[el])).map((day => day.toUpperCase())),
      timezone_offset: _moment().format('Z')
    };
    this.model.confirmAction(this.notebook, parameters);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();

    this.resetDialog();
    }
    
    private formInit(start?, end?) {
      this.schedulerForm = this.formBuilder.group({
        startDate: start || new Date(),
        finishDate: end || _moment(new Date()).add(1,'days').format()
      });
    }
    
    private getExploratorySchedule() {
      this.schedulerService.getExploratorySchedule(this.notebook.name)
      .subscribe((params: any) => {
        if (params) {
          params.days_repeat.filter(key => this.selectedWeekDays[key.toLowerCase()] = true);

          this.startTime = this.setTimeFormat(params.start_time);
          this.endTime = this.setTimeFormat(params.end_time);

          this.formInit(params.begin_date, params.finish_date)
        }
      }, error => {
        debugger;
      });
    }

    private setTimeFormat(time24: string) {
      let spl = time24.split(':');

      return {
        hour: +spl[0] % 12 || 12,
        minute: +spl[1],
        meridiem: (+spl[0] < 12 || +spl[0] === 24) ? 'AM' : 'PM'
      };
    }

    private resetDialog() {
      this.errorMessage = false;
      this.startTime = this.setTimeFormat("00:00");
      this.endTime = this.setTimeFormat("00:00");
    }
}
