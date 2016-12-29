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
import { UserAccessKeyService } from '../services/userAccessKey.service';
import { UserResourceService } from '../services/userResource.service';
import { ResourcesGrid } from '../components/resources-grid/resources-grid.component';
import { ExploratoryEnvironmentVersionModel } from '../models/exploratoryEnvironmentVersion.model';
import { ComputationalResourceImage } from '../models/computationalResourceImage.model';

import HTTP_STATUS_CODES from 'http-status-enum';

@Component({
  moduleId: module.id,
  selector: 'sd-home',
  templateUrl: 'home.component.html',
  styleUrls: ['./home.component.css']
})

export class HomeComponent implements OnInit {

  userUploadAccessKeyState: number;
  exploratoryEnvironments: Array<ExploratoryEnvironmentVersionModel> = [];
  computationalResources: Array<ComputationalResourceImage> = [];
  progressDialogConfig: any;

  @ViewChild('keyUploadModal') keyUploadModal;
  @ViewChild('preloaderModal') preloaderModal;
  @ViewChild('createAnalyticalModal') createAnalyticalModal;
  @ViewChild(ResourcesGrid) resourcesGrid: ResourcesGrid;

  private readonly CHECK_ACCESS_KEY_TIMEOUT : number = 10000;

  constructor(
    private userAccessKeyService: UserAccessKeyService,
    private userResourceService: UserResourceService
  ) {
    this.userUploadAccessKeyState = HTTP_STATUS_CODES.NOT_FOUND;
  }

  ngOnInit() {
    this.checkInfrastructureCreationProgress();
    this.progressDialogConfig = this.setProgressDialogConfiguration();

    this.createAnalyticalModal.resourceGrid = this.resourcesGrid;
  }

  public createNotebook_btnClick(): void {
    this.processAccessKeyStatus(this.userUploadAccessKeyState, true);
  }

  public refreshGrid(): void {
    this.resourcesGrid.buildGrid();
  }

  private checkInfrastructureCreationProgress() {
    this.userAccessKeyService.checkUserAccessKey()
      .subscribe(
      response => this.processAccessKeyStatus(response.status, false),
      error => this.processAccessKeyStatus(error.status, false)
      );
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
}
