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

export enum Image_Table_Column_Headers {
  imageName = 'Image name',
  creationDate = 'Creation date',
  provider = 'Provider',
  imageStatus = 'Status',
  sharedStatus = 'Shared status',
  templateName = 'Template name',
  actions = 'Actions',
  endpoint = 'Endpoint',
}

export enum SharingStatus {
  shared = 'SHARED',
  private = 'PRIVATE',
  received = 'RECEIVED'
}

export const Image_Table_Titles = <const>[
  'checkbox',
  'imageName',
  'imageStatus',
  'creationDate',
  'endpoint',
  'templateName',
  'sharedStatus',
  'actions'
];

export enum Localstorage_Key {
  userName = 'user_name'
}

export enum Toaster_Message {
  successTitle = 'Success!',
  successShare = 'The image has been successfully shared.',
  successTerminate = 'The image has been terminated',
  successUnShare = 'The action has been performed successfully.'
}

export enum Placeholders {
  projectSelect = 'Select project'
}

export enum ImageStatuses {
  creating = 'CREATING',
  active = 'ACTIVE',
  failed = 'FAILED'
}

export enum TooltipStatuses {
  activeOnly = 'The image cannot be shared because it is not in the "Active" status',
  creatorOnly = 'Images may be shared by creators only',
  unableTerminate = 'Unable to terminate notebook because at least one compute is in progress'
}

export enum DropdownFieldNames {
  imageNames = 'imageNames',
  endpoints = 'endpoints',
  templateNames = 'templateNames',
  statuses = 'statuses',
  sharingStatuses = 'sharingStatuses'
}

export enum FilterFormControlNames {
  imageName = 'imageName',
  endpoints = 'endpoints',
  templateNames = 'templateNames',
  statuses = 'statuses',
  sharingStatuses = 'sharingStatuses'
}

export const FilterFormInitialValue = {
    endpoints: [],
    imageName: '',
    statuses: [],
    templateNames: [],
    sharingStatuses: [],
};

export const ChangedColumnStartValue = {
    endpoints: false,
    imageNames: false,
    statuses: false,
    templateNames: false,
    sharingStatuses: false,
};

export enum ImageModelKeysForFilter {
  status = 'status',
  name = 'name',
  endpoint = 'endpoint',
  templateName = 'templateName',
  shared = 'shared'
}

export const DropdownSelectAllValue = 'selectAllFound';

export enum ImageActions {
  share = 'share',
  terminate = 'terminate'
}

export enum ModalTitle {
  share = 'Share image',
  terminate = 'Terminate image',
  unShare = '! Warning',
  addPlatform = 'Add platform'
}

export enum URL_Chunk {
  sharingInfo = 'sharing_info',
  autocomplete = 'share_autocomplete'
}
