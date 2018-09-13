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

import { MaterialModule } from '../shared/material.module';
import { ResourcesComponent } from './resources.component';
import { ResourcesGridModule } from './resources-grid';
import { ExploratoryEnvironmentCreateDialogModule } from './exploratory/exploratory-environment-create-dialog';
import { ManageUngitComponent } from './manage-ungit/manage-ungit.component';
import { ConfirmDeleteAccountDialog } from './manage-ungit/manage-ungit.component';
import {
  NavbarModule,
  ModalModule,
  ProgressDialogModule,
  UploadKeyDialogModule
} from '../shared';

@NgModule({
  imports: [
    CommonModule,
    ModalModule,
    FormsModule,
    ReactiveFormsModule,
    ResourcesGridModule,
    ProgressDialogModule,
    UploadKeyDialogModule,
    ExploratoryEnvironmentCreateDialogModule,
    NavbarModule,
    MaterialModule
  ],
  declarations: [
    ResourcesComponent,
    ManageUngitComponent,
    ConfirmDeleteAccountDialog
  ],
  entryComponents: [ConfirmDeleteAccountDialog],
  exports: [ResourcesComponent]
})
export class ResourcesModule {}
