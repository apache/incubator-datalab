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
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { ToastrService } from 'ngx-toastr';
import { MatDialog } from '@angular/material/dialog';
import { ProjectService, UserResourceService, OdahuDeploymentService } from '../../core/services';
import { ExploratoryModel } from './resources-grid.model';
import { FilterConfigurationModel } from './filter-configuration.model';
import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { ConfirmationDialogComponent, ConfirmationDialogType } from '../../shared';
import { SortUtils, CheckUtils } from '../../core/util';
import { DetailDialogComponent } from '../exploratory/detail-dialog';
import { AmiCreateDialogComponent } from '../exploratory/ami-create-dialog';
import { InstallLibrariesComponent } from '../exploratory/install-libraries';
import { ComputationalResourceCreateDialogComponent } from '../computational/computational-resource-create-dialog/computational-resource-create-dialog.component';
import { CostDetailsDialogComponent } from '../exploratory/cost-details-dialog';
import { SchedulerComponent } from '../scheduler';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import { ProgressBarService } from '../../core/services/progress-bar.service';
import { ComputationModel } from '../computational/computational-resource.model';
import { NotebookModel } from '../exploratory/notebook.model';
import { AuditService } from '../../core/services/audit.service';
import { CompareUtils } from '../../core/util/compareUtils';
import { OdahuActionDialogComponent } from '../../shared/modal-dialog/odahu-action-dialog';
import { Project } from '../../administration/project/project.model';

export interface SharedEndpoint {
  edge_node_ip: string;
  shared_bucket_name?: string | null;
  shared_container_name?: string | null;
  status: string;
  user_own_bucket_name?: string | null;
  user_container_name?: string | null;
  user_own_bicket_name?: string | null;
  shared_storage_account_name?: string | null;
  user_storage_account_name?: string | null;
}

export interface ProjectEndpoint {
  account: string;
  cloudProvider: string;
  endpoint_tag: string;
  name: string;
  status: string;
  url: string;
}

export interface BucketList {
  name: string;
  children: Bucket[];
  length?: number;
  cloud: string;
}

export interface Bucket {
  name: string;
  endpoint: string;
}

@Component({
  selector: 'resources-grid',
  templateUrl: 'resources-grid.component.html',
  styleUrls: ['./resources-grid.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({ height: '0px', minHeight: '0' })),
      state('expanded', style({ height: '*' })),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})

export class ResourcesGridComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  @Input() projects: Array<any>;
  @Output() getEnvironments: EventEmitter<any> = new EventEmitter();

  public environments;
  public collapseFilterRow: boolean = false;
  public filtering: boolean = false;
  public activeFiltering: boolean = false;
  public activeProject: any;
  public healthStatus: GeneralEnvironmentStatus;
  public filteredEnvironments = [];
  public filterConfiguration: FilterConfigurationModel = new FilterConfigurationModel('', [], [], [], '', '');
  public filterForm: FilterConfigurationModel = new FilterConfigurationModel('', [], [], [], '', '');

  public filteringColumns: Array<any> = [
    { title: 'Environment name', name: 'name', class: 'name-col', filter_class: 'name-filter', filtering: true },
    { title: 'Status', name: 'statuses', class: 'status-col', filter_class: 'status-filter', filtering: true },
    { title: 'Instance size', name: 'shapes', class: 'shape-col', filter_class: 'shape-filter', filtering: true },
    { title: 'Tags', name: 'tag', class: 'tag-col', filter_class: 'tag-filter', filtering: false },
    { title: 'Computational resource', name: 'resources', class: 'resources-col', filter_class: 'resource-filter', filtering: true },
    { title: 'Cost', name: 'cost', class: 'cost-col', filter_class: 'cost-filter', filtering: false },
    { title: '', name: 'actions', class: 'actions-col', filter_class: 'action-filter', filtering: false }
  ];

  public displayedColumns: string[] = this.filteringColumns.map(item => item.name);
  public displayedFilterColumns: string[] = this.filteringColumns.map(item => item.filter_class);
  public bucketsList: BucketList;
  public activeProjectsList: any;
  public isFilterChanged: boolean;
  public isFilterSelected: boolean;
  private cashedFilterForm: FilterConfigurationModel;

  constructor(
    public toastr: ToastrService,
    private userResourceService: UserResourceService,
    private dialog: MatDialog,
    private progressBarService: ProgressBarService,
    private projectService: ProjectService,
    private auditService: AuditService,
    private odahuDeploymentService: OdahuDeploymentService,
  ) { }

  ngOnInit(): void {
    this.buildGrid();
    this.getUserProjects();
  }

  public getUserProjects() {
    this.projectService.getUserProjectsList(true).subscribe((projects: Project[]) => {
      this.activeProjectsList = projects;
    });
  }

  public buildGrid(): void {
    this.progressBarService.startProgressBar();
    this.userResourceService.getUserProvisionedResources()
      .subscribe(
        (result: any) => {
          this.environments = ExploratoryModel.loadEnvironments(result);
          if (this.environments?.length === 1) {
            this.selectActiveProject(this.environments[0].project);
          }
          this.getEnvironments.emit(this.environments);
          this.getBuckets();
          this.getDefaultFilterConfiguration();
          (this.environments.length) ? this.getUserPreferences() : this.filteredEnvironments = [];
          this.healthStatus && !this.healthStatus.billingEnabled && this.modifyGrid();
          this.progressBarService.stopProgressBar();
        },
        () => this.progressBarService.stopProgressBar()
      );
  }

  public toggleFilterRow(): void {
    this.collapseFilterRow = !this.collapseFilterRow;
  }

  public onUpdate($event): void {
    this.filterForm[$event.type] = $event.model;
    this.checkFilters();
  }

  private checkFilters() {
    this.isFilterChanged = !CompareUtils.compareFilters(this.filterForm, this.cashedFilterForm);
    this.isFilterSelected = Object.keys(this.filterForm).some(v => this.filterForm[v].length > 0);
  }

  public selectActiveProject(project = ''): void {
    this.filterForm.project = project;
    this.applyFilter_btnClick(this.filterForm);
  }

  public showActiveInstances(): void {
    this.filterForm = this.loadUserPreferences(this.filterActiveInstances());
    this.applyFilter_btnClick(this.filterForm);
    this.buildGrid();
  }

  public containsNotebook(notebook_name: string, environmentNames: Array<string>): boolean {
    if (notebook_name && environmentNames.length ) {
      return environmentNames
        .some(item => CheckUtils.delimitersFiltering(notebook_name) === CheckUtils.delimitersFiltering(item));
    }
      return false;
  }

  public isResourcesInProgress(notebook): boolean {
    const env = this.getResourceByName(notebook.name, notebook.project);

    if (env && env.resources.length) {
      return env.resources.filter(item => (item.status !== 'failed' && item.status !== 'terminated'
        && item.status !== 'running' && item.status !== 'stopped')).length > 0;
    }
    return false;
  }

  public filterActiveInstances(): FilterConfigurationModel {
    return (<FilterConfigurationModel | any>Object).assign({}, this.filterConfiguration, {
      statuses: SortUtils.activeStatuses(),
      resources: SortUtils.activeStatuses(),
      type: 'active',
      project: this.activeProject || ''
    });
  }

  public resetFilterConfigurations(): void {
    this.filterForm.resetConfigurations();
    this.updateUserPreferences(this.filterForm);
    this.buildGrid();
  }

  public printDetailEnvironmentModal(data): void {
    this.dialog.open(DetailDialogComponent, { data:
      {
        notebook: data,
        bucketStatus: this.healthStatus.bucketBrowser,
        buckets: this.bucketsList,
        type: 'resource'
      },
      panelClass: 'modal-lg'
    })
      .afterClosed().subscribe(() => this.buildGrid());
  }

  public printDetailOdahuModal(data): void {
    this.dialog.open(DetailDialogComponent, { data: { odahu: data }, panelClass: 'modal-lg' })
      .afterClosed().subscribe(() => this.buildGrid());
  }

  public printCostDetails(data): void {
    this.dialog.open(CostDetailsDialogComponent, { data: data, panelClass: 'modal-xl' })
      .afterClosed().subscribe(() => this.buildGrid());
  }

  public exploratoryAction(data, action: string): void {
    const resource = this.getResourceByName(data.name, data.project);
    if (action === 'deploy') {
      this.dialog.open(ComputationalResourceCreateDialogComponent, { data:
        {
          notebook: resource,
          full_list: this.environments
        },
        panelClass: 'modal-xxl'
      })
        .afterClosed().subscribe((res) => {
        res && this.buildGrid();
      });
    } else if (action === 'run') {
      this.userResourceService
        .runExploratoryEnvironment({ notebook_instance_name: data.name, project_name: data.project })
        .subscribe(
          () => this.buildGrid(),
          error => this.toastr.error(error.message || 'Exploratory starting failed!', 'Oops!'));
    } else if (action === 'stop') {
      const compute =  data.resources.filter(cluster => cluster.status === 'running');
      this.dialog.open(ConfirmationDialogComponent,
        { data: { notebook: data, compute, type: ConfirmationDialogType.StopExploratory }, panelClass: 'modal-sm' })
        .afterClosed().subscribe((res) => {
        res && this.buildGrid();
      });
    } else if (action === 'terminate') {
      const compute =  data.resources.filter(cluster => cluster.status === 'running' || cluster.status === 'stopped');
      this.dialog.open(ConfirmationDialogComponent, { data:
        {
          notebook: data,
          compute,
          type: ConfirmationDialogType.TerminateExploratory
        },
        panelClass: 'modal-sm'
      })
        .afterClosed().subscribe((res) => res && this.buildGrid());
    } else if (action === 'install') {
      this.dialog.open(InstallLibrariesComponent, { data: data, panelClass: 'modal-fullscreen' })
        .afterClosed().subscribe((res) => res && this.buildGrid());
    } else if (action === 'schedule') {
      this.dialog.open(SchedulerComponent, { data: { notebook: data, type: 'EXPLORATORY' }, panelClass: 'modal-xl-s' })
        .afterClosed().subscribe((res) => res && this.buildGrid());
    } else if (action === 'ami') {
      this.dialog.open(AmiCreateDialogComponent, { data: data, panelClass: 'modal-sm' })
        .afterClosed().subscribe((res) => res && this.buildGrid());
    }
  }

  public getBuckets(): void {
    const bucketsList = [];
    this.environments.forEach(project => {
      if (project.endpoints && project.endpoints.length !== 0) {
        project.endpoints.forEach((endpoint: ProjectEndpoint) => {
          if (endpoint.status === 'ACTIVE') {
            const currEndpoint: SharedEndpoint = project.projectEndpoints[endpoint.name];
            const edgeItem: BucketList = {
              name: `${project.project} (${endpoint.name})`,
              children: [],
              cloud: endpoint.cloudProvider
            };
            let projectBucket: string = currEndpoint.user_own_bicket_name
              || currEndpoint.user_own_bucket_name;
            if (currEndpoint.user_container_name) {
              projectBucket = currEndpoint.user_storage_account_name + '.' + currEndpoint.user_container_name;
            }
            let sharedBucket: string = currEndpoint.shared_bucket_name;
            if (currEndpoint.shared_container_name) {
              sharedBucket = currEndpoint.shared_storage_account_name + '.' + currEndpoint.shared_container_name;
            }
            if (projectBucket && currEndpoint.status !== 'terminated'
              && currEndpoint.status !== 'terminating' && currEndpoint.status !== 'failed') {
              edgeItem.children.push({name: projectBucket, endpoint: endpoint.name});
            }
            if (sharedBucket) {
              edgeItem.children.push({name: sharedBucket, endpoint: endpoint.name});
            }
            bucketsList.push(edgeItem);
          }
        });
      }
    });

    this.bucketsList = SortUtils.flatDeep(bucketsList, 1).filter(v => v.children.length);
  }

  // PRIVATE
  private getResourceByName(notebook_name: string, project_name: string) {
    return this.getEnvironmentsListCopy()
      .filter(environments => environments.project === project_name)
      .map(env => env.exploratory.find(({ name }) => name === notebook_name))
      .filter(name => !!name)[0];
  }

  private getEnvironmentsListCopy() {
    return this.environments.map(env => JSON.parse(JSON.stringify(env)));
  }

  private getDefaultFilterConfiguration(): void {
    const data = this.environments;
    const shapes = [], statuses = [], resources = [],
          gpuTypes = [], gpuCounts = [];

    data.filter(elem => elem.exploratory.map((item: any) => {
      if (shapes.indexOf(item.shape) === -1) shapes.push(item.shape);
      if  (item.gpu_type && gpuTypes.indexOf(item.gpu_type) === -1) gpuTypes.push(item.gpu_type);
      if  (item.gpu_count && gpuCounts.indexOf(`GPU count: ${item.gpu_count}`) === -1) gpuCounts.push(`GPU count: ${item.gpu_count}`);
      if (statuses.indexOf(item.status) === -1) statuses.push(item.status);
      statuses.sort(SortUtils.statusSort);

      item.resources.map((resource: any) => {
        if (resources.indexOf(resource.status) === -1) resources.push(resource.status);
        resources.sort(SortUtils.statusSort);
      });
    }));

    this.filterConfiguration = new FilterConfigurationModel('', statuses, [...shapes, ...gpuTypes, ...gpuCounts], resources, '', '');
  }

  public applyFilter_btnClick(config: FilterConfigurationModel): void {
    const cached = this.loadUserPreferences(config);
    this.cashedFilterForm = JSON.parse(JSON.stringify(cached));
    Object.setPrototypeOf(this.cashedFilterForm, Object.getPrototypeOf(cached));
    let filteredData = this.getEnvironmentsListCopy();
    this.checkFilters();
    const containsStatus = (list, selectedItems) => {
      return list.filter((item: any) => { if (selectedItems.indexOf(item.status) !== -1) return item; });
    };

    if (filteredData.some((v) => v.exploratory.length)) {
      this.filtering = true;
    }

    if (config) {
      this.activeProject = config.project;
      filteredData = filteredData
        .filter(project => config.project ? project.project === config.project : project)
        .filter(project => {

          project.exploratory = project.exploratory.filter(item => {

            const isName = item.name.toLowerCase().indexOf(config.name.toLowerCase()) !== -1;
            const isStatus = config.statuses.length > 0 ? (config.statuses.indexOf(item.status) !== -1) : (config.type !== 'active');

            const isShapeCondition = (config.shapes.indexOf(item.shape) !== -1 ||
                                      config.shapes.indexOf(item.gpu_type) !== -1 ||
                                      config.shapes.indexOf(`GPU count: ${item.gpu_count}`) !== -1 );

            const isShape = config.shapes.length > 0 ? isShapeCondition : true;

            const modifiedResources = containsStatus(item.resources, config.resources);
            let isResources = config.resources.length > 0 ? (modifiedResources.length > 0) : true;

            if (config.resources.length > 0 && modifiedResources.length > 0) { item.resources = modifiedResources; }

            if (config.resources.length === 0 && config.type === 'active' ||
              modifiedResources.length >= 0 && config.resources.length > 0 && config.type === 'active') {
              item.resources = modifiedResources;
              isResources = true;
            }

            return isName && isStatus && isShape && isResources;
          });
          return project.exploratory.length > 0;
        });

      this.updateUserPreferences(config);
    }

    let failedNotebooks = NotebookModel.notebook(this.getEnvironmentsListCopy());
    failedNotebooks = SortUtils.flatDeep(failedNotebooks, 1).filter(notebook => notebook.status === 'failed');
    if (this.filteredEnvironments.length && this.activeFiltering) {
      let creatingNotebook = NotebookModel.notebook(this.filteredEnvironments);
      creatingNotebook = SortUtils.flatDeep(creatingNotebook, 1).filter(resourse => resourse.status === 'creating');
      const fail = failedNotebooks
        .filter(v => creatingNotebook
          .some(create => create.project === v.project && create.exploratory === v.exploratory && create.resource === v.resource));
      if (fail.length) {
        this.toastr.error('Creating notebook failed!', 'Oops!');
      }
    }

    let failedResource = ComputationModel.computationRes(this.getEnvironmentsListCopy());
    failedResource = SortUtils.flatDeep(failedResource, 2).filter(resourse => resourse.status === 'failed');
    if (this.filteredEnvironments.length && this.activeFiltering) {
      let creatingResource = ComputationModel.computationRes(this.filteredEnvironments);
      creatingResource = SortUtils.flatDeep(creatingResource, 2).filter(resourse => resourse.status === 'creating');
      const fail = failedResource
        .filter(v => creatingResource
          .some(create => create.project === v.project && create.exploratory === v.exploratory && create.resource === v.resource));
      if (fail.length) {
        this.toastr.error('Creating computation resource failed!', 'Oops!');
      }
    }
    this.filteredEnvironments = filteredData;
  }

  private modifyGrid(): void {
    this.displayedColumns = this.displayedColumns.filter(el => el !== 'cost');
    this.displayedFilterColumns = this.displayedFilterColumns.filter(el => el !== 'cost-filter');
  }

  private aliveStatuses(config): void {
    for (const index in this.filterConfiguration) {
      if (config[index] && config[index] instanceof Array)
        config[index] = config[index].filter(item => this.filterConfiguration[index].includes(item));
    }
    return config;
  }

  isActiveFilter(filterConfig): void {
    this.activeFiltering = false;

    for (const index in filterConfig)
      if (filterConfig[index].length) this.activeFiltering = true;
  }

  private getUserPreferences(): void {
    this.userResourceService.getUserPreferences()
      .subscribe(
        (result: FilterConfigurationModel) => {
          if (result) {
            this.isActiveFilter(result);
            this.filterForm = this.loadUserPreferences(result.type ? this.filterActiveInstances() : this.aliveStatuses(result));
          }
          this.applyFilter_btnClick(result || this.filterForm);
          this.checkFilters();
        },
        () => this.applyFilter_btnClick(null)
      );
  }

  private loadUserPreferences(config): FilterConfigurationModel {
    return new FilterConfigurationModel(config.name, config.statuses, config.shapes, config.resources, config.type, config.project);
  }

  private updateUserPreferences(filterConfiguration: FilterConfigurationModel): void {
    this.userResourceService.updateUserPreferences(filterConfiguration)
      .subscribe(() => { },
        (error) => console.log('UPDATE USER PREFERENCES ERROR ', error));
  }

  private odahuAction(element: any, action: string) {
    this.dialog.open(OdahuActionDialogComponent, {data: {type: action, item: element}, panelClass: 'modal-sm'})
      .afterClosed().subscribe(result => {
        result && this.odahuDeploymentService.odahuAction(element,  action).subscribe(v =>
            this.buildGrid(),
          error => this.toastr.error(`Odahu cluster ${action} failed!`, 'Oops!')
        ) ;
      }, error => this.toastr.error(error.message || `Odahu cluster ${action} failed!`, 'Oops!')
    );
  }

  public logAction(name) {
    this.auditService.sendDataToAudit({
      resource_name: name, info: `Open terminal, requested for notebook`, type: 'WEB_TERMINAL'
    }).subscribe();
  }

  public trackBy(index, item) {
    return null;
  }

  public onFilterNameUpdate(targetElement: any) {
    this.filterForm.name = targetElement;
    this.checkFilters();
  }

  public checkLibStatus(element) {
    let installingLib = [];
    if (element.libs) {
      installingLib = element.libs.filter(lib => lib.status === 'installing');
    }
    return !!installingLib.length;
  }
}
