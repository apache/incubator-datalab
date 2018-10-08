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

import { Component, OnInit, ViewChild, Output, EventEmitter, ViewEncapsulation, Inject } from '@angular/core';
import { ValidatorFn, FormControl, AbstractControl } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { DICTIONARY } from '../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-manage-roles-groups',
  templateUrl: './manage-roles-groups.component.html',
  styleUrls: ['./manage-roles-groups.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ManageRolesGroupsComponent {
  readonly DICTIONARY = DICTIONARY;

  public groupsData: Array<any> = [];

  public roles: Array<any> = [];
  public rolesList: Array<string> = [];
  public setupGroup: string = '';
  public setupUser: string = '';
  public manageUser: string = '';
  public setupRoles: Array<string> = [];
  public updatedRoles: Array<string> = [];
  public delimitersRegex = /[-_]?/g;
  public groupnamePattern = new RegExp(/^[a-zA-Z_\-]+$/);

  @ViewChild('bindDialog') bindDialog;
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
      this.setupRoles = $event.model
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
          users: this.setupUser.split(',').map(item => item.trim()),
          roleIds: this.extractIds(this.roles, this.setupRoles)
        }
      });
      this.stepperView = false;
    } else if (action === 'delete') {
      let data = (type === 'users') ? {group: item.group, user: value} : {group: item.group, id: item};
      const dialogRef: MatDialogRef<ConfirmDeleteUserAccountDialog> = this.dialog.open(ConfirmDeleteUserAccountDialog, { data: data, width: '550px' });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          let emitValue = (type === 'users') ? {action, type, id: item.name, value: { user: value, group: item.group }} : {action, type, id: item.name, value: item.group} ;
          this.manageRolesGroupAction.emit(emitValue);
        }
      });
    } else if (action === 'update') {
      let source = (type === 'roles')
          ? { group: item.group, roleIds: this.extractIds(this.roles, item.selected_roles) }
          : { group: item.group, users: value.split(',').map(item => item.trim())
      }
      this.manageRolesGroupAction.emit({action, type, value: source});
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

    let duplicateList = this.groupsData.map(item => item.group)
    return <ValidatorFn>((control:FormControl) => {
      if (control.value && duplicateList.includes(this.delimitersFiltering(control.value)))
        return { duplicate: true };

      if (control.value && !this.groupnamePattern.test(control.value))
        return { patterns: true };

      return null;
    });
  }

  compareObjects(o1: any, o2: any): boolean {
    return o1.toLowerCase() === o2.toLowerCase();
  }

  public delimitersFiltering(resource): string {
    return resource.replace(this.delimitersRegex, '').toString().toLowerCase();
  }

  public resetDialog() {
    this.setupGroup = '';
    this.setupUser = '';
    this.manageUser = '';
    this.setupRoles = [];
    this.updatedRoles = [];
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
export class ConfirmDeleteUserAccountDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteUserAccountDialog>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}