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

import { Injectable } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable } from 'rxjs';
import { take, tap } from 'rxjs/operators';

import isEqual from 'lodash.isequal';

import { ImagesService } from '../../../images/images.service';
import { ImageActionDialogModule } from '../image-action-dialog.module';
import { ImageModel, ImageParams, ProjectImagesInfo } from '../../../images';
import { ShareDialogData, UserData } from '../image-action.model';

@Injectable({
  providedIn: ImageActionDialogModule
})
export class ShareDialogService {
  private searchUserDataDropdownList$$: BehaviorSubject<UserData[]> = new BehaviorSubject<UserData[]>([]);
  private userDataList$$: BehaviorSubject<UserData[]> = new BehaviorSubject<UserData[]>([]);
  private temporaryUserDataList$$: BehaviorSubject<UserData[]> = new BehaviorSubject<UserData[]>([]);
  private cashedUserDataDropdownList: UserData[];

  searchUserDataDropdownList$: Observable<UserData[]> = this.searchUserDataDropdownList$$.asObservable();
  userDataList$: Observable<UserData[]> = this.userDataList$$.asObservable();
  temporaryUserDataList$: Observable<UserData[]> = this.temporaryUserDataList$$.asObservable();
  imageInfo: ImageParams;

  constructor(
    private imagesService: ImagesService,
  ) { }

  getUserDataForShareDropdown(userData: string = ''): Observable<UserData[]> {
    return this.imagesService.getUserDataForShareDropdown(userData, this.imageInfo)
      .pipe(
        tap(response => this.filterSearchDropdown(response))
      );
  }

  sendShareRequest(flag: boolean, image: ImageModel, userData: UserData): Observable<ProjectImagesInfo> {
    const filteredList = this.filterSharingList(userData);
    const imageInfo = this.imagesService.createImageRequestInfo(image, filteredList);
    if (!flag) {
      return EMPTY;
    }
    return  this.imagesService.shareImageAllUsers(imageInfo);
  }

  getImageShareInfo(): Observable<ShareDialogData> {
    return this.imagesService.getImageShareInfo(this.imageInfo).pipe(
      tap(({canBeSharedWith, sharedWith}) => {
        this.cashedUserDataDropdownList = canBeSharedWith;
        this.searchUserDataDropdownList$$.next(canBeSharedWith);
        this.userDataList$$.next(sharedWith);
      }),
      take(1)
    );
  }

  filterSharingList(userData: UserData): UserData[] {
    return this.userDataList$$.value.filter(item => !isEqual(item, userData));
  }

  getSharingUserDataList(): UserData[] {
    return [...this.userDataList$$.value, ...this.temporaryUserDataList$$.value];
  }

  addToTemporaryList(user: UserData): void {
    const filteredList = [...this.temporaryUserDataList$$.value, user]
      .sort((a, b) => a.value < b.value ? 1 : -1)
      .sort((a, b) => a.type > b.type ? 1 : -1);
    this.temporaryUserDataList$$.next(filteredList);
  }

  isTemporaryListEmpty(): boolean {
    return !Boolean(this.temporaryUserDataList$$.value.length);
  }

  clearTemporaryList(): void {
    this.temporaryUserDataList$$.next([]);
  }

  removeUserFromTemporaryList(userData: UserData): void {
    const filteredList = this.temporaryUserDataList$$.value.filter(item => !isEqual(item, userData));
    this.temporaryUserDataList$$.next(filteredList);
  }

  filterSearchDropdown(userData: UserData[] = this.cashedUserDataDropdownList): void {
   const filteredDropdownList = userData.filter(item => !this.getSharingUserDataList()
     .some(temporaryDataItem => isEqual(item, temporaryDataItem))
   );
    this.searchUserDataDropdownList$$.next(filteredDropdownList);
  }

  getImageParams(image: ImageModel): void {
    this.imageInfo = this.imagesService.createImageRequestInfo(image);
  }
}
