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

import { Component, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormControl, FormArray, FormBuilder } from '@angular/forms';
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
  private startTime = {
    hour: new Date().getHours(),
    minute: new Date().getMinutes(),
    meridiem: new Date().getHours() < 12 ? 'AM' : 'PM'
  };

  private endTime = {
    hour: new Date().getHours(),
    minute: new Date().getMinutes(),
    meridiem: new Date().getHours() < 12 ? 'AM' : 'PM'
  };

  public weekdays: string[] = [ 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday' ];
  schedulerFormGroup : FormGroup;

  @ViewChild('bindDialog') bindDialog;

  constructor(private formBuilder: FormBuilder, private schedulerService: SchedulerService) { }

  ngOnInit() {
    this.schedulerFormGroup = this.formBuilder.group({
      weekdays: this.formBuilder.array([]),
      startDate: new Date(),
      finishDate: new Date(),
      startTime: this.startTime,
      endtTime: this.endTime,
    });
  }

  public open(param, notebook): void {
    this.notebook = notebook;
    
    
    
    if (!this.bindDialog.isOpened)
    this.model = new SchedulerModel((response: Response) => { },
    error => {
      debugger;
    },
    () => {
      this.bindDialog.open(param);
      this.getExploratorySchedule();
    },
    this.schedulerService);
  }

  onChange(event) {
    console.log(event);

    const weekdays = <FormArray>this.schedulerFormGroup.get('weekdays') as FormArray;

    if (event.checked) {
      weekdays.push(new FormControl(event.source.name))
    } else {
      const i = weekdays.controls.findIndex(x => x.value === event.source.name);
      weekdays.removeAt(i);
    }
    console.log(this.schedulerFormGroup.value.weekdays);
    
  }
  public scheduleInstance_btnClick(data) {
    debugger;
    this.model.confirmAction();
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
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
