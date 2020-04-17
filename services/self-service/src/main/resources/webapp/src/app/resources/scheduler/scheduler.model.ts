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

import { SchedulerService } from '../../core/services';

export interface ScheduleSchema {
  begin_date: string | null;
  finish_date: string | null;
  start_time: string | null;
  end_time: string | null;
  start_days_repeat: Array<string>;
  stop_days_repeat: Array<string>;
  timezone_offset: string;
  sync_start_required: boolean;
  max_inactivity?: number;
  terminate_datetime?: string | null;
  check_inactivity_required?: boolean;
  consider_inactivity?: boolean;
}

export class SchedulerModel {
  public confirmAction: Function;
  private continueWith: Function;
  private schedulerService: SchedulerService;

  fnProcessResults: any;
  fnProcessErrors: any;

  static getDefault(schedulerService): SchedulerModel {
    return new SchedulerModel(() => {}, () => {}, null, schedulerService);
  }

  constructor(
    fnProcessResults: any,
    fnProcessErrors: any,
    continueWith: Function,
    schedulerService: SchedulerService
  ) {
    this.fnProcessResults = fnProcessResults;
    this.fnProcessErrors = fnProcessErrors;
    this.continueWith = continueWith;
    this.schedulerService = schedulerService;
    this.prepareModel(fnProcessResults, fnProcessErrors);

    if (this.continueWith) this.continueWith();
  }

  private scheduleInstance(project, notebook, params, resourse) {
    return this.schedulerService.setExploratorySchedule(project, notebook, params, resourse);
  }

  public setInactivityTime(params) {
    const [project, notebook, data, resource] = params;
    return this.scheduleInstance(project, notebook, data, resource);
  }

  public resetSchedule(notebook, resourse) {
    return this.schedulerService.resetScheduleSettings(notebook, resourse);
  }

  private prepareModel(fnProcessResults: any, fnProcessErrors: any): void {
    this.confirmAction = (project, notebook, data, resourse?) =>
      this.scheduleInstance(project, notebook, data, resourse).subscribe(
        response => fnProcessResults(response),
        error => fnProcessErrors(error)
      );
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
  ) {}

  public static setDefault(): WeekdaysModel {
    return new WeekdaysModel(false, false, false, false, false, false, false);
  }

  reset(): void {
    this.sunday = false;
    this.monday = false;
    this.tuesday = false;
    this.wednesday = false;
    this.thursday = false;
    this.friday = false;
    this.saturday = false;
  }
}
