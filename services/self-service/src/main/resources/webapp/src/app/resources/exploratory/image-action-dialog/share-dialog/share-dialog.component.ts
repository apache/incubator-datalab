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

import { ChangeDetectionStrategy, Component, ViewChild } from '@angular/core';
import { SharePlaceholder, TabName, UserDataTypeConfig } from '../image-action.config';
import { DialogWindowTabConfig, UserData, UserDataType } from '../image-action.model';
import { NgModel } from '@angular/forms';

@Component({
  selector: 'datalab-share-dialog',
  templateUrl: './share-dialog.component.html',
  styleUrls: ['./share-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShareDialogComponent {
  @ViewChild('searchUser') searchUser: NgModel;

  readonly placeholder: typeof SharePlaceholder = SharePlaceholder;
  readonly tabsName: typeof TabName = TabName;
  readonly userDataTypeConfig: typeof UserDataTypeConfig = UserDataTypeConfig;

  userList: UserData[] = [];
  temporaryUserList: UserData[] = [];
  userNameOrGroup: UserDataType;
  activeTabConfig: DialogWindowTabConfig = {
    shareImage: true,
    shareWith: false
  };
  searchInput = '';

  onAddUser(): void {
    if (!this.searchInput) {
      return;
    }
    const newUserEntity: UserData = {
      value: this.searchInput.trim(),
      type: this.userNameOrGroup
    };
    this.temporaryUserList = [...this.temporaryUserList, newUserEntity];
    this.searchInput = '';
  }

  onTabTitle(tabName: keyof DialogWindowTabConfig): void {
    Object.keys(this.activeTabConfig).forEach(item => this.activeTabConfig[item] = false);
    this.activeTabConfig = {...this.activeTabConfig, [tabName]: true};
  }

  onRemoveUserData(userName: string, tabName: TabName): void {
    this.temporaryUserList = this.temporaryUserList.filter(({value}) => value !== userName);
  }

  get isApplyBtnDisabled(): boolean {
    return this.searchInput.length > 25 || !Boolean(this.temporaryUserList.length);
  }

  get isAddUserDataBtnDisabled(): boolean {
    return !Boolean(this.userNameOrGroup) || this.searchInput.length > 25 || !Boolean(this.searchInput.length);
  }

  get isSearchUserTouched(): boolean {
    return this.searchUser?.control.touched && !Boolean(this.userNameOrGroup);
  }

  get isUserInputEmpty(): boolean {
    return this.searchUser?.control.touched
      && !Boolean(this.searchInput.length)
      && !Boolean(this.temporaryUserList.length);
  }

  get isLongInputMessage() {
    return this.searchInput.length > 25;
  }
}
