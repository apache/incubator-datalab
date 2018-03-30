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
import { ResourcesGridComponent } from './resources-grid';
import { UserAccessKeyService, UserResourceService, HealthStatusService, AppRoutingService } from '../core/services';
import { ExploratoryEnvironmentVersionModel, ComputationalResourceImage } from '../core/models';
import { HTTP_STATUS_CODES } from '../core/util';
import { NavbarComponent } from '../shared';

@Component({
  moduleId: module.id,
  selector: 'dlab-resources',
  templateUrl: 'resources.component.html',
  styleUrls: ['./resources.component.css']
})

export class ResourcesComponent implements OnInit {

  userUploadAccessKeyState: number;
  exploratoryEnvironments: Array<ExploratoryEnvironmentVersionModel> = [];
  computationalResources: Array<ComputationalResourceImage> = [];
  progressDialogConfig: any;
  healthStatus: any;
  billingEnabled: boolean;
  backupAllowed: boolean;

  @ViewChild('keyUploadModal') keyUploadModal;
  @ViewChild('preloaderModal') preloaderModal;
  @ViewChild('createAnalyticalModal') createAnalyticalModal;
  @ViewChild('manageUngitDialog') manageUngitDialog;
  @ViewChild(ResourcesGridComponent) resourcesGrid: ResourcesGridComponent;
  @ViewChild(NavbarComponent) navbarComponent: NavbarComponent;

  private readonly CHECK_ACCESS_KEY_TIMEOUT: number = 20000;

  constructor(
    private userAccessKeyService: UserAccessKeyService,
    private userResourceService: UserResourceService,
    private healthStatusService: HealthStatusService,
    private appRoutingService: AppRoutingService
  ) {
    this.userUploadAccessKeyState = HTTP_STATUS_CODES.NOT_FOUND;
  }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.checkInfrastructureCreationProgress();
    this.progressDialogConfig = this.setProgressDialogConfiguration();

    this.createAnalyticalModal.resourceGrid = this.resourcesGrid;
  }

  public createNotebook_btnClick(): void {
    this.processAccessKeyStatus(this.userUploadAccessKeyState, true);
  }

  public refreshGrid(): void {
    this.resourcesGrid.buildGrid();
    this.getEnvironmentHealthStatus();
  }

  public toggleFiltering(): void {
    if (this.resourcesGrid.activeFiltering) {
      this.resourcesGrid.resetFilterConfigurations();
    } else {
      this.resourcesGrid.showActiveInstances();
    }
  }

  public checkInfrastructureCreationProgress() {
    this.userAccessKeyService.checkUserAccessKey()
      .subscribe(
      response => this.processAccessKeyStatus(response.status, false),
      error => this.processAccessKeyStatus(error.status, false)
      );
  }

  public manageUngit(): void {
    if (!this.manageUngitDialog.isOpened)
        this.manageUngitDialog.open({ isFooter: false });
  }

  public generateUserKey($event) {
    this.userAccessKeyService.generateAccessKey().subscribe(data => this.downloadFile(data));
  }

  private downloadFile(data: any) {
    const fileName = data.headers.get('content-disposition').match(/filename="(.+)"/)[1];

    let parsedResponse = data.text();
    let blob = new Blob([parsedResponse]);
    let url = window.URL.createObjectURL(blob);

    if (navigator.msSaveOrOpenBlob) {
        navigator.msSaveBlob(blob, fileName);
    } else {
        let a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }
    window.URL.revokeObjectURL(url);

    this.checkInfrastructureCreationProgress();
  }

  private toggleDialogs(keyUploadDialogToggle, preloaderDialogToggle, createAnalyticalToolDialogToggle) {

    if (keyUploadDialogToggle) {
      this.keyUploadModal.open({ isFooter: false });
    } else {
      this.keyUploadModal.close();
    }

    if (preloaderDialogToggle) {
      this.preloaderModal.open({ isHeader: false, isFooter: false });
    } else {
      this.preloaderModal.close();
    }

    if (createAnalyticalToolDialogToggle) {
      if (!this.createAnalyticalModal.isOpened)
        this.createAnalyticalModal.open({ isFooter: false });
    } else {
      if (this.createAnalyticalModal.isOpened)
        this.createAnalyticalModal.close();
    }
  }

  private processAccessKeyStatus(status: number, forceShowKeyUploadDialog: boolean) {
    this.userUploadAccessKeyState = status;

    if (status === HTTP_STATUS_CODES.NOT_FOUND) {// key haven't been uploaded
      this.toggleDialogs(true, false, false);
    } else if (status === HTTP_STATUS_CODES.ACCEPTED) { // Key uploading
      this.toggleDialogs(false, true, false);
      setTimeout(() => this.checkInfrastructureCreationProgress(), this.CHECK_ACCESS_KEY_TIMEOUT);
    } else if (status === HTTP_STATUS_CODES.OK && forceShowKeyUploadDialog) {
      this.toggleDialogs(false, false, true);
    } else if (status === HTTP_STATUS_CODES.OK) { // Key uploaded
      this.toggleDialogs(false, false, false);
    }
  }

  private setProgressDialogConfiguration() {
    return {
      message: 'Initial infrastructure is being created, <br/>please, wait...',
      content: '<img src="assets/img/gif-spinner.gif" alt="">',
      modal_size: 'modal-xs',
      text_style: 'info-label',
      aligning: 'text-center'
    };
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe(
        (result: any) => {
          this.healthStatus = result.status;
          this.billingEnabled = result.billingEnabled;
          this.backupAllowed = result.backupAllowed;

          this.resourcesGrid.healthStatus = this.healthStatus;
          this.resourcesGrid.billingEnabled = this.billingEnabled;
        });
  }
}
