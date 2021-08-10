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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../../shared/material.module';
import { ResourcesGridComponent } from './resources-grid.component';
import { ComputationalResourcesModule } from '../computational/computational-resources-list';
import { ConfirmationDialogModule, BubbleModule } from '../../shared';
import { ComputationalResourceCreateDialogModule } from '../computational/computational-resource-create-dialog';
import { DetailDialogModule } from '../exploratory/detail-dialog';
import { FormControlsModule } from '../../shared/form-controls';
import { CostDetailsDialogModule } from '../exploratory/cost-details-dialog';
import { InstallLibrariesModule } from '../exploratory/install-libraries';
import { AmiCreateDialogModule } from '../exploratory/ami-create-dialog';
import { SchedulerModule } from '../scheduler';
import { UnderscorelessPipeModule } from '../../core/pipes/underscoreless-pipe';
import { LocalCurrencyModule } from '../../core/pipes/local-currency-pipe';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    ComputationalResourcesModule,
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
    MaterialModule,
    LocalCurrencyModule
  ],
  declarations: [ResourcesGridComponent],
  exports: [ResourcesGridComponent]
})
export class ResourcesGridModule { }
