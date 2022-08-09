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

export interface ShareImageAllUsersParams {
  imageName: string;
  projectName: string;
  endpoint: string;
}

export interface ShareModalData {
  image: ImageModel;
}

export interface Library {
  add_pkgs: string[];
  available_versions: string[];
  error_message: string;
  group: string;
  name: string;
  status: string;
  version: string;
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
