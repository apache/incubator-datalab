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

import { Input, Output, Component, EventEmitter } from '@angular/core';
import { SortUtils } from '../../../core/util';

export interface Rights {
  role: string;
  type: string;
  cloud: string;
}

export enum TypeOfRights {
  admin = 'ADMINISTRATION',
  img = 'IMAGE'
}

@Component({
  selector: 'multi-level-select-dropdown',
  templateUrl: 'multi-level-select-dropdown.component.html',
  styleUrls: ['multi-level-select-dropdown.component.scss']
})

export class MultiLevelSelectDropdownComponent {

  @Input() items: Array<any>;
  @Input() model: Rights[];
  @Input() isAddGroup: boolean = false;
  @Input() type: string;
  @Input() isAdmin: boolean;
  @Output() selectionChange: EventEmitter<{}> = new EventEmitter();

  public isOpenCategory = {};
  public isCloudOpen = {};

  public labels = {
    COMPUTATIONAL_SHAPE: 'Compute shapes',
    NOTEBOOK_SHAPE: 'Notebook shapes',
    COMPUTATIONAL: 'Compute',
    BUCKET_BROWSER: 'Bucket browser actions'
  };
  public selectedList: any;

  toggleSelectedOptions( model: Rights[], value: Rights, event?): void {
    if (event) event.preventDefault();
    const currRole = model.filter(v => v.role === value.role).length;
    currRole ? this.model = this.checkUserRights(model, value) : model.push(value);
    this.onUpdate(event);
  }

  toggleSelectedCategory(model: Rights[], value: string, event?): void {
    if (event) event.preventDefault();
    const categoryItems = this.items.filter(role => role.type === value);

    this.selectedAllInCattegory(value)
      ? this.model = this.checkUserRights(value)
      : categoryItems.forEach(role => {
        if (!model.filter(mod => mod.role === role.role).length) {
          this.model.push(role);
        }
      });
    this.onUpdate(event);
  }

  toggleSelectedCloud( model, category, cloud, event?): void {
    if (event) event.preventDefault();
    const categoryItems = this.items.filter(role => role.type === category && role.cloud === cloud);
    this.selectedAllInCloud(category, cloud) ? this.model = this.model.filter(role => {
      if (role.type === category && role.cloud === cloud) {
        return false;
      }
      return true;
    }) : categoryItems.forEach(role => {
      if (!model.filter(mod => mod.role === role.role).length) {this.model.push(role); }
    });
    this.onUpdate(event);
  }

  selectAllOptions($event): void {
    $event.preventDefault();
    this.model = [...this.items];
    this.onUpdate($event);
    $event.preventDefault();
  }

  deselectAllOptions($event): void {
    this.model = [];
    this.onUpdate($event);
    $event.preventDefault();
  }

  onUpdate($event): void {
    this.selectedList = SortUtils.sortByKeys(this.getSelectedRolesList(), ['type']);
    this.selectionChange.emit({ model: this.model, type: this.type, $event });
  }

  public toggleItemsForLable(label, $event): void {
    this.isOpenCategory[label] = !this.isOpenCategory[label];
    this.isCloudOpen[label + 'AWS'] = false;
    this.isCloudOpen[label + 'GCP'] = false;
    this.isCloudOpen[label + 'AZURE'] = false;
    $event.preventDefault();
  }

  public toggleItemsForCloud(label, $event): void {
    this.isCloudOpen[label] = !this.isCloudOpen[label];
    $event.preventDefault();
  }

  public selectedAllInCattegory(category): boolean {
    const selected = this.model.filter(role => role.type === category);
    const categoryItems = this.items.filter(role => role.type === category);
    return selected.length === categoryItems.length;
  }

  public selectedSomeInCattegory(category): boolean {
    const selected = this.model.filter(role => role.type === category);
    const categoryItems = this.items.filter(role => role.type === category);
    return selected.length && selected.length !== categoryItems.length;
  }

  public selectedAllInCloud(category, cloud): boolean {
    const selected = this.model.filter(role => role.type === category && role.cloud === cloud);
    const categoryItems = this.items.filter(role => role.type === category && role.cloud === cloud);
    return selected.length === categoryItems.length;
  }

  public selectedSomeInCloud(category, cloud): boolean {
    const selected = this.model.filter(role => role.type === category && role.cloud === cloud);
    const categoryItems = this.items.filter(role => role.type === category && role.cloud === cloud);
    return selected.length && selected.length !== categoryItems.length;
  }

  public checkInModel(item): boolean {
    return !!this.model.filter(v => v.role === item).length;
  }

  public getSelectedRolesList() {
    return this.model.map(role => role.role);
  }

  private checkUserRights(value: string): Rights[];
  private checkUserRights(model: Rights[], value: Rights): Rights[];
  private checkUserRights(...args: any[]): Rights[] {
    if (args.length === 1) {
      const [ value ] = args;

      const filteredModelList = this.model.filter(role => role.type !== value);
      const isNeedUncheckImage = value === TypeOfRights.admin && this.isAddGroup;
      return isNeedUncheckImage ? filteredModelList.filter(role => role.type !== TypeOfRights.img) : filteredModelList;
    } else {
      const [model, value] = args;

      const filteredModelList = model.filter(v => v.role !== value.role);
      const isNeedUncheckImage = this.isAddGroup && value.type === TypeOfRights.admin;
      return isNeedUncheckImage ? filteredModelList.filter(role => role.type !== TypeOfRights.img) : filteredModelList;
    }
  }
}
