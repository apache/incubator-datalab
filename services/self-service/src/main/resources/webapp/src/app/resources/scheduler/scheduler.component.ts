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

import {Component, OnInit, ViewChild, ViewEncapsulation, ChangeDetectorRef, Inject, LOCALE_ID} from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import * as _moment from 'moment';
import 'moment-timezone';

import { SchedulerService } from '../../core/services';
import { SchedulerModel, WeekdaysModel } from './scheduler.model';
import { SchedulerCalculations } from './scheduler.calculations';
import { HTTP_STATUS_CODES, CheckUtils } from '../../core/util';
import { ScheduleSchema } from './scheduler.model';

@Component({
  selector: 'datalab-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SchedulerComponent implements OnInit {

  readonly CheckUtils = CheckUtils;

  public model: SchedulerModel;
  public selectedStartWeekDays: WeekdaysModel = WeekdaysModel.setDefault();
  public selectedStopWeekDays: WeekdaysModel = WeekdaysModel.setDefault();
  public notebook: any;
  public infoMessage: boolean = false;
  public timeReqiered: boolean = false;
  public terminateDataReqiered: boolean = false;
  public inherit: boolean = false;
  public allowInheritView: boolean = false;
  public parentInherit: boolean = false;
  public enableSchedule: boolean = false;
  public enableIdleTime: boolean = false;
  public enableIdleTimeView: boolean = false;
  public considerInactivity: boolean = false;
  public date_format: string = 'YYYY-MM-DD';
  public timeFormat: string = 'HH:mm';
  public weekdays: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  public schedulerForm: FormGroup;
  public destination: any;
  public zones: {};
  public tzOffset: string = _moment().format('Z');
  public startTime = SchedulerCalculations.convertTimeFormat('09:00');
  public startTimeMilliseconds: number = SchedulerCalculations.setTimeInMiliseconds(this.startTime);
  public endTime = SchedulerCalculations.convertTimeFormat('20:00');
  public endTimeMilliseconds: number = SchedulerCalculations.setTimeInMiliseconds(this.endTime);
  public terminateTime = null;
  public terminateTimeMilliseconds: number;

  public inactivityLimits = { min: 120, max: 10080 };
  public integerRegex: string = '^[0-9]*$';

  @ViewChild('resourceSelect') resource_select;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<SchedulerComponent>,
    private formBuilder: FormBuilder,
    private schedulerService: SchedulerService,
    private changeDetector: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.open(this.data.notebook, this.data.type, this.data.resource);
  }

  public open(notebook, type, resource?): void {
    this.notebook = notebook;
    this.zones = _moment.tz.names()
      .map(item => [_moment.tz(item).format('Z'), item])
      .sort()
      .reduce((memo, item) => {
        memo[item[0]] ? memo[item[0]] += `, ${item[1]}` : memo[item[0]] = item[1];
        return memo;
      }, {});

    this.model = new SchedulerModel(
      response => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.toastr.success('Schedule data were successfully saved', 'Success!');
          this.dialogRef.close();
        }
      },
      error => this.toastr.error(error.message || 'Scheduler configuration failed!', 'Oops!'),
      () => {
        this.formInit();
        this.changeDetector.detectChanges();
        this.destination = (type === 'EXPLORATORY') ? this.notebook : resource;
        this.destination.type = type;
        this.selectedStartWeekDays.reset();
        this.selectedStopWeekDays.reset();
        this.allowInheritView = false;

        if (this.destination.type === 'СOMPUTATIONAL') {
          this.allowInheritView = true;
          this.getExploratorySchedule(this.notebook.project, this.notebook.name, this.destination.computational_name);
          this.checkParentInherit();
        } else if (this.destination.type === 'EXPLORATORY') {
          this.allowInheritView = this.checkIsActiveSpark();
          this.getExploratorySchedule(this.notebook.project, this.notebook.name);
        }
      },
      this.schedulerService
    );
  }

  public onDaySelect($event, day, action) {
    if (action === 'start') {
      this.selectedStartWeekDays[day.toLowerCase()] = $event.source.checked;
    } else if (action === 'stop') {
      this.selectedStopWeekDays[day.toLowerCase()] = $event.source.checked;
    }
  }

  public toggleInherit($event) {
    this.inherit = $event.checked;

    if (this.destination.type === 'СOMPUTATIONAL' && this.inherit) {
      this.getExploratorySchedule(this.notebook.project, this.notebook.name);
      this.schedulerForm.get('startDate').disable();
      this.schedulerForm.get('finishDate').disable();
    } else {
      this.schedulerForm.get('startDate').enable();
      this.schedulerForm.get('finishDate').enable();
    }
  }

  public toggleSchedule($event) {
    this.enableSchedule = $event.checked;
    this.timeReqiered = false;
    this.allowInheritView = this.destination.type === 'СOMPUTATIONAL' || this.checkIsActiveSpark();

    this.enableSchedule && this.enableIdleTime && this.toggleIdleTimes({ checked: false });
    (this.enableSchedule && !(this.destination.type === 'СOMPUTATIONAL' && this.inherit))
      ? this.schedulerForm.get('startDate').enable()
      : this.schedulerForm.get('startDate').disable();

    this.enableSchedule && this.destination.type !== 'СOMPUTATIONAL' ?
      this.schedulerForm.get('finishDate').enable() : this.schedulerForm.get('finishDate').disable();
    this.enableSchedule ?
      this.schedulerForm.get('terminateDate').enable() : this.schedulerForm.get('terminateDate').disable();

    if (this.enableSchedule && $event.source) this.enableIdleTimeView = false;
  }

  public toggleIdleTimes($event) {
    const control = this.schedulerForm.controls.inactivityTime;

    this.enableIdleTime = $event.checked;
    this.enableIdleTime && this.enableSchedule && this.toggleSchedule({ checked: false });
    this.allowInheritView = false;

    if (!this.enableIdleTime) {
      this.allowInheritView = this.destination.type === 'СOMPUTATIONAL' || this.checkIsActiveSpark();
      control.setValue('');
    } else {
      !control.value && control.setValue(this.inactivityLimits.min);
      this.enableIdleTimeView = true;
    }
  }

  public setInactivity(...params) {
    this.model.setInactivityTime(params)
      .subscribe(
        (response: any) => {
          if (response.status === HTTP_STATUS_CODES.OK) {
            this.toastr.success('Schedule data were successfully saved', 'Success!');
            this.dialogRef.close();
          }
        },
        error => this.toastr.error(error.message || 'Scheduler configuration failed!', 'Oops!')
      );
  }

  public inactivityCounter($event, action: string): void {
    $event.preventDefault();
    const value = this.schedulerForm.controls.inactivityTime.value;
    const newValue = (action === 'increment' ? Number(value) + 10 : Number(value) - 10);
    this.schedulerForm.controls.inactivityTime.setValue(newValue);
  }

  public scheduleInstance_btnClick() {
    if (this.enableIdleTimeView) {
      this.enableIdleTime ? this.setScheduleByInactivity() : this.resetScheduler();
    } else {
      this.enableSchedule ? this.setScheduleByTime() : this.resetScheduler();
    }
  }

  private resetScheduler() {
    const resource = this.destination.type === 'СOMPUTATIONAL' ? this.destination.computational_name : null;
    this.model.resetSchedule(this.notebook.name, resource)
      .subscribe(() => {
        this.resetDialog();
        this.toastr.success('Schedule data were successfully deleted', 'Success!');
        this.dialogRef.close();
      });
  }

  private setScheduleByTime() {
    const data = {
      startDate: this.schedulerForm.controls.startDate.value,
      finishDate: this.schedulerForm.controls.finishDate.value,
      terminateDate: this.schedulerForm.controls.terminateDate.value
    };
    const terminateDateTime = (data.terminateDate && this.terminateTime)
      ? `${_moment(data.terminateDate).format(this.date_format)} ${SchedulerCalculations.convertTimeFormat(this.terminateTime)}`
      : null;

    if (!this.startTime && !this.endTime && !this.terminateTime && this.enableSchedule) {
      this.timeReqiered = true;
      return false;
    }

    if ((data.terminateDate && !this.terminateTime) || (!data.terminateDate && this.terminateTime)) {
      this.terminateDataReqiered = true;
      return false;
    }

    const selectedDays = Object.keys(this.selectedStartWeekDays);
    const parameters: ScheduleSchema = {
      begin_date: data.startDate ? _moment(data.startDate).format(this.date_format) : null,
      finish_date: data.finishDate ? _moment(data.finishDate).format(this.date_format) : null,
      start_time: this.startTime ? SchedulerCalculations.convertTimeFormat(this.startTime) : null,
      end_time: this.endTime ? SchedulerCalculations.convertTimeFormat(this.endTime) : null,
      start_days_repeat: selectedDays.filter(el => Boolean(this.selectedStartWeekDays[el])).map(day => day.toUpperCase()),
      stop_days_repeat: selectedDays.filter(el => Boolean(this.selectedStopWeekDays[el])).map(day => day.toUpperCase()),
      timezone_offset: this.tzOffset,
      sync_start_required: this.inherit,
      check_inactivity_required: this.enableIdleTime,
      terminate_datetime: terminateDateTime
    };

    if (this.destination.type === 'СOMPUTATIONAL') {
      this.model.confirmAction(this.notebook.project, this.notebook.name, parameters, this.destination.computational_name);
    } else {
      parameters['consider_inactivity'] = this.considerInactivity;
      this.model.confirmAction(this.notebook.project, this.notebook.name, parameters);
    }
  }

  private setScheduleByInactivity() {
    const data = {
      sync_start_required: this.parentInherit,
      check_inactivity_required: this.enableIdleTime,
      max_inactivity: this.schedulerForm.controls.inactivityTime.value
    };
    (this.destination.type === 'СOMPUTATIONAL')
      ? this.setInactivity(this.notebook.project, this.notebook.name, data, this.destination.computational_name)
      : this.setInactivity(this.notebook.project, this.notebook.name, { ...data, consider_inactivity: this.considerInactivity });
  }

  private formInit(start?: string, end?: string, terminate?: string) {
    this.schedulerForm = this.formBuilder.group({
      startDate: { disabled: this.inherit, value: start ? _moment(start).format() : null },
      finishDate: { disabled: this.inherit, value: end ? _moment(end).format() : null },
      terminateDate: { disabled: false, value: terminate ? _moment(terminate).format() : null },
      inactivityTime: [this.inactivityLimits.min,
      [Validators.compose([Validators.pattern(this.integerRegex), this.validInactivityRange.bind(this)])]]
    });
  }

  private getExploratorySchedule(project, resource, resource2?) {
    this.schedulerService.getExploratorySchedule(project, resource, resource2).subscribe(
      (params: ScheduleSchema) => {
        if (params) {
          params.start_days_repeat.filter(key => (this.selectedStartWeekDays[key.toLowerCase()] = true));
          params.stop_days_repeat.filter(key => (this.selectedStopWeekDays[key.toLowerCase()] = true));
          this.inherit = params.sync_start_required;
          this.tzOffset = params.timezone_offset && params.timezone_offset !== 'Z' ? params.timezone_offset : this.tzOffset;
          this.startTime = params.start_time ? SchedulerCalculations.convertTimeFormat(params.start_time) : this.startTime;
          this.startTimeMilliseconds = SchedulerCalculations.setTimeInMiliseconds(this.startTime);
          this.endTime = params.end_time ? SchedulerCalculations.convertTimeFormat(params.end_time) : this.endTime;
          this.endTimeMilliseconds = SchedulerCalculations.setTimeInMiliseconds(this.endTime);

          this.formInit(params.begin_date, params.finish_date, params.terminate_datetime);
          this.schedulerForm.controls.inactivityTime.setValue(params.max_inactivity || this.inactivityLimits.min);
          this.enableIdleTime = params.check_inactivity_required;
          this.considerInactivity = params.consider_inactivity || false;

          if (params.terminate_datetime) {
            const terminate_datetime = params.terminate_datetime.split(' ');
            this.schedulerForm.controls.terminateDate.setValue(terminate_datetime[0]);
            this.terminateTime = SchedulerCalculations.convertTimeFormat(terminate_datetime[1]);
            this.terminateTimeMilliseconds = SchedulerCalculations.setTimeInMiliseconds(this.terminateTime);
          }

          (this.enableIdleTime && params.max_inactivity)
            ? this.toggleIdleTimes({ checked: true })
            : this.toggleSchedule({ checked: true });
        }
      },
      error => this.resetDialog());
  }

  private checkParentInherit() {
    this.schedulerService.getExploratorySchedule(this.notebook.project, this.notebook.name)
      .subscribe((res: any) => this.parentInherit = res.sync_start_required);
  }

  private validInactivityRange(control) {
    if (control) {
      return this.enableIdleTime
        ? (control.value
          && control.value >= this.inactivityLimits.min
          && control.value <= this.inactivityLimits.max ? null : { valid: false })
        : control.value;
    }
  }

  private checkIsActiveSpark() {
    return this.notebook.resources.length > 0 && this.notebook.resources.some(el => el.image === 'docker.datalab-dataengine'
      && (el.status !== 'terminated' && el.status !== 'terminating' && el.status !== 'failed'));
  }

  private resetDialog() {
    this.infoMessage = false;
    this.timeReqiered = false;
    this.terminateDataReqiered = false;
    this.inherit = false;
    this.enableSchedule = false;
    this.considerInactivity = false;
    this.enableIdleTime = false;
    this.tzOffset = _moment().format('Z');
    this.startTime = SchedulerCalculations.convertTimeFormat('09:00');
    this.endTime = SchedulerCalculations.convertTimeFormat('20:00');
    this.terminateTime = null;

    this.schedulerForm.get('startDate').disable();
    this.schedulerForm.get('finishDate').disable();
    this.schedulerForm.get('terminateDate').disable();
  }
}
