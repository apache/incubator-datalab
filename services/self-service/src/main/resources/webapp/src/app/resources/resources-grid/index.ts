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
import { ResourcesGrid } from './resources-grid.component';
import { ComputationalResourcesModule } from './../computational/computational-resources-list/index';
import { ModalModule } from './../../shared/modal-dialog/index';
import { ComputationalResourceCreateDialogModule } from './../computational/computational-resource-create-dialog/index';
import { ConfirmationDialogModule } from './../../shared/modal-dialog/confirmation-dialog/index';
import { DetailDialogModule } from './../exploratory/detail-dialog/index';
import { FormControlsModule } from '../../shared/form-controls';
import { MaterialModule } from '@angular/material';

export * from './resources-grid.component';

@NgModule({
  imports: [
    CommonModule,
    ComputationalResourcesModule,
    ModalModule,
    ConfirmationDialogModule,
    DetailDialogModule,
    ComputationalResourceCreateDialogModule,
    FormControlsModule,
    MaterialModule.forRoot()
  ],
  declarations: [ResourcesGrid],
  exports: [ResourcesGrid]
})

export class ResourcesGridModule { }
