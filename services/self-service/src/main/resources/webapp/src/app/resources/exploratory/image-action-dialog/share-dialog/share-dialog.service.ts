import { Injectable } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable } from 'rxjs';
import { take, tap } from 'rxjs/operators';

import isEqual from 'lodash.isequal';

import { ImagesService } from '../../../images/images.service';
import { ImageActionDialogModule } from '../image-action-dialog.module';
import { ImageParams, ProjectImagesInfo } from '../../../images';
import { ShareDialogData, UserData } from '../image-action.model';
import { UserImagesPageService } from '../../../../core/services';

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
    private userImagesPageService: UserImagesPageService
  ) { }

  getUserDataForShareDropdown(userData: string = ''): Observable<UserData[]> {
    return this.userImagesPageService.getUserDataForShareDropdown(this.imageInfo, userData)
      .pipe(
        tap(response => this.filterSearchDropdown(response))
      );
  }

  sendShareRequest(flag: boolean, imageInfo: ImageParams): Observable<ProjectImagesInfo> {
    if (!flag) {
      return EMPTY;
    }
    return  this.imagesService.shareImageAllUsers(imageInfo);
  }

  getImageShareInfo(): Observable<ShareDialogData> {
    return this.userImagesPageService.getImageShareInfo(this.imageInfo).pipe(
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
}
