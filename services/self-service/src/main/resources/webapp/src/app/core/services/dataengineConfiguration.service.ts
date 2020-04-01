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
import { ErrorUtils } from '../util';

@Injectable()
export class DataengineConfigurationService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) {}

  public getClusterConfiguration(project, exploratory, cluster, provider): Observable<{}> {
    const url = `/${project}/${exploratory}/${cluster}/config`;
    return this.applicationServiceFacade
      .buildGetClusterConfiguration(url, provider)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public editClusterConfiguration(data, project, exploratory, cluster, provider): Observable<{}> {
    const url = `/dataengine/${project}/${exploratory}/${cluster}/config`;
    return this.applicationServiceFacade
      .buildEditClusterConfiguration(url, data, provider)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getExploratorySparkConfiguration(project, exploratory): Observable<{}> {
    const url = `/${project}/${exploratory}/cluster/config`;
    return this.applicationServiceFacade
      .buildGetExploratorySparkConfiguration(url)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public editExploratorySparkConfiguration(data, project, exploratory): Observable<{}> {
    const url = `/${project}/${exploratory}/reconfigure`;
    return this.applicationServiceFacade
      .buildEditExploratorySparkConfiguration(url, data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
