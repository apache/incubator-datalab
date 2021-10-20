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
import {ApplicationServiceFacade} from './applicationServiceFacade.service';
import {catchError, map} from 'rxjs/operators';
import {ErrorUtils} from '../util';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  constructor(private applicationServiceFacade: ApplicationServiceFacade) { }

  public getAuditData(filterData, page, itemsPrPage) {
    let queryString = `?page-number=${page}&page-size=${itemsPrPage}`;

    if (filterData.projects.length) {
      queryString += `&projects=${filterData.projects.join(',')}`;
    }

    if (filterData.resources.length) {
      queryString += `&resource-names=${filterData.resources.join(',')}`;
    }

    if (filterData.resource_types.length) {
      queryString += `&resource-types=${filterData.resource_types.join(',')}`;
    }

    if (filterData.users.length) {
      queryString += `&users=${filterData.users.join(',')}`;
    }

    if (filterData.date_start) {
      queryString += `&date-start=${filterData.date_start}`;
    }
    
    if (filterData.date_end) {
      queryString += `&date-end=${filterData.date_end}`;
    }

    return this.applicationServiceFacade
      .getAuditList(queryString)
      .pipe(
        map(response => {
          response[0].audit.forEach( item => {
            if(item?.info?.startsWith('Copy')) {
              item.action = 'COPY_LINK'              
            }
            if (item?.info?.startsWith('Open terminal')) {
              item.action = 'OPEN_TERMINAL'
            }
          })
          return response
        }),
        catchError(ErrorUtils.handleServiceError));
  }

  public sendDataToAudit(data) {
    return this.applicationServiceFacade
      .postActionToAudit(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }
}
