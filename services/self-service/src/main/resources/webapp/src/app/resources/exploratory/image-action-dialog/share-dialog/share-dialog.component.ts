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

import { ChangeDetectionStrategy, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { SharePlaceholder, TabName, UserDataTypeConfig } from '../image-action.config';
import { DialogWindowTabConfig, UserData, UserDataType } from '../image-action.model';
import { NgModel } from '@angular/forms';
import { ImagesService } from '../../../images/images.service';
import { MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { ImageActionModalData, ImageParams, ModalTitle, ProjectImagesInfo, Toaster_Message, UnShareModal } from '../../../images';
import { switchMap, tap } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { UnShareWarningComponent } from '../unshare-warning/un-share-warning.component';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'datalab-share-dialog',
  templateUrl: './share-dialog.component.html',
  styleUrls: ['./share-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShareDialogComponent implements OnInit {
  @ViewChild('searchUser') searchUser: NgModel;

  readonly placeholder: typeof SharePlaceholder = SharePlaceholder;
  readonly tabsName: typeof TabName = TabName;
  readonly userDataTypeConfig: typeof UserDataTypeConfig = UserDataTypeConfig;

  userDataList: UserData[] = [];
  temporaryUserDataList: UserData[] = [];
  userNameOrGroup: UserDataType;
  activeTabConfig: DialogWindowTabConfig = {
    shareImage: true,
    shareWith: false
  };
  searchInput = '';

  $getUserListData: Observable<UserData[]>;

  constructor(
    private imagesService: ImagesService,
    @Inject(MAT_DIALOG_DATA) public data: ImageActionModalData,
    private dialog: MatDialog,
    public toastr: ToastrService,
  ) {
  }

  ngOnInit(): void {
    this.initUserList();
  }

  onAddUser(): void {
    if (!this.searchInput) {
      return;
    }
    const newUserEntity: UserData = {
      value: this.searchInput.trim(),
      type: this.userNameOrGroup
    };
    this.temporaryUserDataList = [...this.temporaryUserDataList, newUserEntity]
      .sort((a, b) => a.value < b.value ? 1 : -1)
      .sort((a, b) => a.type > b.type ? 1 : -1);
    this.searchInput = '';
  }

  onTabTitle(tabName: keyof DialogWindowTabConfig): void {
    Object.keys(this.activeTabConfig).forEach(item => this.activeTabConfig[item] = false);
    this.activeTabConfig = {...this.activeTabConfig, [tabName]: true};
  }

  onRemoveUserData(userName: string): void {
    this.temporaryUserDataList = this.temporaryUserDataList.filter(({value}) => value !== userName);
  }

  unShare(userName: string): void {
    const data: UnShareModal = {
      userName,
      title: ModalTitle.unShare
    };
    const filteredList = this.userDataList.filter(({value}) => value !== userName);
    const imageInfo = this.imagesService.createImageRequestInfo(this.data.image, filteredList);

    this.$getUserListData = this.dialog.open(UnShareWarningComponent, {
      data,
      panelClass: 'modal-sm'
    }).afterClosed()
      .pipe(
        switchMap((isShare) => this.sendShareRequest(isShare, imageInfo)),
        switchMap(() =>  this.getSharingUserList()),
        tap(() => this.toastr.success(Toaster_Message.successUnShare, Toaster_Message.successTitle))
      );
  }

  private sendShareRequest(flag: boolean, imageInfo: ImageParams): Observable<ProjectImagesInfo> {
    if (!flag) {
      return EMPTY;
    }
    return  this.imagesService.shareImageAllUsers(imageInfo);
  }

  private initUserList(): void {
    this.$getUserListData = this.getSharingUserList();
  }

  private getSharingUserList(): Observable<UserData[]> {
    const { name, project, endpoint} = this.data.image;
    const imageParams = {
      imageName: name,
      projectName: project,
      endpoint
    };
    return this.imagesService.getImageShareInfo(imageParams).pipe(
      tap(userListData => this.userDataList = userListData)
    );
  }

  get responseObj(): UserData[] {
    return [...this.temporaryUserDataList, ...this.userDataList];
  }

  get isApplyBtnDisabled(): boolean {
    return this.searchInput.length > 25 || !Boolean(this.temporaryUserDataList.length);
  }

  get isAddUserDataBtnDisabled(): boolean {
    return !Boolean(this.userNameOrGroup) || this.searchInput.length > 25 || !Boolean(this.searchInput.length);
  }

  get isSearchUserTouched(): boolean {
    return this.searchUser?.control.touched && !Boolean(this.userNameOrGroup);
  }

  get isUserInputEmpty(): boolean {
    return this.searchUser?.control.touched
      && !Boolean(this.searchInput.length)
      && !Boolean(this.temporaryUserDataList.length);
  }

  get isLongInputMessage() {
    return this.searchInput.length > 25;
  }
}
