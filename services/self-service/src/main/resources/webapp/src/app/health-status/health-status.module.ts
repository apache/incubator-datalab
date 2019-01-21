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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ToastModule } from 'ng2-toastr';

import { MaterialModule } from '../shared/material.module';
import { ModalModule, UploadKeyDialogModule, ProgressDialogModule, BubbleModule, ConfirmationDialogModule } from '../shared';
import { FormControlsModule } from '../shared/form-controls';
import { HealthStatusComponent } from './health-status.component';
import { BackupDilogComponent } from './backup-dilog/backup-dilog.component';
import {
  ManageEnvironmentComponent,
  ConfirmActionDialogComponent
} from './manage-environment/manage-environment-dilog.component';

import { GroupNameValidationDirective } from './manage-roles-groups/group-name-validarion.directive';
import { DirectivesModule } from '../core/directives';

import { HealthStatusGridComponent } from './health-status-grid/health-status-grid.component';
import { SsnMonitorComponent } from './ssn-monitor/ssn-monitor.component';
import { ManageRolesGroupsComponent, ConfirmDeleteUserAccountDialogComponent } from './manage-roles-groups/manage-roles-groups.component';

export * from './environment-status.model';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ModalModule,
    UploadKeyDialogModule,
    ProgressDialogModule,
    BubbleModule,
    ConfirmationDialogModule,
    FormControlsModule,
    MaterialModule,
    DirectivesModule,
    ToastModule.forRoot()
  ],
  declarations: [
    GroupNameValidationDirective,
    HealthStatusComponent,
    BackupDilogComponent,
    ManageEnvironmentComponent,
    ConfirmActionDialogComponent,
    ConfirmDeleteUserAccountDialogComponent,
    SsnMonitorComponent,
    ManageRolesGroupsComponent,
    HealthStatusGridComponent
  ],
  entryComponents: [ConfirmActionDialogComponent, ConfirmDeleteUserAccountDialogComponent],
  exports: [HealthStatusComponent, HealthStatusGridComponent, GroupNameValidationDirective]
})
export class HealthStatusModule {}
