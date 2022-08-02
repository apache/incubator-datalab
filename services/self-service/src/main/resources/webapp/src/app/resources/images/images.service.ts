import { Injectable } from '@angular/core';
import { take, tap } from 'rxjs/operators';
import { BehaviorSubject, Observable } from 'rxjs';

import {
  FilteredColumnList,
  ImageFilterFormDropdownData,
  ImageFilterFormValue,
  ImageModel,
  ProjectModel,
  ShareImageAllUsersParams
} from './images.model';
import { ApplicationServiceFacade, UserImagesPageService } from '../../core/services';
import { ChangedColumnStartValue, FilterFormInitialValue, ImageModelNames } from './images.config';
import { caseInsensitiveSortUtil } from '../../core/util';

@Injectable({
  providedIn: 'root'
})
export class ImagesService {
  private $$projectList: BehaviorSubject<ProjectModel[]> = new BehaviorSubject<ProjectModel[]>([]);
  private $$isProjectListEmpty: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  private $$cashedProjectList: BehaviorSubject<ProjectModel[]> = new BehaviorSubject<ProjectModel[]>([]);
  private $$imageList: BehaviorSubject<ImageModel[]> = new BehaviorSubject<ImageModel[]>([]);
  private $$isFilterOpened: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  // tslint:disable-next-line:max-line-length
  private $$filterDropdownData: BehaviorSubject<ImageFilterFormDropdownData> = new BehaviorSubject<ImageFilterFormDropdownData>({} as ImageFilterFormDropdownData);
  private $$filterFormValue: BehaviorSubject<ImageFilterFormValue> = new BehaviorSubject<ImageFilterFormValue>(FilterFormInitialValue);
  private $$changedColumn: BehaviorSubject<FilteredColumnList> = new BehaviorSubject<FilteredColumnList>(ChangedColumnStartValue);
  private dropdownStartValue: ImageFilterFormDropdownData;

  $projectList = this.$$projectList.asObservable();
  $isProjectListEmpty = this.$$isProjectListEmpty.asObservable();
  $imageList = this.$$imageList.asObservable();
  $isFilterOpened = this.$$isFilterOpened.asObservable();
  $filterDropdownData = this.$$filterDropdownData.asObservable();
  $filterFormValue = this.$$filterFormValue.asObservable();

  constructor(
    private applicationServiceFacade: ApplicationServiceFacade,
    private userImagesPageService: UserImagesPageService
  ) { }

  getImagePageInfo(): Observable<ProjectModel[]> {
    return this.userImagesPageService.getFilterImagePage()
      .pipe(
        tap(value => this.getImagePageData(value)),
        take(1)
      );
  }

  filterImagePageInfo(params: ImageFilterFormValue): Observable<ProjectModel[]> {
    return this.userImagesPageService.filterImagePage(params)
      .pipe(
        tap(value => this.getImagePageData(value)),
        take(1)
      );
  }

  shareImageAllUsers(image: ImageModel): Observable<ProjectModel[]> {
    const shareParams: ShareImageAllUsersParams = {
      imageName: image.name,
      projectName: image.project,
      endpoint: image.endpoint
    };

    return this.userImagesPageService.shareImagesAllUser(shareParams)
      .pipe(
        tap(value => this.getImagePageData(value)),
        take(1)
      );
  }

  getActiveProject(projectName: string): void {
    if (!projectName) {
      this.updateProjectList(this.$$cashedProjectList.getValue());
    } else {
      const currentProject = this.$$cashedProjectList.getValue().find(({project}) => project === projectName);
      this.updateProjectList([currentProject]);
    }
  }

  getProjectNameList(imageList: ProjectModel[]): string[] {
    return imageList.map(({project}) => project);
  }

  updateImageList(imageList: ImageModel[]): void {
    this.$$imageList.next(imageList);
  }

  updateProjectList(projectList: ProjectModel[]): void {
    this.$$projectList.next(projectList);
  }

  changeCheckboxValue(value: boolean): void {
    const updatedImageList = this.$$imageList.value.map(image => {
      image.isSelected = value;
      return image;
    });
    this.$$imageList.next(updatedImageList);
  }

  isImageSelected(): boolean {
    return this.$$imageList.value.some(image => image.isSelected);
  }

  openFilter(): void {
    this.$$isFilterOpened.next(true);
  }

  closeFilter(): void {
    this.$$isFilterOpened.next(false);
  }

  filterDropdownField(field: keyof ImageFilterFormDropdownData, value: string, ) {
    const filteredDropdownList = this.dropdownStartValue[field].filter(item => item.toLowerCase().includes(value));
    this.addFilterDropdownData({...this.$$filterDropdownData.value, imageName: filteredDropdownList});
  }

  setFilterFormValue(value: ImageFilterFormValue): void {
    this.$$filterFormValue.next(value);
  }

  showImage(flag: boolean, field: keyof ImageModel, comparedValue: string): void {
    const projectList = this.$$cashedProjectList.getValue();
    if (flag) {
      this.updateProjectList(projectList);
    } else {
      const filteredImageList = this.filterByCondition(this.$$projectList.getValue(), field, comparedValue);
      this.updateProjectList(filteredImageList);
    }
  }

  private filterByCondition(arr: ProjectModel[], field: keyof ImageModel, comparedValue: string) {
    return arr.map(item => {
      const filteredImageList = item.images.filter(image => image[field] === comparedValue);
      return {...item, images: filteredImageList};
    });
  }

  private updateCashedProjectList(projectList: ProjectModel[]): void {
    this.$$cashedProjectList.next(projectList);
  }

  private getDropdownDataList(): void {
    const dropdownList = {
      imageName: this.getDropdownDataItem(ImageModelNames.name),
      statuses: this.getDropdownDataItem(ImageModelNames.status),
      endpoints: this.getDropdownDataItem(ImageModelNames.endpoint),
      templateNames: this.getDropdownDataItem(ImageModelNames.templateName),
      sharingStatuses: this.getDropdownDataItem(ImageModelNames.sharingStatus),
    };
    this.addFilterDropdownData(dropdownList);
    this.dropdownStartValue = dropdownList;
  }

  private getDropdownDataItem(key: keyof ImageModel): string[] {
    const dropdownItem = this.$$imageList.value.reduce((acc: Set<string>, item) => {
      if (item) {
        acc.add(<string>item[key]);
      }
      return acc;
    }, new Set<string>());
    return caseInsensitiveSortUtil([...dropdownItem]);
  }

  private addFilterDropdownData(data: ImageFilterFormDropdownData): void {
    this.$$filterDropdownData.next(data);
  }

  private getImagePageData(imagePageData: ProjectModel[]): void {
    const imageList = this.getImageList(imagePageData);
    this.updateProjectList(imagePageData);
    this.updateImageList(imageList);
    this.updateCashedProjectList(imagePageData);
    this.getDropdownDataList();
    this.$$isProjectListEmpty.next(this.isProjectListEmpty(imagePageData));
  }

  private isProjectListEmpty(imagePageData: ProjectModel[]): boolean {
    return imagePageData.every(({images}) => Boolean(!images.length));
  }

  private getImageList(imagePageData: ProjectModel[]): ImageModel[] {
    return imagePageData.reduce((acc: ImageModel[], {images}) => [...acc, ...images], []);
  }
}
