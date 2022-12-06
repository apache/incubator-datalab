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
import { EMPTY, Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { ComponentType, ToastrService } from 'ngx-toastr';

import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { ApplicationSecurityService, HealthStatusService } from '../../core/services';
import {
  FilteredColumnList,
  ImageActionType,
  ImageFilterFormDropdownData,
  ImageFilterFormValue,
  ImageModel,
  ImageParams,
  ProjectModel
} from './images.model';
import {
  Image_Table_Column_Headers,
  Image_Table_Titles,
  ImageStatuses,
  Localstorage_Key,
  Placeholders,
  SharingStatus,
  DropdownFieldNames,
  FilterFormInitialValue,
  ImageModelKeysForFilter,
  DropdownSelectAllValue,
  FilterFormControlNames,
  ImageActions,
  Toaster_Message,
} from './images.config';
import { ImagesService } from './images.service';
import { ProgressBarService } from '../../core/services/progress-bar.service';
import { ImageDetailDialogComponent } from '../exploratory/image-detail-dialog/image-detail-dialog.component';
import { ActivatedRoute } from '@angular/router';
import { ShareDialogComponent } from '../exploratory/image-action-dialog/share-dialog/share-dialog.component';
import { TerminateDialogComponent } from '../exploratory/image-action-dialog/terminate-dialog/terminate-dialog.component';

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
  readonly sharedStatus: typeof SharingStatus = SharingStatus;
  readonly imageStatus: typeof ImageStatuses = ImageStatuses;
  readonly columnFieldNames: typeof FilterFormControlNames = FilterFormControlNames;
  readonly dropdownFieldNames: typeof DropdownFieldNames = DropdownFieldNames;
  readonly imageActionType: typeof ImageActions = ImageActions;

  isActionsOpen: boolean = false;
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

  onActionBtnClick(): void {
    this.isActionsOpen = !this.isActionsOpen;
  }

  isObjectFieldTrue(elementField: ImageModel): boolean {
    return (<any>Object).values(elementField).some(item => Boolean(item));
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

  onActionClick(image: ImageModel, actionType: ImageActionType): void {
    let imageInfo: ImageParams;
    const data = this.imagesService.createActionDialogConfig(image, actionType);
    const requestCallback = this.imagesService.getRequestByAction(actionType).bind(this.imagesService);
    const component = this.getComponentByAction(actionType);

    this.dialog.open(component, {
      data,
      panelClass: 'modal-sm'
    }).afterClosed()
      .pipe(
        tap(userDataList => {
          imageInfo = this.imagesService.createImageRequestInfo(image, userDataList);
        }),
        switchMap((confirm) => {
          if (confirm) {
            return requestCallback(imageInfo, actionType);
          }
          return EMPTY;
        }),
        tap(() => this.callActionHelpers(actionType, this.callToasterShareSuccess)),
        catchError(() => of(this.callToasterShareError(actionType)))
    )
      .subscribe();
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
    this.imagesService.resetFilterField(dropdownFieldNames, DropdownSelectAllValue);
  }

  onClickOutside(): void {
    this.imagesService.closeFilter();
  }

  private getComponentByAction(actionType): ComponentType<unknown> {
    const componentList = {
      share: ShareDialogComponent,
      terminate: TerminateDialogComponent
    };
    return componentList[actionType];
  }

  private callActionHelpers(actionType: ImageActionType, callback?: (actionType: string) => void): void {
    const toasterInvoke = callback.bind(this);
    toasterInvoke(actionType);
    this.checkAuthorize();
    this.progressBarService.stopProgressBar();
  }

  private callToasterShareSuccess(actionType: ImageActionType): void {
    if (actionType === ImageActions.share) {
      this.toastr.success(Toaster_Message.successShare, 'Success!');
    }
  }

  private callToasterShareError(actionType: ImageActionType): void {
    if (actionType === ImageActions.share) {
      this.toastr.error('Something went wrong. Please try again.', 'Oops!');
    }
  }

  private checkAuthorize(): void {
    this.applicationSecurityService.isLoggedIn().subscribe(() => {
      this.getEnvironmentHealthStatus();
    });
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe();
  }

  private initFilteredColumnState(): void {
    this.$filteredColumnState = this.imagesService.filteredColumnState$;
  }

  private getUserImagePageInfo(): void {
    this.route.data.pipe(
      map(data => data['projectList']),
      tap((response) => this.imagesService.initImagePageInfo(response)),
      tap(({projectImagesInfos}) => this.getProjectList(projectImagesInfos)),
    ).subscribe();
  }

  private initImageTable(): void {
    this.dataSource = this.imagesService.imageList$;
    this.projectSource = this.imagesService.projectList$;
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
    this.isFilterOpened = this.imagesService.isFilterOpened$;
  }

  private getDropdownList(): void {
    this.$filterDropdownData = this.imagesService.filterDropdownData$;
  }

  private getFilterFormValue(): void {
    this.$filterFormValue = this.imagesService.filterFormValue$;
  }

  private getIsProjectListEmpty(): void {
    this.$isProjectListEmpty = this.imagesService.isProjectListEmpty$;
  }

  private initIsImageListFiltered(): void {
    this.$isFiltered = this.imagesService.isImageListFiltered$;
  }
}
