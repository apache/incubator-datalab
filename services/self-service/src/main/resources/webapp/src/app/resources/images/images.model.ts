import { ImageActions } from './images.config';

export interface ProjectImagesInfo {
  filterData: ImageFilterFormDropdownData;
  imageFilter: ImageFilterFormValue;
  projectImagesInfos: ProjectModel[];
}

export interface ProjectModel {
  project: string;
  images: ImageModel[];
}

export interface ImageModel {
  application: string;
  cloudProvider: 'AWS' | 'GCP' | 'Azure';
  timestamp: string;
  description: string;
  endpoint: string;
  fullName: string;
  instanceName: string;
  imageUserPermissions: ImageUserPermissions;
  name: string;
  project: string;
  sharingStatus: 'SHARED'| 'PRIVATE' | 'RECEIVED';
  status: 'ACTIVE' | 'CREATING' | 'FAILED';
  user: string;
  isSelected?: boolean;
  libraries: Library[];
  computationalLibraries: Library[];
  clusterConfig: ClusterConfig;
  templateName: string;
}

export interface ImageUserPermissions {
  canShare: boolean;
  canTerminate: boolean;
}

export interface ImageParams {
  imageName: string;
  projectName: string;
  endpoint: string;
}

export interface ImageActionModalData {
  actionType: ImageActionType;
  title: string;
  imageName: string;
  isShared?: boolean;
}

export interface ImageDetailModalData {
  image: ImageModel;
}

export type ImageActionType = 'share' | 'terminate';

export interface Library {
  add_pkgs: string[];
  available_versions: string[];
  error_message: string;
  group: string;
  name: string;
  status: string;
  version: string;
  resourceName: string;
  type: 'EXPLORATORY';
}

export interface ClusterConfig {
  Classification: string;
  Properties: Record<string, any>;
  Configurations: any[];
}

export interface ImageFilterFormDropdownData {
  imageNames: string[];
  statuses: string[];
  endpoints: string[];
  templateNames: string[];
  sharingStatuses: string[];
}

export interface ImageFilterFormValue {
  endpoints: string[];
  imageName: string;
  statuses: string[];
  templateNames: string[];
  sharingStatuses: string[];
}


export interface LibraryInfoItem {
  name: string;
  libs: string[];
}

export interface FilteredColumnList {
  imageNames: boolean;
  statuses: boolean;
  endpoints: boolean;
  templateNames: boolean;
  sharingStatuses: boolean;
}

export type FilterFormItemType = [string, string[] | string];
