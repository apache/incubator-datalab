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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util/';
import { ScheduleSchema } from '../../resources/scheduler/scheduler.model';

@Injectable()
export class SchedulerService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {}

  public getExploratorySchedule(project, notebook, resource?): Observable<{}> {
    const param = resource ? `/${project}/${notebook}/${resource}` : `/${project}/${notebook}`;
    return this.applicationServiceFacade
      .buildGetExploratorySchedule(param)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public setExploratorySchedule(project, notebook, data, resource?): Observable<ScheduleSchema> {
    const param = resource ? `/${project}/${notebook}/${resource}` : `/${project}/${notebook}`;
    return this.applicationServiceFacade
      .buildSetExploratorySchedule(param, data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public resetScheduleSettings(notebook, resource?): Observable<{}> {
    const url = resource ? `/${notebook}/${resource}` : `/${notebook}`;
    return this.applicationServiceFacade
      .buildResetScheduleSettings(JSON.stringify(url))
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getActiveSchcedulersData(offset: number): Observable<{}> {
    const param = `/active?minuteOffset=${offset}`;
    return this.applicationServiceFacade
      .BuildGetActiveSchcedulersData(param)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
