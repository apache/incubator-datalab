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

import { Component, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

import { HealthStatusService, ManageEnvironmentsService, UserAccessKeyService, AppRoutingService } from '../core/services';
import { EnvironmentModel } from './management.model';

@Component({
  selector: 'environments-management',
  templateUrl: './management.component.html',
  styleUrls: ['./management.component.scss']
})
export class ManagementComponent implements OnInit {
  public healthStatus: string = '';
  public billingEnabled: boolean;
  public admin: boolean;

  public allEnvironmentData: Array<EnvironmentModel>;
  public uploadKey: boolean = true;

  constructor(
    private healthStatusService: HealthStatusService,
    private manageEnvironmentsService: ManageEnvironmentsService,
    private userAccessKeyService: UserAccessKeyService,
    private appRoutingService: AppRoutingService,
    public toastr: ToastrService
  ) {}

  ngOnInit() {
    this.buildGrid();
  }

  public buildGrid() {
    this.getEnvironmentHealthStatus();
  }

  public manageEnvironmentAction($event) {
    this.manageEnvironmentsService
      .environmentManagement(
        $event.environment.user,
        $event.action,
        $event.environment.name === 'edge node' ? 'edge' : $event.environment.name,
        $event.resource ? $event.resource.computational_name : null
      )
      .subscribe(
        () => this.buildGrid(),
        error => this.toastr.error('Environment management failed!', 'Oops!'));
  }

  private getAllEnvironmentData() {
    this.manageEnvironmentsService.getAllEnvironmentData()
        .subscribe((result: any) => this.allEnvironmentData = this.loadEnvironmentList(result));
  }

  private loadEnvironmentList(data): Array<EnvironmentModel> {
    if (data)
      return data.map(value => {
        return new EnvironmentModel(
          value.resource_name || value.resource_type,
          value.status,
          value.shape,
          value.computational_resources,
          value.user
        );
      });
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService
        .getEnvironmentHealthStatus()
        .subscribe(result => {
          this.healthStatus = result.status;
          this.billingEnabled = result.billingEnabled;
          this.admin = result.admin;

          if (!this.admin) {
            this.appRoutingService.redirectToNoAccessPage();
            return false;
          }

          this.getAllEnvironmentData();
          this.userAccessKeyService.initialUserAccessKeyCheck();
        });
  }
}
