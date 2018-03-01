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

import { Component, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormControl, FormArray, FormBuilder } from '@angular/forms';

import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MomentDateAdapter } from '@angular/material-moment-adapter';

import * as _moment from 'moment';
import { HTTP_STATUS_CODES } from './../../core/util';

import { SchedulerService } from './../../core/services';
import { SchedulerModel } from './scheduler.model';

@Component({
  selector: 'dlab-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.css']
})
export class SchedulerComponent implements OnInit {

  public model: SchedulerModel;
  public notebook: any;
  
  // private startTime = {
  //   hour: new Date().getHours(),
  //   minute: new Date().getMinutes(),
  //   meridiem: new Date().getHours() < 12 ? 'AM' : 'PM'
  // };
    
  
  public date_format: string = 'YYYY-MM-DD';
  public timeFormat: string = 'HH:mm';
  public weekdays: string[] = [ 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday' ];
  schedulerForm : FormGroup;
    
  public startTime: any;
  public endTime: any;
  @ViewChild('bindDialog') bindDialog;
  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(private formBuilder: FormBuilder, private schedulerService: SchedulerService) { }

  ngOnInit() {
    

    console.log(_moment().format());
  }

  public open(param, notebook): void {
    this.notebook = notebook;
    
    if (!this.bindDialog.isOpened)
    this.model = new SchedulerModel((response: Response) => {
      debugger;
      if (response.status === HTTP_STATUS_CODES.OK) {
        this.close();
        this.buildGrid.emit();
      }
    },
    error => {
      debugger;
    },
    () => {
      this.formInit()
      this.bindDialog.open(param);
      this.getExploratorySchedule();
    },
    this.schedulerService);
  }

  onChange(event) {
    const weekdays = <FormArray>this.schedulerForm.get('weekdays') as FormArray;

    if (event.checked) {
      weekdays.push(new FormControl(event.source.name))
    } else {
      const i = weekdays.controls.findIndex(x => x.value === event.source.name);
      weekdays.removeAt(i);
    }
    console.log(this.schedulerForm.value.weekdays);
  }
  public scheduleInstance_btnClick(data) {
    const parameters = {
      begin_date: _moment(data.startDate).format(this.date_format),
      finish_date: _moment(data.endDate).format(this.date_format),
      start_time: _moment(this.startTime).format(this.timeFormat),
      end_time: _moment(this.endTime).format(this.timeFormat),
      days_repeat: data.weekdays.map((day => day.toUpperCase())),
      timezone_offset: _moment().format('Z')
    };

    debugger;
    // this.model.setCreatingParams(data, parameters);
    this.model.confirmAction(this.notebook, parameters);
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  private formInit() {
    this.schedulerForm = this.formBuilder.group({
      weekdays: this.formBuilder.array([]),
      startDate: new Date(),
      finishDate: new Date()
    });
  }

  private getExploratorySchedule() {
    this.schedulerService.getExploratorySchedule(this.notebook.name)
      .subscribe(params => {
        console.log(params);
      }, error => {
        debugger;
      });
  }
}
