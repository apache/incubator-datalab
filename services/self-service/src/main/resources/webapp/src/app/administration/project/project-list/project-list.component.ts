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

import { Component, OnInit, Output, EventEmitter, OnDestroy, Inject, Input, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { MatTableDataSource } from '@angular/material/table';
import { Subscription } from 'rxjs';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';

import { ProjectDataService } from '../project-data.service';
import {ProgressBarService} from '../../../core/services/progress-bar.service';
import {EdgeActionDialogComponent} from '../../../shared/modal-dialog/edge-action-dialog';
import { EndpointService } from '../../../core/services';
import { Endpoint, ModifiedEndpoint, Project } from '../project.model';
import { checkEndpointListUtil } from '../../../core/util';
import { EndpointStatus } from '../project.config';

@Component({
  selector: 'project-list',
  templateUrl: './project-list.component.html',
  styleUrls: [
    './project-list.component.scss',
    '../../../resources/computational/computational-resources-list/computational-resources-list.component.scss'
  ]
})
export class ProjectListComponent implements OnInit, OnDestroy {
  @Input() isProjectAdmin: boolean;
  @Output() editItem: EventEmitter<{}> = new EventEmitter();
  @Output() toggleStatus: EventEmitter<{}> = new EventEmitter();

  @ViewChild('recreateBtn') recreateBtn: ElementRef;

  displayedColumns: string[] = ['name', 'groups', 'endpoints', 'actions'];
  dataSource: Project[] | any = [];
  projectList: Project[];
  isEndpointAvailable: boolean;

  private subscriptions: Subscription = new Subscription();

  constructor (
    public toastr: ToastrService,
    private projectDataService: ProjectDataService,
    private progressBarService: ProgressBarService,
    private endpointService: EndpointService,
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<ProjectListComponent>,
    public dialog: MatDialog,
  ) { }

  ngOnInit(): void {
    this.getProjectList();
    this.getEndpointList();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  public showActiveInstances(): void {
    const filteredList = this.projectList.map(project => {
      project.endpoints = project.endpoints.filter((endpoint: Endpoint) => {
        return endpoint.status !== 'TERMINATED' && endpoint.status !== 'TERMINATING' && endpoint.status !== 'FAILED';
      });
      return project;
    });

    this.dataSource = new MatTableDataSource(filteredList);
  }

  public editProject(item: Project[]) {
    this.editItem.emit(item);
  }

  public openEdgeDialog(action: string, project: Project) {
    const endpoints = this.getFilteredEndpointList(action, project);

    if (action === 'terminate' && endpoints.length === 1) {
      this.toggleStatus.emit({ project, endpoint: endpoints, action, oneEdge: true });
    } else {
      this.dialog.open(EdgeActionDialogComponent, { data: { type: action, item: endpoints }, panelClass: 'modal-sm' })
        .afterClosed().subscribe(
        endpoint => {
          if (endpoint && endpoint.length) {
            this.toggleStatus.emit({project, endpoint, action});
          }
        },
        error => this.toastr.error(error.message || `Endpoint ${action} failed!`, 'Oops!')
      );
    }
  }

  public areResoursesInStatuses(resources, statuses: Array<string>) {
    return resources.some(resource => statuses.some(status => resource.status === status));
  }

  isRecreateBtnDisabled(endpointList: ModifiedEndpoint[]): boolean {
    return checkEndpointListUtil(endpointList);
  }

  private getFilteredEndpointList(action: string, project) {
    return project.endpoints.filter(endpoint => {
      if (action === 'stop') {
        return endpoint.status === EndpointStatus.running;
      }
      if (action === 'start') {
        return endpoint.status === EndpointStatus.stopped;
      }
      if (action === 'terminate') {
        return endpoint.status === EndpointStatus.running || endpoint.status === EndpointStatus.stopped;
      }
      if (action === 'recreate') {
        const edgeNodeStatus = endpoint.status === EndpointStatus.terminated || endpoint.status === EndpointStatus.failed;
        return edgeNodeStatus && endpoint.endpointStatus === 'ACTIVE';
      }
    });
  }

  private getProjectList() {
    this.progressBarService.startProgressBar();
    this.subscriptions.add(this.projectDataService._projects
      .subscribe(
        (value: Project[]) => {
          this.modifyProjectList(value);
          if (value) this.dataSource = new MatTableDataSource(value);
          this.progressBarService.stopProgressBar();
        },
        () => this.progressBarService.stopProgressBar()
      )
    );
  }

  private modifyProjectList(value: Project[]): void {
    this.projectList = value;
    if (this.projectList) {
      this.projectList.forEach(project => {
        project.areRunningNode = this.areResoursesInStatuses(project.endpoints, [EndpointStatus.running]);
        project.areStoppedNode = this.areResoursesInStatuses(project.endpoints, [EndpointStatus.stopped]);
        project.areTerminatedNode = this.areResoursesInStatuses(project.endpoints, [EndpointStatus.terminated, EndpointStatus.failed]);
      });
    }
  }

  private getEndpointList() {
    this.endpointService.getEndpointsData().subscribe(
      (response: Endpoint[] | []) => {
        this.isEndpointAvailable = this.checkIsEndpointAvailable(response);
      }
    );
  }

  private checkIsEndpointAvailable(data: Endpoint[] | []): boolean {
    return  data.length ? true : false;
  }
}
