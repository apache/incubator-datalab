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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map, tap} from 'rxjs/operators';

import { ToastrService } from 'ngx-toastr';

import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { ApplicationSecurityService, HealthStatusService } from '../../core/services';
import { FilteredColumnList, ImageFilterFormDropdownData, ImageFilterFormValue, ImageModel, ProjectModel } from './images.model';
import {
  TooltipStatuses,
  Image_Table_Column_Headers,
  Image_Table_Titles,
  ImageStatuses,
  Localstorage_Key,
  Placeholders,
  Shared_Status,
  DropdownFieldNames,
  FilterFormInitialValue,
  ImageModelKeysForFilter,
  DropdownSelectAllValue, FilterFormControlNames,
} from './images.config';
import { ShareImageDialogComponent } from '../exploratory/share-image-dialog/share-image-dialog.component';
import { ImagesService } from './images.service';
import { ProgressBarService } from '../../core/services/progress-bar.service';
import { ImageDetailDialogComponent } from '../exploratory/image-detail-dialog/image-detail-dialog.component';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'datalab-images',
  templateUrl: './images.component.html',
  styleUrls: [
    './images.component.scss',
    '../resources-grid/resources-grid.component.scss',
    '../resources.component.scss'
  ]
})

export class ImagesComponent implements OnInit, OnDestroy {
  readonly tableHeaderCellTitles: typeof Image_Table_Column_Headers = Image_Table_Column_Headers;
  readonly displayedColumns: typeof Image_Table_Titles = Image_Table_Titles;
  readonly placeholder: typeof Placeholders = Placeholders;
  readonly sharedStatus: typeof Shared_Status = Shared_Status;
  readonly imageStatus: typeof ImageStatuses = ImageStatuses;
  readonly columnFieldNames: typeof FilterFormControlNames = FilterFormControlNames;
  readonly dropdownFieldNames: typeof DropdownFieldNames = DropdownFieldNames;

  isActionsOpen: boolean = false;
  healthStatus: GeneralEnvironmentStatus;
  dataSource: Observable<ImageModel[]>;
  projectSource: Observable<ProjectModel[]>;
  checkboxSelected: boolean = false;
  projectList: string[] = [];
  activeProjectName: string = '';
  userName: string;
  isProjectsMoreThanOne: boolean;
  isFilterOpened: Observable<boolean>;
  $filterDropdownData: Observable<ImageFilterFormDropdownData>;
  $filterFormValue: Observable<ImageFilterFormValue>;
  $isProjectListEmpty: Observable<boolean>;
  $filteredColumnState: Observable<FilteredColumnList>;
  $isFiltered: Observable<boolean>;
  isShowActive: boolean = true;

  constructor(
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
    private dialog: MatDialog,
    private imagesService: ImagesService,
    private progressBarService: ProgressBarService,
    private route: ActivatedRoute,
    private applicationSecurityService: ApplicationSecurityService
  ) { }

  ngOnInit(): void {
    this.getEnvironmentHealthStatus();
    this.getUserImagePageInfo();
    this.getUserName();
    this.initImageTable();
    this.initFilterBtn();
    this.getDropdownList();
    this.getFilterFormValue();
    this.getIsProjectListEmpty();
    this.initFilteredColumnState();
    this.initIsImageListFiltered();
  }

  ngOnDestroy(): void {
    this.imagesService.closeFilter();
    this.imagesService.setFilterFormValue(FilterFormInitialValue);
  }

  trackBy(index, item) {
    return null;
  }

  onCheckboxClick(element: ImageModel): void {
    element.isSelected = !element.isSelected;
  }

  allCheckboxToggle(): void {
    this.checkboxSelected = !this.checkboxSelected;
    this.imagesService.changeCheckboxValue(this.checkboxSelected);
  }

  onActionClick(): void {
    this.isActionsOpen = !this.isActionsOpen;
  }

  onSelectClick(projectName: string = ''): void {
    this.imagesService.getActiveProject(projectName);
    this.activeProjectName = projectName;
  }

  onRefreshClick(): void {
    this.imagesService.getImagePageInfo().subscribe();
    this.activeProjectName = '';
  }

  onImageInfoClick(image: ImageModel): void {
    this.dialog.open(ImageDetailDialogComponent, {
      data: {
        image
      },
      panelClass: 'modal-md'
    });
  }

  onShareClick(image: ImageModel): void {
    this.dialog.open(ShareImageDialogComponent, {
      data: {
        image
      },
      panelClass: 'modal-sm'
    }).afterClosed()
      .subscribe(() => {
        this.checkAuthorize();
        this.progressBarService.stopProgressBar();
      });
  }

  onFilterClick(): void {
    this.imagesService.openFilter();
  }

  onFilterApplyClick(filterFormValue: ImageFilterFormValue): void {
    const normalizeFilterFormValue = this.imagesService.normalizeFilterFormValue(filterFormValue, DropdownSelectAllValue);
    this.imagesService.updateFilterColumnState(normalizeFilterFormValue);
    this.imagesService.filterImagePageInfo(normalizeFilterFormValue).subscribe();
    this.imagesService.setFilterFormValue(filterFormValue);
    this.imagesService.checkIsPageFiltered();
    this.imagesService.closeFilter();
  }

  onFilterCancelClick(): void {
    this.imagesService.closeFilter();
  }

  onControlChanges(controlName: keyof ImageFilterFormDropdownData, inputValue: string): void {
    const normalizedInputValue = inputValue.toLowerCase();
    this.imagesService.filterDropdownField(DropdownFieldNames.imageNames, normalizedInputValue);
  }

  toggleShowActive(): void {
    this.isShowActive = !this.isShowActive;
    this.imagesService.showImage(this.isShowActive, ImageModelKeysForFilter.status, ImageStatuses.active);
  }

  onResetFilterClick(event: Event): void {
    event.stopPropagation();
    this.imagesService.filterImagePageInfo(FilterFormInitialValue).subscribe();
    this.imagesService.setFilterFormValue(FilterFormInitialValue);
    this.imagesService.updateFilterColumnState(FilterFormInitialValue);
    this.imagesService.checkIsPageFiltered();
  }

  onResetColumn(dropdownFieldNames: FilterFormControlNames): void {
    console.log(dropdownFieldNames);
    this.imagesService.resetFilterField(dropdownFieldNames, DropdownSelectAllValue);
  }

  private checkAuthorize() {
    this.applicationSecurityService.isLoggedIn().subscribe(() => {
      this.getEnvironmentHealthStatus();
    });
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe(
      (result: GeneralEnvironmentStatus) => {
        this.healthStatus = result;
      },
      error => this.toastr.error(error.message, 'Oops!')
    );
  }

  onClickOutside() {
    this.imagesService.closeFilter();
  }

  private initFilteredColumnState(): void {
    this.$filteredColumnState = this.imagesService.$filteredColumnState;
  }

  private getUserImagePageInfo(): void {
    this.route.data.pipe(
      map(data => data['projectList']),
      tap(({projectImagesInfos}) => this.getProjectList(projectImagesInfos))
    ).subscribe();
  }

  private initImageTable(): void {
    this.dataSource = this.imagesService.$imageList;
    this.projectSource = this.imagesService.$projectList;
  }

  private getProjectList(imagePageList: ProjectModel[]): void {
    if (!imagePageList) {
      return;
    }
    this.projectList = this.imagesService.getProjectNameList(imagePageList);
    this.isProjectsMoreThanOne = this.projectList.length > 1;
    if (!this.isProjectsMoreThanOne) {
      this.activeProjectName = this.projectList[0];
    }
  }

  private getUserName(): void {
    this.userName = localStorage.getItem(Localstorage_Key.userName);
  }

  private initFilterBtn(): void {
    this.isFilterOpened = this.imagesService.$isFilterOpened;
  }

  private getDropdownList(): void {
    this.$filterDropdownData = this.imagesService.$filterDropdownData;
  }

  private getFilterFormValue(): void {
    this.$filterFormValue = this.imagesService.$filterFormValue;
  }

  private getIsProjectListEmpty(): void {
    this.$isProjectListEmpty = this.imagesService.$isProjectListEmpty;
  }

  private initIsImageListFiltered(): void {
    this.$isFiltered = this.imagesService.$isImageListFiltered;
  }
}
