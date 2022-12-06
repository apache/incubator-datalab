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

import { UserData } from '../exploratory/image-action-dialog/image-action.model';
import { ModalTitle } from './images.config';

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
  sharedWith: SharedWithField;
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
  sharedWith?: UserData[];
}

export interface ImageActionModalData {
  actionType: ImageActionType;
  title: string;
  image: ImageModel;
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

export interface UnShareModal {
  userData: UserData;
  title: ModalTitle;
}

export interface SharedWithField {
  users: string[];
  groups: string[];
}
