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

import { Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { SharePlaceholder } from '../image-action.config';
import { ShareDialogData, UserData } from '../image-action.model';
import { FormControl } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import {
  ImageActionModalData,
  ModalTitle,
  Toaster_Message,
  UnShareModal,
} from '../../../images';
import { switchMap, tap } from 'rxjs/operators';
import { Observable, timer } from 'rxjs';
import { UnShareWarningComponent } from '../unshare-warning/un-share-warning.component';
import { ToastrService } from 'ngx-toastr';
import { ShareDialogService } from './share-dialog.service';

@Component({
  selector: 'datalab-share-dialog',
  templateUrl: './share-dialog.component.html',
  styleUrls: ['./share-dialog.component.scss'],
})

export class ShareDialogComponent implements OnInit, OnDestroy {
  @ViewChild('searchUserData') searchUserData: ElementRef;

  readonly placeholder: typeof SharePlaceholder = SharePlaceholder;

  searchUserDataDropdownList$: Observable<UserData[]>;
  userDataList$!: Observable<UserData[]>;
  temporaryUserDataList$!: Observable<UserData[]>;
  sharingDataList: UserData[] = [];
  activeTabIndex: boolean = false;
  addUserDataControl: FormControl = new FormControl('');

  onInputSubscription$: Observable<UserData[]>;
  getUserListDataSubscription$: Observable<ShareDialogData>;
  getUserListDataOnChangeSubscription$: Observable<UserData[]>;

  constructor(
    public toastr: ToastrService,
    @Inject(MAT_DIALOG_DATA) public data: ImageActionModalData,
    private dialog: MatDialog,
    private shareDialogService: ShareDialogService
  ) {
  }

  ngOnInit(): void {
    this.getImageParams();
    this.getUserListDataSubscription$ = this.shareDialogService.getImageShareInfo();
    this.initUserData();
    this.initSearchDropdownList();
    this.initTemporaryUserDataList();
  }

  ngOnDestroy(): void {
    this.shareDialogService.clearTemporaryList();
  }

  onInputKeyDown(): void {
    this.onInputSubscription$ = timer(300).pipe(
      switchMap(() => this.shareDialogService.getUserDataForShareDropdown(this.addUserDataControl.value))
    );
  }

  onChange(): void {
    this.addUserDataControl.setValue('');
    // We need a timer because the click event cannot have time to select userData from the dropdown list.
    this.getUserListDataOnChangeSubscription$ = timer(300).pipe(
      switchMap(() => this.shareDialogService.getUserDataForShareDropdown())
    );
  }

  addUserToTemporaryList(user: UserData): void {
    if (!user) {
      return;
    }
    this.shareDialogService.addToTemporaryList(user);
    this.addUserDataControl.setValue('');
    this.searchUserData.nativeElement.blur();
    this.sharingDataList = this.shareDialogService.getSharingUserDataList();
    this.shareDialogService.filterSearchDropdown();
  }

  onTabTitle(): void {
    this.activeTabIndex = !this.activeTabIndex;
  }

  onRemoveUserData(userData: UserData): void {
    this.shareDialogService.removeUserFromTemporaryList(userData);
    this.shareDialogService.filterSearchDropdown();
  }

  unShare(userData: UserData): void {
    const data: UnShareModal = {
      userData,
      title: ModalTitle.unShare
    };

    this.getUserListDataSubscription$ = this.dialog.open(UnShareWarningComponent, {
      data,
      panelClass: 'modal-sm'
    }).afterClosed()
      .pipe(
        switchMap((isShare) => this.shareDialogService.sendShareRequest(isShare, this.data.image, userData)),
        switchMap(() =>  this.shareDialogService.getImageShareInfo()),
        tap(() => this.toastr.success(Toaster_Message.successUnShare, Toaster_Message.successTitle))
      );
  }

  private getImageParams(): void {
    this.shareDialogService.getImageParams(this.data.image);
  }

  private initUserData(): void {
    this.userDataList$ = this.shareDialogService.userDataList$;
  }

  private initSearchDropdownList(): void {
    this.searchUserDataDropdownList$ = this.shareDialogService.searchUserDataDropdownList$;
  }

  private initTemporaryUserDataList(): void {
    this.temporaryUserDataList$ = this.shareDialogService.temporaryUserDataList$;
  }

  get isShareBtnDisabled(): boolean {
    return !this.shareDialogService.isTemporaryListEmpty();
  }

  get isUserDataListEmpty(): boolean {
    return this.addUserDataControl.touched && this.shareDialogService.isTemporaryListEmpty();
  }
}
