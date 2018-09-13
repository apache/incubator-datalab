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
import { MaterialModule } from '../shared/material.module';
import { NavbarModule, ModalModule, UploadKeyDialogModule, ProgressDialogModule, BubbleModule, ConfirmationDialogModule } from '../shared';
import { HealthStatusComponent } from './health-status.component';
import { BackupDilogComponent } from './backup-dilog/backup-dilog.component';
import {
  ManageEnvironmentComponent,
  ConfirmActionDialog
} from './manage-environment/manage-environment-dilog.component';

import { HealthStatusGridComponent } from './health-status-grid/health-status-grid.component';
import { SsnMonitorComponent } from './ssn-monitor/ssn-monitor.component';
import { ToastModule } from 'ng2-toastr';

export * from './environment-status.model';

@NgModule({

  imports: [
    CommonModule,
    NavbarModule,
    ModalModule,
    UploadKeyDialogModule,
    ProgressDialogModule,
    BubbleModule,
    ConfirmationDialogModule,
    MaterialModule,
    ToastModule.forRoot()
  ],
  declarations: [
    HealthStatusComponent,
    BackupDilogComponent,
    ManageEnvironmentComponent,
    ConfirmActionDialog,
    HealthStatusGridComponent,
    SsnMonitorComponent
  ],
  entryComponents: [ConfirmActionDialog],
  exports: [HealthStatusComponent, HealthStatusGridComponent]
})
export class HealthStatusModule {}
