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

import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';

import { SortUtil } from './../../core/util';
import { SchedulerService } from './../../core/services';

export interface SchedulerParameters {
    begin_date : string;
    finish_date : string;
    start_time: string;
    end_time: string;
    days_repeat: Array<string>;
    timezone_offset: string;
    sync_start_required: boolean;
}

export class SchedulerModel {
    public confirmAction: Function;

    private createParameters: SchedulerParameters;

    private continueWith: Function;
    private schedulerService: SchedulerService;

    static getDefault(schedulerService): SchedulerModel {
        return new SchedulerModel(() => { }, () => { }, null, schedulerService);
    }

    constructor(
        fnProcessResults: any,
        fnProcessErrors: any,
        continueWith: Function,
        schedulerService: SchedulerService
    ) {
        this.continueWith = continueWith;
        this.schedulerService = schedulerService;
        this.prepareModel(fnProcessResults, fnProcessErrors);

        if (this.continueWith) this.continueWith();
    }

    private scheduleInstance(notebook, params, resourse): Observable<Response> {
        return this.schedulerService.setExploratorySchedule(notebook, params, resourse);
    }

    private prepareModel(fnProcessResults: any, fnProcessErrors: any): void {
        this.confirmAction = (notebook, data, resourse?) => this.scheduleInstance(notebook, data, resourse)
            .subscribe(
                (response: Response) => fnProcessResults(response),
                (response: Response) => fnProcessErrors(response));
    }
}

export class WeekdaysModel {
    constructor(
      public sunday: boolean,
      public monday: boolean,
      public tuesday: boolean,
      public wednesday: boolean,
      public thursday: boolean,
      public friday: boolean,
      public saturday: boolean
    ) { }
  
    setDegault(): void {
      this.sunday = false;
      this.monday = false;
      this.tuesday = false;
      this.wednesday = false;
      this.thursday = false;
      this.friday = false;
      this.saturday = false;
    }
  }