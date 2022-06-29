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
import { BehaviorSubject, of } from 'rxjs';
import { mergeMap} from 'rxjs/operators';

import { ProjectService, EndpointService } from '../../core/services';
import { Project } from './project.model';

@Injectable()
export class ProjectDataService {
  public _projects = new BehaviorSubject<any>(null);
  private endpointsList: any = [];
  constructor(
    private projectService: ProjectService,
    private endpointService: EndpointService
  ) {
    this.getProjectsList();
  }

  public updateProjects() {
    this.getProjectsList();
  }

  private getProjectsList() {
    this.endpointService.getEndpointsData().subscribe(list => this.endpointsList = list);
    this.projectService.getProjectsList()
      .pipe(
        mergeMap ((response: Project[]) => {
            if (response && this.endpointsList.length) {
              response.forEach(project => project.endpoints.forEach(endpoint => {
                const idx = this.endpointsList.findIndex(v => v.name === endpoint.name);
                if (idx >= 0) {
                  endpoint.endpointStatus = this.endpointsList[idx].status;
                }
              }));
            }
          return of(response);
        }))
      .subscribe(
        (response: Project[]) => {
          return this._projects.next(response);
        });
  }
}
