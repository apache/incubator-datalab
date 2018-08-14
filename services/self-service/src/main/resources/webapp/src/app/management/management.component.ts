/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import { Component, OnInit, ViewChild } from '@angular/core';
import { HealthStatusService, ManageEnvironmentsService, UserAccessKeyService } from './../core/services';
import { EnvironmentModel } from './management.model';
import { FileUtils, HTTP_STATUS_CODES } from '../core/util';

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

  private readonly CHECK_ACCESS_KEY_TIMEOUT: number = 20000;

  @ViewChild('keyUploadModal') keyUploadDialog;
  @ViewChild('preloaderModal') preloaderDialog;

  constructor(
    private healthStatusService: HealthStatusService,
    private manageEnvironmentsService: ManageEnvironmentsService,
    private userAccessKeyService: UserAccessKeyService
  ) {}

  ngOnInit() {
    this.buildGrid();
  }

  public buildGrid() {
    this.getEnvironmentHealthStatus();
    this.getAllEnvironmentData();
  }

  public manageEnvironmentAction($event) {
    this.manageEnvironmentsService
      .environmentManagement(
        $event.environment.user,
        $event.action,
        $event.environment.name === 'edge node' ? 'edge' : $event.environment.name,
        $event.resource ? $event.resource.computational_name : null
      )
      .subscribe(() => this.buildGrid(), error => console.log(error));
  }

  public checkUserAccessKey() {
    this.userAccessKeyService.checkUserAccessKey()
      .subscribe(
        response => this.processAccessKeyStatus(response.status),
        error => this.processAccessKeyStatus(error.status));
  }

  private processAccessKeyStatus(status: number) {
    if (status === HTTP_STATUS_CODES.NOT_FOUND) {
      this.healthStatus === 'error' && this.keyUploadDialog.open({ isFooter: false });
      this.uploadKey = false;
    } else if (status === HTTP_STATUS_CODES.ACCEPTED) {
      this.preloaderDialog.open({ isHeader: false, isFooter: false });

      setTimeout(() => this.buildGrid(), this.CHECK_ACCESS_KEY_TIMEOUT);
    } else if (status === HTTP_STATUS_CODES.OK) {
      this.preloaderDialog.close();
      this.keyUploadDialog.close();
      this.uploadKey = true;
    }
  }

  public generateUserKey($event) {
    this.userAccessKeyService.generateAccessKey().subscribe(
      data => {
        FileUtils.downloadFile(data);
        this.buildGrid();
      });
  }

  private getAllEnvironmentData() {
    this.manageEnvironmentsService.getAllEnvironmentData().subscribe((result: any) => {
      this.allEnvironmentData = this.loadEnvironmentList(result);

      console.log(this.allEnvironmentData);
    });
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
    this.healthStatusService.getEnvironmentHealthStatus().subscribe((result: any) => {
      this.healthStatus = result.status;
      this.billingEnabled = result.billingEnabled;
      this.admin = result.admin;

      this.checkUserAccessKey();
    });
  }
}
