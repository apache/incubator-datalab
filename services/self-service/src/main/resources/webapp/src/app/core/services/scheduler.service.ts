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

import { Injectable } from '@angular/core';
import { Response } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import { ApplicationServiceFacade } from './';

@Injectable()
export class SchedulerService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {}

  public getExploratorySchedule(notebook, resource?): Observable<Response> {
    const param = resource ? `/${notebook}/${resource}` : `/${notebook}`;
    return this.applicationServiceFacade
      .buildGetExploratorySchedule(param)
      .map((response: Response) => response.json())
      .catch((error: Response | any) => {
        return Observable.throw(
            new Error(`{"status": "${ error.status }", "statusText": "${ error.statusText }", "message": "${ error._body }"}`)
        );
    });
  }

  public setExploratorySchedule(notebook, data, resource?): Observable<Response> {
    const param = resource ? `/${notebook}/${resource}` : `/${notebook}`;
    return this.applicationServiceFacade
      .buildSetExploratorySchedule(param, data)
      .map((response: Response) => response)
      .catch((error: Response | any) => {
        return Observable.throw(
            new Error(`{"status": "${ error.status }", "statusText": "${ error.statusText }", "message": "${ error._body }"}`)
        );
    });
  }
}
