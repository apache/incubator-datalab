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

import { Component, OnInit, Output, EventEmitter, Inject } from '@angular/core';
import { ValidatorFn, FormControl } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import {RolesGroupsService, HealthStatusService, ApplicationSecurityService, AppRoutingService} from '../../core/services';
import { CheckUtils } from '../../core/util';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import {ProgressBarService} from '../../core/services/progress-bar.service';
import {ConfirmationDialogComponent, ConfirmationDialogType} from '../../shared/modal-dialog/confirmation-dialog';

@Component({
  selector: 'dlab-roles',
  templateUrl: './roles.component.html',
  styleUrls: ['../../resources/resources-grid/resources-grid.component.scss', './roles.component.scss']
})
export class RolesComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  public groupsData: Array<any> = [];
  public roles: Array<any> = [];
  public rolesList: Array<any> = [];
  public setupGroup: string = '';
  public setupUser: string = '';
  public manageUser: string = '';
  public setupRoles: Array<any> = [];
  public updatedRoles: Array<string> = [];
  public healthStatus: any;
  public delimitersRegex = /[-_]?/g;
  public groupnamePattern = new RegExp(/^[a-zA-Z0-9_\-]+$/);

  stepperView: boolean = false;
  displayedColumns: string[] = ['name', 'roles', 'users', 'actions'];
  @Output() manageRolesGroupAction: EventEmitter<{}> = new EventEmitter();
  private startedGroups: Array<any>;

  constructor(
    public toastr: ToastrService,
    public dialog: MatDialog,
    private rolesService: RolesGroupsService,
    private healthStatusService: HealthStatusService,
    private progressBarService: ProgressBarService,
    private applicationSecurityService: ApplicationSecurityService,
    private appRoutingService: AppRoutingService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
  }

  openManageRolesDialog() {
    setTimeout(() => {this.progressBarService.startProgressBar(); } , 0);
    this.rolesService.getGroupsData().subscribe(groups => {
      this.rolesService.getRolesData().subscribe(
        (roles: any) => {
          this.roles = roles;
          this.rolesList = roles.map((role) => {
              return {role: role.description, type: role.type, cloud: role.cloud};
          });
          this.rolesList = this.rolesList.sort((a, b) => (a.cloud > b.cloud) ? 1 : ((b.cloud > a.cloud) ? -1 : 0));
          this.rolesList = this.rolesList.sort((a, b) => (a.type > b.type) ? 1 : ((b.type > a.type) ? -1 : 0));
          this.updateGroupData(groups);
          this.stepperView = false;
        },
        error => this.toastr.error(error.message, 'Oops!'));
        this.progressBarService.stopProgressBar();
      },
      error => {
      this.toastr.error(error.message, 'Oops!');
      this.progressBarService.stopProgressBar();
    });
  }

  getGroupsData() {
    this.rolesService.getGroupsData().subscribe(
      list => this.updateGroupData(list),
      error => this.toastr.error(error.message, 'Oops!'));
  }

  public selectAllOptions(item, values, byKey?) {
    byKey ? (item[byKey] = values ? values : []) : this.setupRoles = values ? values : [];
  }

  public manageAction(action: string, type: string, item?: any, value?) {
    if (action === 'create') {
      this.manageRolesGroups(
        {
          action, type, value: {
            name: this.setupGroup,
            users: this.setupUser ? this.setupUser.split(',').map(elem => elem.trim()) : [],
            roleIds: this.extractIds(this.roles, this.setupRoles.map(v => v.role))
          }
        });
      this.stepperView = false;
    } else if (action === 'delete') {
      const data = (type === 'users') ? { group: item.group, user: value } : { group: item.group, id: item };
      const dialogRef: MatDialogRef<ConfirmDeleteUserAccountDialogComponent> = this.dialog.open(
        ConfirmDeleteUserAccountDialogComponent,
        { data: data, width: '550px', panelClass: 'error-modalbox' }
      );

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          const emitValue = (type === 'users')
            ? { action, type, id: item.name, value: { user: value, group: item.group } }
            : { action, type, id: item.name, value: item.group };
          this.manageRolesGroups(emitValue);
        }
      });
    } else if (action === 'update') {
      const currGroupSource = this.startedGroups.filter(cur => cur.group === item.group)[0];
      let deletedUsers = currGroupSource.users.filter(user => {
        return !item.users.includes(user);
      });
      this.dialog.open(ConfirmationDialogComponent, { data:
          { notebook: deletedUsers, type: ConfirmationDialogType.deleteUser }, panelClass: 'modal-sm' })
        .afterClosed().subscribe((res) => {
        if (!res) {
          item.users = [...currGroupSource.users];
          item.selected_roles = [...currGroupSource.selected_roles];
          item.roles = [...currGroupSource.roles];
        } else {
          this.manageRolesGroups({
            action, type, value: {
              name: item.group,
              roleIds: this.extractIds(this.roles, item.selected_roles.map(v => v.role)),
              users: item.users || []
            }
          });
        }
        deletedUsers = [];
      });
    }
    this.resetDialog();
  }

  public manageRolesGroups($event) {
    switch ($event.action) {
      case 'create':
        this.rolesService.setupNewGroup($event.value).subscribe(() => {
          this.toastr.success('Group creation success!', 'Created!');
          this.getGroupsData();
        }, () => this.toastr.error('Group creation failed!', 'Oops!'));
        break;

      case 'update':
        this.rolesService.updateGroup($event.value).subscribe(() => {
          this.toastr.success(`Group data is updated successfully!`, 'Success!');
          if (!$event.value.roleIds.includes('admin' || 'projectAdmin')) {
            this.applicationSecurityService.isLoggedIn().subscribe(() => {
              this.getEnvironmentHealthStatus();
            });
          } else {
            this.openManageRolesDialog();
          }
        }, (re) => this.toastr.error('Failed group data updating!', 'Oops!'));

        break;

      case 'delete':
        if ($event.type === 'users') {
          this.rolesService.removeUsersForGroup($event.value).subscribe(() => {
            this.toastr.success('Users was successfully deleted!', 'Success!');
            this.getGroupsData();
          }, () => this.toastr.error('Failed users deleting!', 'Oops!'));
        } else if ($event.type === 'group') {
          this.rolesService.removeGroupById($event.value).subscribe(() => {
            this.toastr.success('Group was successfully deleted!', 'Success!');
            this.getGroupsData();
          }, (error) => this.toastr.error(error.message, 'Oops!'));
        }
        break;

      default:
    }
  }

  public extractIds(sourceList, target) {
    return sourceList.reduce((acc, item) => {
      target.includes(item.description) && acc.push(item._id);
      return acc;
    }, []);
  }

  public updateGroupData(groups) {
    this.groupsData = groups.map(v => {
      if (!v.users) {
        v.users = [];
      }
      return v;
    }).sort((a, b) => (a.group > b.group) ? 1 : ((b.group > a.group) ? -1 : 0));
    this.groupsData.forEach(item => {
      item.selected_roles = item.roles.map(role => ({role: role.description, type: role.type, cloud: role.cloud}));
    });
    this.getGroupsListCopy();
  }

  private getGroupsListCopy() {
    this.startedGroups = JSON.parse(JSON.stringify(this.groupsData));
  }

  public groupValidarion(): ValidatorFn {
    const duplicateList: any = this.groupsData.map(item => item.group.toLowerCase());
    return <ValidatorFn>((control: FormControl) => {
      if (control.value && duplicateList.includes(CheckUtils.delimitersFiltering(control.value.toLowerCase()))) {
        return { duplicate: true };
      }

      if (control.value && !this.groupnamePattern.test(control.value))
        return { patterns: true };

      return null;
    });
  }

  private isGroupChanded(currGroup) {
    const currGroupSource = this.startedGroups.filter(cur => cur.group === currGroup.group)[0];
   if (currGroup.users.length !== currGroupSource.users.length &&
     currGroup.selected_roles.length !== currGroupSource.selected_roles.length) {
     return false;
   }
   return JSON.stringify(currGroup.users) === JSON.stringify(currGroupSource.users) &&
     JSON.stringify(
       currGroup.selected_roles.map(role => role.role).sort()
     ) === JSON
       .stringify(
         currGroupSource.selected_roles.map(role => role.role).sort()
       );
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
  }

  public addUser(value: string, item): void {
    if (value && value.trim()) {
      item.users instanceof Array ? item.users.push(value.trim()) : item.users = [value.trim()];
    }
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => {
        this.healthStatus = result;
          if (!this.healthStatus.admin && !this.healthStatus.projectAdmin) {
            this.appRoutingService.redirectToHomePage();
          } else {
            this.openManageRolesDialog();
          }
      }
      );
  }

  public onUpdate($event): void {
   if ($event.type) {
     this.groupsData.filter(group => group.group === $event.type)[0].selected_roles = $event.model;
   } else {
     this.setupRoles = $event.model;
   }
  }
}


@Component({
  selector: 'dialog-result-example-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title"><i class="material-icons">priority_high</i>Warning</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
  <div mat-dialog-content class="content">
    <p *ngIf="data.user">User <span class="strong">{{ data.user }}</span> will be deleted from <span class="strong">{{ data.group }}</span> group.</p>
    <p *ngIf="data.id">Group <span class="ellipsis group-name strong">{{ data.group }}</span> will be decommissioned.</p>
    <p class="m-top-20"><span class="strong">Do you want to proceed?</span></p>
  </div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: [`.group-name { max-width: 96%; display: inline-block; vertical-align: bottom; }`]
})

export class ConfirmDeleteUserAccountDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteUserAccountDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
