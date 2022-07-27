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

import { Component, OnInit } from '@angular/core';

import { ToastrService } from 'ngx-toastr';

import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { HealthStatusService, UserImagesPageService } from '../../core/services';
import { ImageModel, ProjectModel } from './images.model';
import {
  TooltipStatuses,
  Image_Table_Column_Headers,
  Image_Table_Titles,
  ImageStatuses,
  Localstorage_Key,
  Placeholders,
  Shared_Status,
} from './images.config';
import { MatDialog } from '@angular/material/dialog';
import { ShareImageDialogComponent } from '../exploratory/share-image/share-image-dialog.component';
import { ImagesService } from './images.service';
import { ProgressBarService } from '../../core/services/progress-bar.service';
import { ImageDetailDialogComponent } from '../exploratory/image-detail-dialog/image-detail-dialog.component';

@Component({
  selector: 'datalab-images',
  templateUrl: './images.component.html',
  styleUrls: [
    './images.component.scss',
    '../resources-grid/resources-grid.component.scss',
    '../resources.component.scss'
  ]
})

export class ImagesComponent implements OnInit {
  readonly tableHeaderCellTitles: typeof Image_Table_Column_Headers = Image_Table_Column_Headers;
  readonly displayedColumns: typeof Image_Table_Titles = Image_Table_Titles;
  readonly placeholder: typeof Placeholders = Placeholders;
  readonly sharedStatus: typeof Shared_Status = Shared_Status;
  readonly imageStatus: typeof ImageStatuses = ImageStatuses;
  readonly tooltipStatuses: typeof TooltipStatuses = TooltipStatuses;

  isActionsOpen: boolean = false;
  healthStatus: GeneralEnvironmentStatus;
  dataSource: ImageModel[] = [];
  checkboxSelected: boolean = false;
  projectList: string[] = [];
  activeProjectName: string = '';
  userName!: string;

  private cashedImageListData: ProjectModel[] = [];

  constructor(
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
    private userImagesPageService: UserImagesPageService,
    private dialog: MatDialog,
    private imagesService: ImagesService,
    private progressBarService: ProgressBarService
  ) { }

  ngOnInit(): void {
    this.getEnvironmentHealthStatus();
    this.getUserImagePageInfo();
    this.getUserName();
  }

  onCheckboxClick(element: ImageModel): void {
    element.isSelected = !element.isSelected;
  }

  allCheckboxToggle(): void {
    this.checkboxSelected = !this.checkboxSelected;

    if (this.checkboxSelected) {
      this.dataSource.forEach(image => image.isSelected = true);
    } else {
      this.dataSource.forEach(image => image.isSelected = false);
    }
  }

  onActionClick(): void {
    this.isActionsOpen = !this.isActionsOpen;
  }

  onSelectClick(projectName: string = ''): void {
    if (!projectName) {
      this.dataSource = this.getImageList();
      return;
    }
    const currentProject = this.cashedImageListData.find(({project}) => project === projectName);
    this.dataSource = [...currentProject.images];
    this.activeProjectName = currentProject.project;
  }

  onRefresh(): void {
    this.getUserImagePageInfo();
    this.activeProjectName = '';
  }

  onImageInfo(image: ImageModel): void {
    this.dialog.open(ImageDetailDialogComponent, {
      data: {
        image
      },
      panelClass: 'modal-md'
    });
  }

  onShare(image: ImageModel): void {
    this.dialog.open(ShareImageDialogComponent, {
      data: {
        image
      },
      panelClass: 'modal-sm'
    }).afterClosed()
      .subscribe(() => {
        if (this.imagesService.projectList) {
          this.initImageTable(this.imagesService.projectList);
        }
        this.progressBarService.stopProgressBar();
      });
  }

  private getImageList(): ImageModel[] {
    return this.cashedImageListData.reduce((acc, {images}) => [...acc, ...images], []);
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe(
      (result: GeneralEnvironmentStatus) => {
        this.healthStatus = result;
      },
      error => this.toastr.error(error.message, 'Oops!')
    );
  }

  private getUserImagePageInfo(): void {
    this.userImagesPageService.getUserImagePageInfo().subscribe(imageListData => this.initImageTable(imageListData));
  }

  private initImageTable(imagePageList: ProjectModel[]): void {
    this.cashedImageListData = imagePageList;
    this.getProjectList(imagePageList);
    this.dataSource = this.getImageList();

    if (imagePageList.length === 1) {
      this.activeProjectName = imagePageList[0].project;
    }
  }

  private getProjectList(imagePageList: ProjectModel[]): void {
    if (!imagePageList) {
      return;
    }
    this.projectList = [];
    imagePageList.forEach(({project}) => this.projectList.push(project));
  }

  private getUserName(): void {
    this.userName = localStorage.getItem(Localstorage_Key.userName);
  }

  get isProjectsMoreThanOne(): boolean {
    return this.projectList.length > 1;
  }

  get isImageNotSelected(): boolean {
    return !this.dataSource.some(image => image.isSelected);
  }
}
