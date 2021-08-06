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
import { Observable, BehaviorSubject } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { AppRoutingService } from './appRouting.service';
import { HTTP_STATUS_CODES, ErrorUtils } from '../util';

@Injectable()
export class HealthStatusService {
  private _statusData = new BehaviorSubject(<GeneralEnvironmentStatus>{});

  constructor(
    private applicationServiceFacade: ApplicationServiceFacade,
    private appRoutingService: AppRoutingService
  ) { }

  get statusData() {
    return this._statusData.asObservable();
  }

  public reloadInitialStatusData() {
    this.getEnvironmentHealthStatus()
      .subscribe(
        (res: GeneralEnvironmentStatus) => {
          this._statusData.next(res);
          console.log('reload Initial Status Data');
        },
        err => console.error('Error retrieving status')
      );
  }

  public isHealthStatusOk(): Observable<boolean> {
    return this.applicationServiceFacade
      .buildGetEnvironmentHealthStatus()
      .pipe(
        map(response => {
          if (response.status === HTTP_STATUS_CODES.OK)
            if (response.body.status === 'ok')
              return true;

          return false;
        }));
  }

  public getEnvironmentHealthStatus(): Observable<GeneralEnvironmentStatus> {
    return this.applicationServiceFacade
      .buildGetEnvironmentHealthStatus()
      .pipe(
        map(response => {
          this._statusData.next(response.body);
          return response.body;
        }),
        catchError(ErrorUtils.handleServiceError));
  }

  public getEnvironmentStatuses(): Observable<GeneralEnvironmentStatus> {
    const body = '?full=1';
    return this.applicationServiceFacade
      .buildGetEnvironmentStatuses(body)
      .pipe(
        map(response => {
          this._statusData.next(response);
          return response;
        }),
        catchError(ErrorUtils.handleServiceError));
  }

  public getQuotaStatus(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetQuotaStatus()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public runEdgeNode(): Observable<{}> {
    return this.applicationServiceFacade
      .buildRunEdgeNodeRequest()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public suspendEdgeNode(): Observable<{}> {
    return this.applicationServiceFacade
      .buildSuspendEdgeNodeRequest()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public recreateEdgeNode(): Observable<{}> {
    return this.applicationServiceFacade
      .buildRecreateEdgeNodeRequest()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public isPassageway(parameter: string): Observable<boolean> {
    return this.applicationServiceFacade
      .buildGetEnvironmentHealthStatus()
      .pipe(
        map(response => {
          if (response.status === HTTP_STATUS_CODES.OK) {
            const data = response.body;
            if (parameter === 'billing' && !data.billingEnabled) {
              this.appRoutingService.redirectToHomePage();
              return false;
            }
            if (parameter === 'audit' && !data.auditEnabled) {
              this.appRoutingService.redirectToHomePage();
              return false;
            }
            if (parameter === 'administration' && !data.admin && !data.projectAdmin) {
              this.appRoutingService.redirectToNoAccessPage();
              return false;
            }
            if (parameter === 'project-admin' && !data.admin && data.projectAdmin) {
              this.appRoutingService.redirectToNoAccessPage();
              return false;
            }
          }
          return true;
        }));
  }

  public getActiveUsers(): Observable<Array<string>> {
    return this.applicationServiceFacade
      .buildGetActiveUsers()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getSsnMonitorData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetSsnMonitorData()
      .pipe(
        map(response => response),
        catchError(error => error));
  }

  public getTotalBudgetData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetTotalBudgetData()
      .pipe(
        map(response => response),
        catchError(error => error));
  }

  public updateTotalBudgetData(data): Observable<{}> {
    const url = (data && data > 0) ? `/budget/${data}` : '/budget';
    const method = (data && data > 0) ? 2 : 3;

    return this.applicationServiceFacade
      .buildUpdateTotalBudgetData(url, method)
      .pipe(
        map(response => response),
        catchError(error => error));
  }

  public getAppMetaData(): Observable<{}> {
    return this.applicationServiceFacade
      .buildGetAppMetaData()
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public resetStatusValue() {
    this._statusData.next(<GeneralEnvironmentStatus>{});
  }
}
