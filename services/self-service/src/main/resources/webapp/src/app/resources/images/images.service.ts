import { Injectable } from '@angular/core';
import { take, tap } from 'rxjs/operators';
import { BehaviorSubject, Observable } from 'rxjs';

import {
  FilteredColumnList,
  FilterFormItemType,
  ImageFilterFormDropdownData,
  ImageFilterFormValue,
  ImageModel,
  ProjectImagesInfo,
  ProjectModel,
  ImageParams,
  ImageActionType,
  ImageActionModalData
} from './images.model';
import { ApplicationServiceFacade, UserImagesPageService } from '../../core/services';
import { ChangedColumnStartValue, FilterFormInitialValue, ModalTitle, SharedStatus } from './images.config';
import { ShareDialogComponent } from '../exploratory/image-action-dialog/share-dialog/share-dialog.component';
import { TerminateDialogComponent } from '../exploratory/image-action-dialog/terminate-dialog/terminate-dialog.component';
import { ComponentType } from 'ngx-toastr';
import { UserData } from '../exploratory/image-action-dialog/image-action.model';

@Injectable({
  providedIn: 'root'
})
export class ImagesService {
  private projectList$$: BehaviorSubject<ProjectModel[]> = new BehaviorSubject<ProjectModel[]>([]);
  private isProjectListEmpty$$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  private cashedProjectList$$: BehaviorSubject<ProjectModel[]> = new BehaviorSubject<ProjectModel[]>([]);
  private imageList$$: BehaviorSubject<ImageModel[]> = new BehaviorSubject<ImageModel[]>([]);
  private isFilterOpened$$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  // tslint:disable-next-line:max-line-length
  private filterDropdownData$$: BehaviorSubject<ImageFilterFormDropdownData> = new BehaviorSubject<ImageFilterFormDropdownData>({} as ImageFilterFormDropdownData);
  private filterFormValue$$: BehaviorSubject<ImageFilterFormValue> = new BehaviorSubject<ImageFilterFormValue>(FilterFormInitialValue);
  private filteredColumnState$$: BehaviorSubject<FilteredColumnList> = new BehaviorSubject<FilteredColumnList>(ChangedColumnStartValue);
  private isImageListFiltered$$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  private dropdownStartValue: ImageFilterFormDropdownData;

  projectList$ = this.projectList$$.asObservable();
  isProjectListEmpty$ = this.isProjectListEmpty$$.asObservable();
  imageList$ = this.imageList$$.asObservable();
  isFilterOpened$ = this.isFilterOpened$$.asObservable();
  filterDropdownData$ = this.filterDropdownData$$.asObservable();
  filterFormValue$ = this.filterFormValue$$.asObservable();
  filteredColumnState$ = this.filteredColumnState$$.asObservable();
  isImageListFiltered$ = this.isImageListFiltered$$.asObservable();

  constructor(
    private applicationServiceFacade: ApplicationServiceFacade,
    private userImagesPageService: UserImagesPageService
  ) { }

  getImagePageInfo(): Observable<ProjectImagesInfo> {
    return this.userImagesPageService.getFilterImagePage()
      .pipe(
        tap(imagePageInfo => this.initImagePageInfo(imagePageInfo))
      );
  }

  filterImagePageInfo(params: ImageFilterFormValue): Observable<ProjectImagesInfo> {
    return this.userImagesPageService.filterImagePage(params)
      .pipe(
        tap(value => this.getImagePageData(value.projectImagesInfos)),
        take(1)
      );
  }

  shareImageAllUsers(imageInfo: ImageParams): Observable<ProjectImagesInfo> {
    return this.userImagesPageService.shareImagesAllUser(imageInfo)
      .pipe(
        tap(value => this.getImagePageData(value.projectImagesInfos)),
        take(1)
      );
  }

  terminateImage(imageInfo: ImageParams, action: ImageActionType): Observable<any> {
    return this.userImagesPageService.terminateImage(imageInfo, action)
      .pipe(
        tap(value => this.getImagePageData(JSON.parse(value['body']).projectImagesInfos)),
        take(1)
      );
  }

  getActiveProject(projectName: string): void {
    const projectList = this.cashedProjectList$$.getValue();
    if (!projectName) {
      this.updateProjectList(projectList);
      this.isProjectListEmpty$$.next(this.isProjectListEmpty(projectList));
    } else {
      const currentProject = projectList.find(({project}) => project === projectName);
      this.updateProjectList([currentProject]);
      this.isProjectListEmpty$$.next(this.isProjectListEmpty([currentProject]));
    }
  }

  getProjectNameList(imageList: ProjectModel[]): string[] {
    return imageList.map(({project}) => project);
  }

  updateImageList(imageList: ImageModel[]): void {
    this.imageList$$.next(imageList);
  }

  updateProjectList(projectList: ProjectModel[]): void {
    this.projectList$$.next(projectList);
  }

  changeCheckboxValue(value: boolean): void {
    const updatedImageList = this.imageList$$.value.map(image => {
      image.isSelected = value;
      return image;
    });
    this.imageList$$.next(updatedImageList);
  }

  openFilter(): void {
    this.isFilterOpened$$.next(true);
  }

  closeFilter(): void {
    this.isFilterOpened$$.next(false);
  }

  filterDropdownField(field: keyof ImageFilterFormDropdownData, value: string, ): void {
    const filteredDropdownList = this.dropdownStartValue[field].filter(item => item.toLowerCase().includes(value));
    this.addFilterDropdownData({...this.filterDropdownData$$.value, imageNames: filteredDropdownList});
  }

  resetFilterField(field: keyof ImageFilterFormValue, exceptionValue: string = ''): void {
    const droppedFieldValue = this.getDroppedFieldValue(field);
    const updatedFilterFormValue = {...this.filterFormValue$$.value, [field]: droppedFieldValue};
    const normalizeFormValue = this.normalizeFilterFormValue(updatedFilterFormValue, exceptionValue);
    this.setFilterFormValue(updatedFilterFormValue);
    this.updateFilterColumnState(normalizeFormValue);
    this.filterImagePageInfo(normalizeFormValue).subscribe();
    this.checkIsPageFiltered();
  }

  setFilterFormValue(value: ImageFilterFormValue): void {
    this.filterFormValue$$.next(value);
  }

  showImage(flag: boolean, field: keyof ImageModel, comparedValue: string): void {
    const projectList = this.cashedProjectList$$.getValue();
    if (flag) {
      this.updateProjectList(projectList);
    } else {
      const filteredImageList = this.filterByCondition(this.projectList$$.getValue(), field, comparedValue);
      this.updateProjectList(filteredImageList);
    }
  }

  normalizeFilterFormValue(filterFormValue: ImageFilterFormValue, exceptionValue: string = '') {
    if (!exceptionValue) {
      return filterFormValue;
    }
    return (<any>Object).entries(filterFormValue)
      .reduce((acc, fieldItem) => this.filterFormValue(acc, fieldItem, exceptionValue), <ImageFilterFormValue>{});
  }

  updateFilterColumnState(filterFormValue: ImageFilterFormValue): void {
    const columnStateList = (<any>Object).entries(filterFormValue)
      .reduce((acc, fieldItem) => this.checkColumnState(acc, fieldItem), <FilteredColumnList>{});

    this.filteredColumnState$$.next(columnStateList);
  }

  getDroppedFieldValue(field: keyof ImageFilterFormValue): string | [] {
    return typeof this.filterFormValue$$.value[field] === 'string'
      ? ''
      : [];
  }

  checkIsPageFiltered(): void {
    const isImageListFiltered = (<any>Object).values(this.filteredColumnState$$.value).some(item => Boolean(item));
    this.isImageListFiltered$$.next(isImageListFiltered);
  }

  createImageRequestInfo(image: ImageModel, userDataList?: UserData[]): ImageParams {
    const { name, project, endpoint } = image;
    const imageParams = {
      imageName: name,
      projectName: project,
      endpoint: endpoint,
    };

    if (userDataList) {
      imageParams['sharedWith'] = userDataList;
    }
    return imageParams;
  }

  createActionDialogConfig(image: ImageModel, actionType: ImageActionType): ImageActionModalData {
    const modalTitle = {
      share: ModalTitle.share,
      terminate: ModalTitle.terminate
    };
    return {
      title: modalTitle[actionType],
      actionType,
      image,
      isShared: this.isImageShared(image)
    };
  }

  getRequestByAction(actionType): Function {
    const callbackList = {
      share: this.shareImageAllUsers,
      terminate: this.terminateImage
    };
    return callbackList[actionType];
  }

  getComponentByAction(actionType): ComponentType<unknown> {
    const componentList = {
      share: ShareDialogComponent,
      terminate: TerminateDialogComponent
    };
    return componentList[actionType];
  }

  initImagePageInfo(imagePageInfo: ProjectImagesInfo): void {
    this.getImagePageData(imagePageInfo.projectImagesInfos);
    this.getDropdownDataList(imagePageInfo.filterData);
    this.setFilterFormValue(imagePageInfo.imageFilter);
    this.updateFilterColumnState(imagePageInfo.imageFilter);
    this.checkIsPageFiltered();
  }

  private isImageShared(image: ImageModel): boolean {
    return image.sharingStatus !== SharedStatus.private;
  }

  private checkColumnState(acc: FilteredColumnList, fieldItem: FilterFormItemType): FilteredColumnList {
    const [ fieldName, fieldValue ] = fieldItem;
    let isColumnFiltered: boolean;
    isColumnFiltered = typeof fieldValue === 'string' ? Boolean(fieldValue) : Boolean(fieldValue.length);
    return  {...acc, [fieldName]: isColumnFiltered};
  }

  private filterFormValue(acc: ImageFilterFormValue, fieldItem: FilterFormItemType, exceptionValue: string = ''): ImageFilterFormValue {
    const [ fieldName, fieldValue ] = fieldItem;
    let value;

    if (typeof fieldValue === 'string') {
      value = fieldValue;
    } else {
      value = fieldValue.filter(item => item !== exceptionValue);
    }
    return {...acc, [fieldName]: value};
  }

  private filterByCondition(arr: ProjectModel[], field: keyof ImageModel, comparedValue: string) {
    return arr.map(item => {
      const filteredImageList = item.images.filter(image => image[field] === comparedValue);
      return {...item, images: filteredImageList};
    });
  }

  private updateCashedProjectList(projectList: ProjectModel[]): void {
    this.cashedProjectList$$.next(projectList);
  }

  private getDropdownDataList(dropdownList: ImageFilterFormDropdownData): void {
    this.addFilterDropdownData(dropdownList);
    this.dropdownStartValue = dropdownList;
  }

  private addFilterDropdownData(data: ImageFilterFormDropdownData): void {
    this.filterDropdownData$$.next(data);
  }

  private getImagePageData(imagePageData: ProjectModel[]): void {
    const imageList = this.getImageList(imagePageData);
    this.updateProjectList(imagePageData);
    this.updateImageList(imageList);
    this.updateCashedProjectList(imagePageData);
    this.isProjectListEmpty$$.next(this.isProjectListEmpty(imagePageData));
  }

  private isProjectListEmpty(imagePageData: ProjectModel[]): boolean {
    return imagePageData.every(({images}) => Boolean(!images.length));
  }

  private getImageList(imagePageData: ProjectModel[]): ImageModel[] {
    return imagePageData.reduce((acc: ImageModel[], {images}) => [...acc, ...images], []);
  }
}
