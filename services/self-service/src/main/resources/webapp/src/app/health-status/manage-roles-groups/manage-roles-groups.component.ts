/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

import { Component, OnInit, ViewChild, Output, EventEmitter, Inject } from '@angular/core';
import { ValidatorFn, FormControl } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA, MatChipInputEvent } from '@angular/material';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import { DICTIONARY } from '../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-manage-roles-groups',
  templateUrl: './manage-roles-groups.component.html',
  styleUrls: ['../../resources/resources-grid/resources-grid.component.css', './manage-roles-groups.component.scss']
})
export class ManageRolesGroupsComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  public groupsData: Array<any> = [];

  public roles: Array<any> = [];
  public rolesList: Array<string> = [];
  public setupGroup: string = '';
  public setupUser: string = '';
  public manageUser: string = '';
  public setupRoles: Array<string> = [];
  public updatedRoles: Array<string> = [];
  public delimitersRegex = /[-_]?/g;
  public groupnamePattern = new RegExp(/^[a-zA-Z0-9_\-]+$/);

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('user') user_enter;
  @Output() manageRolesGroupAction: EventEmitter<{}> = new EventEmitter();
  stepperView: boolean = false;

  constructor(public dialog: MatDialog) { }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  public open(param, groups, roles): void {
    this.roles = roles;
    this.rolesList = roles.map(role => role.description);
    this.updateGroupData(groups);

    this.stepperView = false;
    this.bindDialog.open(param);
  }

  public onUpdate($event) {
    if ($event.type === 'role') {
      this.setupRoles = $event.model;
    } else {
      this.updatedRoles = $event.model;
    }
    $event.$event.preventDefault();
  }

  public manageAction(action: string, type: string, item?: any, value?) {
    if (action === 'create') {
      this.manageRolesGroupAction.emit(
        { action, type, value: {
          name: this.setupGroup,
          users: this.setupUser ? this.setupUser.split(',').map(elem => elem.trim()) : [],
          roleIds: this.extractIds(this.roles, this.setupRoles)
        }
      });
      this.stepperView = false;
    } else if (action === 'delete') {
      const data = (type === 'users') ? {group: item.group, user: value} : {group: item.group, id: item};
      const dialogRef: MatDialogRef<ConfirmDeleteUserAccountDialogComponent> = this.dialog.open(
        ConfirmDeleteUserAccountDialogComponent,
        { data: data, width: '550px' }
      );

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          const emitValue = (type === 'users')
            ? {action, type, id: item.name, value: { user: value, group: item.group }}
            : {action, type, id: item.name, value: item.group} ;
          this.manageRolesGroupAction.emit(emitValue);
        }
      });
    } else if (action === 'update') {
      // const source = (type === 'roles')
      //     ? { group: item.group, roleIds: this.extractIds(this.roles, item.selected_roles) }
      //     : { group: item.group, users: value.split(',').map(elem => elem.trim())
      // };
      this.manageRolesGroupAction.emit({action, type, value: {
        name: item.group,
        roleIds: this.extractIds(this.roles, item.selected_roles),
        users: item.users }});
    }
    this.resetDialog();
  }

  public extractIds(sourceList, target) {
    return sourceList.reduce((acc, item) => {
      target.includes(item.description) && acc.push(item._id);
      return acc;
    }, []);
  }

  public updateGroupData(groups) {
    this.groupsData = groups;

    this.groupsData.forEach(item => {
      item.selected_roles = item.roles.map(role => role.description);
    });
  }

  public groupValidarion(): ValidatorFn {

    const duplicateList = this.groupsData.map(item => item.group);
    return <ValidatorFn>((control: FormControl) => {
      if (control.value && duplicateList.includes(this.delimitersFiltering(control.value)))
        return { duplicate: true };

      if (control.value && !this.groupnamePattern.test(control.value))
        return { patterns: true };

      return null;
    });
  }

  public compareObjects(o1: any, o2: any): boolean {
    return o1.toLowerCase() === o2.toLowerCase();
  }

  public delimitersFiltering(resource): string {
    return resource.replace(this.delimitersRegex, '').toString().toLowerCase();
  }

  public resetDialog() {
    this.stepperView = false;
    this.setupGroup = '';
    this.setupUser = '';
    this.manageUser = '';
    this.setupRoles = [];
    this.updatedRoles = [];
  }

  public removeUser(list, item): void {
    list.splice(list.indexOf(item), 1);
    console.log("Removing", list)
  }

  public addUser(value: string, item): void {
    if (value && value.trim()) {
      item.users instanceof Array ? item.users.push(value.trim()) : item.users = [value.trim()];
      console.log("Adding");

      debugger;
      this.user_enter.nativeElement.value = '';
    }
  }
}


@Component({
  selector: 'dialog-result-example-dialog',
  template: `
  <div mat-dialog-content class="content">
    <p *ngIf="data.user">User <strong>{{ data.user }}</strong> will be deleted from <strong>{{ data.group }}</strong> group.</p>
    <p *ngIf="data.id">Group <strong>{{ data.group }}</strong> will be decommissioned.</p>
    <p class="m-top-20"><strong>Do you want to proceed?</strong></p>
  </div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400 }
  `]
})
export class ConfirmDeleteUserAccountDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteUserAccountDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
