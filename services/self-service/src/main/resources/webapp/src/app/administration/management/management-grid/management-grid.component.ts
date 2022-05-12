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

import {
  Component,
  OnInit,
  ViewChild,
  Input,
  Output,
  EventEmitter,
  Inject,
  HostListener,
  AfterViewInit,
  AfterViewChecked,
  ApplicationRef
} from '@angular/core';
import { ChangeDetectorRef } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { BehaviorSubject, Subject, timer } from 'rxjs';

import { SortUtils } from '../../../core/util';
import { EnvironmentsDataService } from '../management-data.service';
import { EnvironmentModel, ManagementConfigModel } from '../management.model';
import { ProgressBarService } from '../../../core/services/progress-bar.service';
import { DetailDialogComponent } from '../../../resources/exploratory/detail-dialog';
import { CompareUtils } from '../../../core/util/compareUtils';

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
export class ManagementGridComponent implements OnInit, AfterViewInit, AfterViewChecked {
  allEnvironmentData: Array<any>;
  allFilteredEnvironmentData: Array<any>;
  loading: boolean = false;
  filterConfiguration: ManagementConfigModel = new ManagementConfigModel([], '', [], [], [], [], []);
  filterForm: ManagementConfigModel = new ManagementConfigModel([], '', [], [], [], [], []);
  filtering: boolean = false;
  collapsedFilterRow: boolean = false;
  isMaxRight: Subject<boolean> = new BehaviorSubject(false);
  tableWrapperWidth: number;
  tableEl = {};
  userAgentIndex: number;

  @Input() environmentsHealthStatuses: Array<any>;
  @Input() resources: Array<any>;
  @Input() isAdmin: boolean;
  @Input() currentUser: string = '';
  @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();
  @Output() actionToggle: EventEmitter<ManageAction> = new EventEmitter();
  @Output() emitSelectedList: EventEmitter<ManageAction> = new EventEmitter();

  @ViewChild('tableWrapper') tableWrapper;
  @ViewChild('wrapper') wrapper;
  @ViewChild('pageWrapper') pageWrapper;
  @ViewChild('table') table;

  @HostListener('window:resize', ['$event'])
  onResize(event) {
    this.checkMaxRight();
  }

  @HostListener('scroll', ['$event'])
  scrollTable($event: Event) {
    this.checkMaxRight();
  }

  displayedColumns: string[] = ['checkbox', 'user', 'type', 'project', 'endpoint', 'shape', 'status', 'resources', 'actions'];
  displayedFilterColumns: string[] = ['checkbox-filter', 'user-filter', 'type-filter', 'project-filter', 'endpoint-filter', 'shape-filter', 'status-filter', 'resource-filter', 'actions-filter'];
  public selected;
  public allActiveNotebooks: any;
  private cashedFilterForm: ManagementConfigModel = new ManagementConfigModel([], '', [], [], [], [], []);
  public isFilterSelected: boolean;
  public isFilterChanged: boolean;

  constructor(
    private environmentsDataService: EnvironmentsDataService,
    public toastr: ToastrService,
    public dialog: MatDialog,
    private progressBarService: ProgressBarService,
    private cdRef: ChangeDetectorRef,
    private applicationRef: ApplicationRef,
  ) { }

  ngOnInit() {
    this.userAgentIndex = window.navigator.userAgent.indexOf('Firefox');
    this.getEnvironmentData();
  }

  ngAfterViewInit() {
    this.progressBarService.startProgressBar();
    this.tableEl = this.table._elementRef.nativeElement;
    this.checkMaxRight();
  }

  ngAfterViewChecked() {
    if (this.tableWrapperWidth !== this.wrapper.nativeElement.offsetWidth) {
      this.tableWrapperWidth = this.wrapper.nativeElement.offsetWidth;
      timer(100).subscribe(_ => {
        this.applicationRef.tick();
      });
    }
    this.cdRef.detectChanges();
  }

  getEnvironmentData() {
    this.progressBarService.startProgressBar();
    this.environmentsDataService._data
      .subscribe(
        data => {
          if (data) {
            this.allEnvironmentData = EnvironmentModel.loadEnvironments(data);
            this.getDefaultFilterConfiguration(data);
            this.applyFilter(this.cashedFilterForm || this.filterForm);
          }
          this.progressBarService.stopProgressBar();
        },
        () => {
          this.progressBarService.stopProgressBar();
        }
    );
  }

  buildGrid(): void {
    this.refreshGrid.emit();
    this.filtering = false;
  }

  public onUpdate($event): void {
    this.filterForm[$event.type] = $event.model;
    this.checkFilters();
  }

  private checkFilters() {
    this.isFilterChanged = CompareUtils.compareFilters(this.filterForm, this.cashedFilterForm);
    this.isFilterSelected = Object.keys(this.filterForm).some(v => this.filterForm[v].length > 0);
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
    if (config) {
      this.filterForm = JSON.parse(JSON.stringify(config));
      Object.setPrototypeOf(this.filterForm, Object.getPrototypeOf(config));
      this.cashedFilterForm = JSON.parse(JSON.stringify(config));
      Object.setPrototypeOf(this.cashedFilterForm, Object.getPrototypeOf(config));
    }

    let filteredData = this.getEnvironmentDataCopy();

    const containsStatus = (list, selectedItems) => {
      if (list) {
        return list.filter((item: any) => {
          if (selectedItems.indexOf(item.status) !== -1) return item;
        });
      }
    };

    if (filteredData.length) this.filtering = true;
    if (config) {
      filteredData = filteredData.filter(item => {
        const isUser = config.users.length > 0 ? (config.users.indexOf(item.user) !== -1) : true;
        const isTypeName = item.name
          ? item.name.toLowerCase().indexOf(config.type.toLowerCase()) !== -1
          : item.type.toLowerCase().indexOf(config.type.toLowerCase()) !== -1;
        const isStatus = config.statuses.length > 0
          ? (config.statuses.indexOf(item.status) !== -1)
          : (config.type !== 'active');
        const isShape = config.shapes.length > 0
          ? (config.shapes.indexOf(item.shape) !== -1 ||
            config.shapes.indexOf(item.gpu_type) !== -1 ||
            config.shapes.indexOf(`GPU count: ${item.gpu_count}`) !== -1)
          : true;
        const isProject = config.projects.length > 0
          ? (config.projects.indexOf(item.project) !== -1)
          : true;
        const isEndpoint = config.endpoints.length > 0
          ? (config.endpoints.indexOf(item.endpoint) !== -1)
          : true;

        const modifiedResources = containsStatus(item.resources, config.resources);
        let isResources = config.resources.length > 0
          ? (modifiedResources && modifiedResources.length > 0)
          : true;
        if (config.resources.length > 0 && modifiedResources && modifiedResources.length > 0) {
          item.resources = modifiedResources;
        }

        if (config.resources && config.resources.length === 0 && config.type === 'active' ||
          modifiedResources && modifiedResources.length >= 0 && config.resources.length > 0 && config.type === 'active') {
          item.resources = modifiedResources;
          isResources = true;
        }

        return isUser && isTypeName && isStatus && isShape && isProject && isResources && isEndpoint;
      });
    }
    this.allFilteredEnvironmentData = filteredData;
    this.allActiveNotebooks = this.allFilteredEnvironmentData
      .filter(v => v.name &&
        (v.status === 'running' || v.status === 'stopped') &&
        !this.clustersInProgress(v.resources || []));
    this.checkFilters();
  }

  getEnvironmentDataCopy() {
    return this.allEnvironmentData;
  }

  toggleResourceAction(environment: any, action: string, resource?): void {
    this.actionToggle.emit({ environment, action, resource });
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
    return resources.some(resource => (
      resource.status !== 'failed'
      && resource.status !== 'terminated'
      && resource.status !== 'running'
      && resource.status !== 'stopped'));
  }

  private getDefaultFilterConfiguration(data): void {
    const users = [], projects = [], shapes = [], statuses = [],
      resources = [], endpoints = [], gpuTypes = [], gpuCounts = [];

    data && data.forEach((item: any) => {
      if (item.user && users.indexOf(item.user) === -1) users.push(item.user);
      if (item.endpoint && endpoints.indexOf(item.endpoint) === -1) endpoints.push(item.endpoint);
      if (item.status && statuses.indexOf(item.status.toLowerCase()) === -1) statuses.push(item.status.toLowerCase());
      if (item.project && projects.indexOf(item.project) === -1) projects.push(item.project);
      if (item.shape && shapes.indexOf(item.shape) === -1) shapes.push(item.shape);
      if (item.gpu_type && gpuTypes.indexOf(item.gpu_type) === -1) gpuTypes.push(item.gpu_type);
      if (item.gpu_count && gpuCounts.indexOf(`GPU count: ${item.gpu_count}`) === -1) gpuCounts.push(`GPU count: ${item.gpu_count}`);
      if (item.computational_resources) {
        item.computational_resources.map((resource: any) => {
          if (resources.indexOf(resource.status) === -1) resources.push(resource.status);
          resources.sort(SortUtils.statusSort);
        });
      }
    });

    this.filterConfiguration = new ManagementConfigModel(
      users,
      '',
      projects,
      [...shapes, ...gpuTypes, ...gpuCounts],
      statuses, resources, endpoints
    );
  }

  public openNotebookDetails(data) {
    if (!data.exploratory_urls || !data.exploratory_urls.length) {
      return;
    }
    this.dialog.open(DetailDialogComponent, {
      data:
        { notebook: data, buckets: [], type: 'environment' },
      panelClass: 'modal-lg'
    })
      .afterClosed().subscribe(() => {});
  }

  public toggleActionForAll(element) {
    element.isSelected = !element.isSelected;
    this.selected = this.allFilteredEnvironmentData.filter(item => !!item.isSelected);
    this.emitSelectedList.emit(this.selected);
  }

  public toggleSelectionAll() {
    if (this.selected && this.selected.length === this.allActiveNotebooks.length) {
      this.allActiveNotebooks.forEach(notebook => notebook.isSelected = false);
    } else {
      this.allActiveNotebooks.forEach(notebook => notebook.isSelected = true);
    }
    this.selected = this.allFilteredEnvironmentData.filter(item => !!item.isSelected);
    this.emitSelectedList.emit(this.selected);
  }

  public clustersInProgress(resources: any) {
    const statuses = ['terminating', 'stopping', 'starting', 'creating', 'configuring', 'reconfiguring'];
    return resources.filter(resource => statuses.includes(resource.status)).length;
  }

  public onFilterNameUpdate(targetElement: any) {
    this.filterForm.type = targetElement;
    this.checkFilters();
  }

  public sctollTo(direction: string) {
    if (direction === 'left') {
      this.wrapper.nativeElement.scrollLeft = 0;
    } else {
      this.wrapper.nativeElement.scrollLeft = this.wrapper.nativeElement.offsetWidth;
    }
  }

  public checkMaxRight() {
    let arg;
    if (this.wrapper && this.table) {
      arg = this.wrapper.nativeElement.offsetWidth +
        this.wrapper.nativeElement.scrollLeft + 2 <= this.table._elementRef.nativeElement.offsetWidth;
    }

    return this.isMaxRight.next(arg);
  }
}

@Component({
  selector: 'confirm-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title">
      <span class="capitalize">{{ data.action }}</span>
      <span *ngIf="data.action !== 'create image'"> resource</span>
    </h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div mat-dialog-content class="content">
    <div *ngIf="data.type === 'cluster'">
      <p>Resource <span class="strong"> {{ data.resource_name }}</span> of user <span class="strong"> {{ data.user }} </span> will be
      <span *ngIf="data.action === 'terminate'"> decommissioned.</span>
      <span *ngIf="data.action === 'stop'">stopped.</span>
    </p>
    </div>
    <div class="resource-list" *ngIf="data.type === 'notebook'">
      <div class="resource-list-header">
        <div class="resource-name">Notebook</div>
        <div class="clusters-list">
          <div class="clusters-list-item">
            <div class="cluster"><span *ngIf="isClusterLength">Compute</span></div>
            <div class="status">Further status</div>
          </div>
        </div>

      </div>
      <div class="scrolling-content resource-heigth">
        <div class="resource-list-row sans node" *ngFor="let notebook of notebooks">
          <div class="resource-name ellipsis">
            {{notebook.name}}
          </div>

          <div class="clusters-list">
            <div class="clusters-list-item">
              <div class="cluster"></div>
              <div class="status"
                   [ngClass]="{
                   'stopped': data.action==='stop', 'terminated': data.action === 'terminate'
                    }"
              >
                {{data.action | convertactiontype}}
              </div>
            </div>
            <div class="clusters-list-item" *ngFor="let cluster of notebook?.resources">
              <div class="cluster">{{cluster.computational_name}}</div>
              <div class="status" [ngClass]="{
              'stopped': (data.action==='stop' && cluster.image==='docker.datalab-dataengine'), 'terminated': data.action === 'terminate' || (data.action==='stop' && cluster.image!=='docker.datalab-dataengine')
              }">{{data.action  === 'stop' && cluster.image === "docker.datalab-dataengine" ? 'Stopped' : 'Terminated'}}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="text-center ">
    <p class="strong">Do you want to proceed?</p>
  </div>
  <div class="text-center m-top-20">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: [
    `
      .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400; margin: 0; }
      .info { color: #35afd5; }
      .info .confirm-dialog { color: #607D8B; }
      header { display: flex; justify-content: space-between; color: #607D8B; }
      header h4 i { vertical-align: bottom; }
      header a i { font-size: 20px; }
      header a:hover i { color: #35afd5; cursor: pointer; }
      .plur { font-style: normal; }
      .scrolling-content{overflow-y: auto; max-height: 200px; }
      .cluster { width: 50%; text-align: left;}
      .status { width: 50%;text-align: left;}
      .label { font-size: 15px; font-weight: 500; font-family: "Open Sans",sans-serif;}
      .node { font-weight: 300;}
      .resource-name { width: 40%;text-align: left; padding: 10px 0;line-height: 26px;}
      .clusters-list { width: 60%;text-align: left; padding: 10px 0;line-height: 26px;}
      .clusters-list-item { width: 100%;text-align: left;display: flex}
      .resource-list{max-width: 100%; margin: 0 auto;margin-top: 20px; }
      .resource-list-header{display: flex; font-weight: 600; font-size: 16px;height: 48px; border-top: 1px solid #edf1f5; border-bottom: 1px solid #edf1f5; padding: 0 20px;}
      .resource-list-row{display: flex; border-bottom: 1px solid #edf1f5;padding: 0 20px;}
      .confirm-resource-terminating{text-align: left; padding: 10px 20px;}
      .confirm-message{color: #ef5c4b;font-size: 13px;min-height: 18px; text-align: center; padding-top: 20px}
      .checkbox{margin-right: 5px;vertical-align: middle; margin-bottom: 3px;}
      label{cursor: pointer}
      .bottom-message{padding-top: 15px;}
      .table-header{padding-bottom: 10px;}`
  ]
})

export class ReconfirmationDialogComponent {
  public notebooks;
  public isClusterLength;
  constructor(
    public dialogRef: MatDialogRef<ReconfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    if (data.notebooks && data.notebooks.length) {
      this.notebooks = JSON.parse(JSON.stringify(data.notebooks));
      this.notebooks = this.notebooks.map(notebook => {
        notebook.resources = notebook.resources.filter(res => res.status !== 'failed' &&
          res.status !== 'terminated' && res.status.slice(0, 4) !== data.action);
        if (notebook.resources.length) {
          this.isClusterLength = true;
        }
        return notebook;
      });
    }
  }
}
