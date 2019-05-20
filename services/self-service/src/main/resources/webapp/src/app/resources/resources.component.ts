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
import { ExploratoryEnvironmentCreateComponent } from './exploratory/exploratory-environment-create-dialog/exploratory-environment-create-dialog.component';
import { UserAccessKeyService, HealthStatusService } from '../core/services';
import { ResourcesGridRowModel } from './resources-grid/resources-grid.model';
import { HTTP_STATUS_CODES, FileUtils } from '../core/util';

@Component({
  selector: 'dlab-resources',
  templateUrl: 'resources.component.html',
  styleUrls: ['./resources.component.scss']
})

export class ResourcesComponent implements OnInit, OnDestroy {

  public userUploadAccessKeyState: number;
  public exploratoryEnvironments: Array<ResourcesGridRowModel> = [];
  public healthStatus: any;

  @ViewChild('createAnalyticalModal') createAnalyticalModal;
  @ViewChild('manageUngitDialog') manageUngitDialog;
  @ViewChild(ResourcesGridComponent) resourcesGrid: ResourcesGridComponent;

  subscriptions: Subscription = new Subscription();

  constructor(
    public toastr: ToastrService,
    private userAccessKeyService: UserAccessKeyService,
    private healthStatusService: HealthStatusService,
    private dialog: MatDialog
  ) {
    this.userUploadAccessKeyState = HTTP_STATUS_CODES.NOT_FOUND;
  }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    // this.createAnalyticalModal.resourceGrid = this.resourcesGrid;

    this.subscriptions.add(this.userAccessKeyService.accessKeyEmitter.subscribe(response => {
      if (response) this.userUploadAccessKeyState = response.status;
    }));
    this.subscriptions.add(this.userAccessKeyService.keyUploadProccessEmitter.subscribe(response => {
      if (response) this.refreshGrid();
    }));
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  public createNotebook_btnClick(): void {
    if (this.userUploadAccessKeyState === HTTP_STATUS_CODES.OK) {
      // if (!this.createAnalyticalModal.isOpened) this.createAnalyticalModal.open({ isFooter: false });

      this.dialog.open(ExploratoryEnvironmentCreateComponent, { data: this.resourcesGrid })
               .afterClosed().subscribe(() => this.refreshGrid());
    } else {
      this.userAccessKeyService.initialUserAccessKeyCheck();
    }
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
    if (!this.manageUngitDialog.isOpened)
        this.manageUngitDialog.open({ isFooter: false });
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe(
        (result: any) => {
          this.healthStatus = result;
          this.resourcesGrid.healthStatus = this.healthStatus;
          this.userAccessKeyService.initialUserAccessKeyCheck();
        },
      error => this.toastr.error(error.message, 'Oops!'));
  }
}
