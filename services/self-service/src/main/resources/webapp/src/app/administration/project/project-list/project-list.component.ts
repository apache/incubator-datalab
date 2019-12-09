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

import { Component, OnInit, Output, EventEmitter, OnDestroy } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { MatTableDataSource } from '@angular/material/table';
import { Subscription } from 'rxjs';

import { ProjectDataService } from '../project-data.service';
import { Project, Endpoint } from '../project.component';
import { CheckUtils } from '../../../core/util';

@Component({
  selector: 'project-list',
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss', '../../../resources/computational/computational-resources-list/computational-resources-list.component.scss']
})
export class ProjectListComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ['name', 'groups', 'endpoints', 'actions'];
  dataSource: Project[] | any = [];
  projectList: Project[];

  @Output() editItem: EventEmitter<{}> = new EventEmitter();
  @Output() deleteItem: EventEmitter<{}> = new EventEmitter();
  @Output() toggleStatus: EventEmitter<{}> = new EventEmitter();

  private subscriptions: Subscription = new Subscription();

  constructor(
    public toastr: ToastrService,
    private projectDataService: ProjectDataService
  ) { }


  ngOnInit() {
    this.subscriptions.add(this.projectDataService._projects.subscribe((value: Project[]) => {
      this.projectList = value;
      if (value) this.dataSource = new MatTableDataSource(value)
    }));
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  public showActiveInstances(): void {
    console.log(this.projectList);
    const filteredList = this.projectList.map(project => {
      project.endpoints = project.endpoints.filter((endpoint: Endpoint) => endpoint.status !== 'TERMINATED' && endpoint.status !== 'TERMINATING' && endpoint.status !== 'FAILED')
      return project;
    })

    this.dataSource = new MatTableDataSource(filteredList);
  }

  public toggleEndpointAction(project, action, endpoint) {
    this.toggleStatus.emit({ project, endpoint, action });
  }

  public editProject(item: Project[]) {
    this.editItem.emit(item);
  }

  public deleteProject(item: Project[]) {
    this.deleteItem.emit(item);
  }

  public isInProgress(project) {
    if (project)
      return project.endpoints.some(e => e.status !== 'RUNNING' && e.status !== 'STOPPED' && e.status !== 'TERMINATED' && e.status !== 'FAILED')
  }

  public isActiveEndpoint(project) {
    if (project)
      return project.endpoints.some(e => e.status !== 'TERMINATED' && e.status !== 'FAILED');
  }

  public toEndpointStatus(status) {
    return CheckUtils.endpointStatus[status] || status;
  }
}