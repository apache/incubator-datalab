import { Injectable } from '@angular/core';
import { tap } from 'rxjs/operators';
import { BehaviorSubject, Observable } from 'rxjs';

import { FilterDropdownValue, ImageModel, ProjectModel, ShareImageAllUsersParams } from './images.model';
import { ApplicationServiceFacade, UserImagesPageService } from '../../core/services';
import {ImageModelNames} from './images.config';

@Injectable({
  providedIn: 'root'
})
export class ImagesService {
  private $$projectList: BehaviorSubject<ProjectModel[]> = new BehaviorSubject<ProjectModel[]>([]);
  private $$imageList: BehaviorSubject<ImageModel[]> = new BehaviorSubject<ImageModel[]>([]);
  private $$isFilterOpened: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  private $$filterDropdownData: BehaviorSubject<FilterDropdownValue> = new BehaviorSubject<FilterDropdownValue>({} as FilterDropdownValue);
  private dropdownStartValue: FilterDropdownValue;

  $imageList = this.$$imageList.asObservable();
  $isFilterOpened = this.$$isFilterOpened.asObservable();
  $filterDropdownData = this.$$filterDropdownData.asObservable();

  constructor(
    private applicationServiceFacade: ApplicationServiceFacade,
    private userImagesPageService: UserImagesPageService
  ) { }

  getUserImagePageInfo(): Observable<ProjectModel[]> {
    return this.userImagesPageService.getUserImagePageInfo()
      .pipe(
        tap(value => this.getImagePageData(value))
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
        tap(value => this.getImagePageData(value))
      );
  }

  getActiveProject(projectName: string): void {
    const currentProject = this.$$projectList.value.find(({project}) => project === projectName);
    this.updateImageList(currentProject.images);
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

  filterDropdownField(field: keyof FilterDropdownValue, value: string, ) {
    const filteredDropdownList = this.dropdownStartValue[field].filter(item => item.toLowerCase().includes(value));
    this.addFilterDropdownData({...this.$$filterDropdownData.value, imageName: filteredDropdownList});
  }

  private getDropdownDataList(): void {
    const dropdownList = {
      imageName: this.getDropdownDataItem(ImageModelNames.name),
      imageStatuses: this.getDropdownDataItem(ImageModelNames.status),
      endpoints: this.getDropdownDataItem(ImageModelNames.endpoint),
      templateNames: this.getDropdownDataItem(ImageModelNames.templateName),
      sharingStatuses: this.getDropdownDataItem(ImageModelNames.shared),
    };

    this.addFilterDropdownData(dropdownList);
    this.dropdownStartValue = dropdownList;
  }

  private getDropdownDataItem(key: keyof ImageModel): string[] {
    const dropdownItem = this.$$imageList.value.reduce((acc: Set<string>, item) => {
      acc.add(item[key].toString());
      return acc;
    }, new Set<string>());
    return [...dropdownItem];
  }

  private addFilterDropdownData(data: FilterDropdownValue): void {
    this.$$filterDropdownData.next(data);
  }

  private getImagePageData(imagePageData: ProjectModel[]): void {
    const imageList = imagePageData.reduce((acc: ImageModel[], {images}) => [...acc, ...images], []);
    this.updateProjectList(imagePageData);
    this.updateImageList(imageList);
    this.getDropdownDataList();
  }
}
