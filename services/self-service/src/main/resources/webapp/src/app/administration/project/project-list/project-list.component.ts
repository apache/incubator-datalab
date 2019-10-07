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
import { ProjectService } from '../../../core/services';
import { Project } from '../project.component';

@Component({
  selector: 'project-list',
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss', '../../../resources/computational/computational-resources-list/computational-resources-list.component.scss']
})
export class ProjectListComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ['name', 'groups', 'endpoints', 'actions'];
  dataSource: Project[] | any = [];
  @Output() editItem: EventEmitter<{}> = new EventEmitter();
  @Output() deleteItem: EventEmitter<{}> = new EventEmitter();
  @Output() toggleStatus: EventEmitter<{}> = new EventEmitter();

  private subscriptions: Subscription = new Subscription();

  constructor(
    public toastr: ToastrService,
    private projectDataService: ProjectDataService,
    private projectService: ProjectService
  ) { }


  ngOnInit() {
    this.subscriptions.add(this.projectDataService._projects.subscribe((value: Project[]) => {
      if (value) this.dataSource = new MatTableDataSource(value);
    }));
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
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

  private toEndpointStatus(status) {
    if (status === 'CREATING') {
      return 'CONNECTING';
    } else if (status === 'STARTING') {
      return 'CONNECTING';
    } else if (status === 'RUNNING') {
      return 'CONNECTED';
    } else if (status === 'STOPPING') {
      return 'DISCONNECTING';
    } else if (status === 'STOPPED') {
      return 'DISCONNECTED';
    } else {
      return status;
    }
  }
}
