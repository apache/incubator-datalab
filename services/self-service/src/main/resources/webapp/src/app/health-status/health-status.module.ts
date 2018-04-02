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
import { MaterialModule } from './../shared/material.module';

import { NavbarModule, ModalModule } from './../shared';
import { HealthStatusComponent } from './health-status.component';
import { HealthStatusGridModule } from './health-status-grid/health-status-grid.module';
import { BackupDilogComponent } from './backup-dilog/backup-dilog.component';
import {
  ManageEnvironmentComponent,
  ConfirmActionDialog
} from './manage-environment/manage-environment-dilog.component';

@NgModule({
  imports: [
    CommonModule,
    NavbarModule,
    ModalModule,
    HealthStatusGridModule,
    MaterialModule
  ],
  declarations: [
    HealthStatusComponent,
    BackupDilogComponent,
    ManageEnvironmentComponent,
    ConfirmActionDialog
  ],
  entryComponents: [ConfirmActionDialog],
  exports: [HealthStatusComponent]
})
export class HealthStatusModule {}
