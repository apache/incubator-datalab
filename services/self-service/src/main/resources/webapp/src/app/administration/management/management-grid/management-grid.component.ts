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

import { Component, OnInit, ViewChild, Input, Output, EventEmitter, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { HealthStatusService } from '../../../core/services';
import { SortUtils } from '../../../core/util';
import { ConfirmationDialogType } from '../../../shared';
import { ConfirmationDialogComponent } from '../../../shared/modal-dialog/confirmation-dialog';
import { EnvironmentsDataService } from '../management-data.service';
import { EnvironmentModel, ManagementConfigModel } from '../management.model';
import {ProgressBarService} from '../../../core/services/progress-bar.service';
import {DetailDialogComponent} from '../../../resources/exploratory/detail-dialog';

export interface ManageAction {
  action: string;
  environment: string;
  resource: string;
}

@Component({
  selector: 'management-grid',
  templateUrl: 'management-grid.component.html',
  styleUrls: [
    './management-grid.component.scss',
    '../../../resources/resources-grid/resources-grid.component.scss',
    '../../../resources/computational/computational-resources-list/computational-resources-list.component.scss'
  ]
})
export class ManagementGridComponent implements OnInit {
  allEnvironmentData: Array<any>;
  allFilteredEnvironmentData: Array<any>;
  loading: boolean = false;
  filterConfiguration: ManagementConfigModel = new ManagementConfigModel([], '', [], [], [], []);
  filterForm: ManagementConfigModel = new ManagementConfigModel([], '', [], [], [], []);
  filtering: boolean = false;
  collapsedFilterRow: boolean = false;

  @Input() environmentsHealthStatuses: Array<any>;
  @Input() resources: Array<any>;
  @Input() isAdmin: boolean;
  @Input() currentUser: string = '';
  @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();
  @Output() actionToggle: EventEmitter<ManageAction> = new EventEmitter();
  @Output() emitSelectedList: EventEmitter<ManageAction> = new EventEmitter();

  displayedColumns: string[] = [ 'checkbox', 'user', 'type', 'project', 'shape', 'status', 'resources', 'actions'];
  displayedFilterColumns: string[] = ['checkbox-filter', 'user-filter', 'type-filter', 'project-filter', 'shape-filter', 'status-filter', 'resource-filter', 'actions-filter'];
  private selected;

  constructor(
    private healthStatusService: HealthStatusService,
    private environmentsDataService: EnvironmentsDataService,
    public toastr: ToastrService,
    public dialog: MatDialog,
    private progressBarService: ProgressBarService,
  ) { }

  ngOnInit() {
  this.getEnvironmentData();
  }

  getEnvironmentData() {
    setTimeout(() => {this.progressBarService.startProgressBar(); } , 0);
    this.environmentsDataService._data.subscribe(data => {
      if (data) {
        this.allEnvironmentData = EnvironmentModel.loadEnvironments(data);
        this.getDefaultFilterConfiguration(data);
        this.applyFilter(this.filterForm);
      }
      this.progressBarService.stopProgressBar();
    }, () => {
      this.progressBarService.stopProgressBar();
    });
  }

  buildGrid(): void {
    this.refreshGrid.emit();
    this.filtering = false;
  }

  public onUpdate($event): void {
    this.filterForm[$event.type] = $event.model;
  }

  public toggleFilterRow(): void {
    this.collapsedFilterRow = !this.collapsedFilterRow;
  }

  public resetFilterConfigurations(): void {
    this.filterForm.defaultConfigurations();
    this.applyFilter(this.filterForm);
    this.buildGrid();
  }

  public applyFilter(config) {
    let filteredData = this.getEnvironmentDataCopy();

    const containsStatus = (list, selectedItems) => {
      if (list) {
        return list.filter((item: any) => { if (selectedItems.indexOf(item.status) !== -1) return item; });
      }
    };

    if (filteredData.length) this.filtering = true;
    if (config) {
      filteredData = filteredData.filter(item => {

        const isUser = config.users.length > 0 ? (config.users.indexOf(item.user) !== -1) : true;
        const isTypeName = item.name ?
          item.name.toLowerCase().indexOf(config.type.toLowerCase()) !== -1 : item.type.toLowerCase().indexOf(config.type.toLowerCase()) !== -1;
        const isStatus = config.statuses.length > 0 ? (config.statuses.indexOf(item.status) !== -1) : (config.type !== 'active');
        const isShape = config.shapes.length > 0 ? (config.shapes.indexOf(item.shape) !== -1) : true;
        const isProject = config.projects.length > 0 ? (config.projects.indexOf(item.project) !== -1) : true;

        const modifiedResources = containsStatus(item.resources, config.resources);
        let isResources = config.resources.length > 0 ? (modifiedResources.length > 0) : true;

        if (config.resources.length > 0 && modifiedResources.length > 0) { item.resources = modifiedResources; }

        if (config.resources && config.resources.length === 0 && config.type === 'active' ||
          modifiedResources && modifiedResources.length >= 0 && config.resources.length > 0 && config.type === 'active') {
          item.resources = modifiedResources;
          isResources = true;
        }

        return isUser && isTypeName && isStatus && isShape && isProject && isResources;
      });
    }
    this.allFilteredEnvironmentData = filteredData;
  }

  getEnvironmentDataCopy() {
    return this.allEnvironmentData;
  }

  toggleResourceAction(environment: any, action: string, resource?): void {
    this.actionToggle.emit({ action, environment, resource });
  }

  isResourcesInProgress(notebook) {
    if (notebook) {
      if (notebook.name === 'edge node') {
        return this.allEnvironmentData
          .filter(env => env.user === notebook.user)
          .some(el => this.inProgress([el]) || this.inProgress(el.resources));
      } else if (notebook.resources && notebook.resources.length) {
        return this.inProgress(notebook.resources);
      }
    }
    return false;
  }

  public inProgress(resources) {
    return resources.filter(resource => (
      resource.status !== 'failed'
      && resource.status !== 'terminated'
      && resource.status !== 'running'
      && resource.status !== 'stopped')).length > 0;
  }

  public isActiveResources(item) {
    if (item.resources.length) return item.resources.some(e => e.status !== 'terminated');
  }


  private getDefaultFilterConfiguration(data): void {
    const users = [], projects = [], shapes = [], statuses = [], resources = [];

    data && data.forEach((item: any) => {
      if (item.user && users.indexOf(item.user) === -1) users.push(item.user);
      if (item.status && statuses.indexOf(item.status.toLowerCase()) === -1) statuses.push(item.status.toLowerCase());
      if (item.project && projects.indexOf(item.project) === -1) projects.push(item.project);
      if (item.shape && shapes.indexOf(item.shape) === -1) shapes.push(item.shape);
      if (item.computational_resources) {
         item.computational_resources.map((resource: any) => {
              if (resources.indexOf(resource.status) === -1) resources.push(resource.status);
              resources.sort(SortUtils.statusSort);
            });
      }
    });

    this.filterConfiguration = new ManagementConfigModel(users, '', projects, shapes, statuses, resources);
  }

  openNotebookDetails(data) {
    if (!data.exploratory_urls || !data.exploratory_urls.length) {
      return;
    }
    this.dialog.open(DetailDialogComponent, { data:
        {notebook: data, bucketStatus: {view: true, upload: true, download: true, delete: true},  buckets: [], type: 'environment'},
      panelClass: 'modal-lg'
    })
      .afterClosed().subscribe(() => {});
  }

  toggleActionForAll(element) {
    element.isSelected = !element.isSelected;
    this.selected = this.allFilteredEnvironmentData.filter(item => !!item.isSelected);
    console.log(this.selected);
    this.emitSelectedList.emit(this.selected)
  }
}


@Component({
  selector: 'confirm-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title"><span class="capitalize">{{ data.action }}</span> resource</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div mat-dialog-content class="content">
      <p>Resource <span class="strong"> {{ data.resource_name }}</span> of user <span class="strong"> {{ data.user }} </span> will be
      <span *ngIf="data.action === 'terminate'"> decommissioned.</span>
      <span *ngIf="data.action === 'stop'">stopped.</span>
    </p>
    <p class="m-top-20"><span class="strong">Do you want to proceed?</span></p>
  </div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: [
  ]
})
export class ReconfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ReconfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
