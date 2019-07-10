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

import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { MatDialog } from '@angular/material';

import { ResourcesGridComponent } from './resources-grid/resources-grid.component';
import { ExploratoryEnvironmentCreateComponent } from './exploratory/create-environment';
import { ExploratoryModel, Exploratory } from './resources-grid/resources-grid.model';
import { HealthStatusService } from '../core/services';
import { ManageUngitComponent } from './manage-ungit/manage-ungit.component';

@Component({
  selector: 'dlab-resources',
  templateUrl: 'resources.component.html',
  styleUrls: ['./resources.component.scss']
})

export class ResourcesComponent implements OnInit {
  public exploratoryEnvironments: Exploratory[] = [];
  public healthStatus: any;

  @ViewChild(ResourcesGridComponent) resourcesGrid: ResourcesGridComponent;

  constructor(
    public toastr: ToastrService,
    private healthStatusService: HealthStatusService,
    private dialog: MatDialog
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
  }

  public createEnvironment(): void {
    this.dialog.open(ExploratoryEnvironmentCreateComponent, { data: this.resourcesGrid, panelClass: 'modal-lg' })
      .afterClosed().subscribe(() => this.refreshGrid());
  }

  public refreshGrid(): void {
    this.resourcesGrid.buildGrid();
    this.getEnvironmentHealthStatus();
    this.exploratoryEnvironments = this.resourcesGrid.environments;
  }

  public toggleFiltering(): void {
    if (this.resourcesGrid.activeFiltering) {
      this.resourcesGrid.resetFilterConfigurations();
    } else {
      this.resourcesGrid.showActiveInstances();
    }
  }

  public manageUngit(): void {
    this.dialog.open(ManageUngitComponent, { panelClass: 'modal-xxl' })
      .afterClosed().subscribe(() => this.refreshGrid());
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe(
      (result: any) => {
        this.healthStatus = result;
        this.resourcesGrid.healthStatus = this.healthStatus;
      },
      error => this.toastr.error(error.message, 'Oops!'));
  }
}
