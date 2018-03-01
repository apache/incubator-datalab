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
   
}

export class SchedulerModel {
    public confirmAction: Function;

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

    private scheduleInstance(params): Observable<Response> {
        return this.schedulerService.setExploratorySchedule('DL', {
            "begin_date" : "2014-01-01",
            "finish_date" : "2019-01-31",
            "start_time": "12:15",
            "end_time": "18:25",
            "days_repeat": ["MONDAY", "WEDNESDAY", "SUNDAY"],
            "timezone_offset": "+02:00"
        });
    }

    private prepareModel(fnProcessResults: any, fnProcessErrors: any): void {
        this.confirmAction = (data) => this.scheduleInstance(data)
            .subscribe(
                (response: Response) => fnProcessResults(response),
                (response: Response) => fnProcessErrors(response));
    }
}
