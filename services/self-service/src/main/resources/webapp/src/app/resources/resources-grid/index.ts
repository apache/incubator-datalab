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

import { MaterialModule } from './../../shared/material.module';
import { ResourcesGridComponent } from './resources-grid.component';
import { ComputationalResourcesModule } from './../computational/computational-resources-list';
import { ModalModule, ConfirmationDialogModule, BubbleModule } from './../../shared';
import { ComputationalResourceCreateDialogModule } from './../computational/computational-resource-create-dialog';
import { DetailDialogModule } from './../exploratory/detail-dialog';
import { FormControlsModule } from '../../shared/form-controls';
import { CostDetailsDialogModule } from './../billing/cost-details-dialog';
import { InstallLibrariesModule } from './../exploratory/install-libraries';
import { AmiCreateDialogModule } from './../exploratory/ami-create-dialog';
import { SchedulerModule } from './../scheduler';
import { UnderscorelessPipeModule } from '../../core/pipes/underscoreless-pipe';

export * from './resources-grid.component';
export * from './resources-grid.model';
export * from './create-resource.model';
export * from './filter-configuration.model';

@NgModule({
  imports: [
    CommonModule,
    ComputationalResourcesModule,
    ModalModule,
    ConfirmationDialogModule,
    BubbleModule,
    DetailDialogModule,
    ComputationalResourceCreateDialogModule,
    FormControlsModule,
    CostDetailsDialogModule,
    InstallLibrariesModule,
    SchedulerModule,
    AmiCreateDialogModule,
    UnderscorelessPipeModule,
    MaterialModule
  ],
  declarations: [ResourcesGridComponent],
  exports: [ResourcesGridComponent]
})
export class ResourcesGridModule {}
