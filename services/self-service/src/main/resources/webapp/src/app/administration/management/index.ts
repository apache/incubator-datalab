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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { BubbleModule, ConfirmationDialogModule } from '../../shared';
import { MaterialModule } from '../../shared/material.module';

import { ManagementComponent } from './management.component';
import { EnvironmentsDataService } from './management-data.service';
import { ManagementGridComponent, ReconfirmationDialogComponent } from './management-grid/management-grid.component';
import { ComputationalResourcesModule } from '../../resources/computational/computational-resources-list';

import { FormControlsModule } from '../../shared/form-controls';
import { BackupDilogComponent } from './backup-dilog/backup-dilog.component';
import { ManageEnvironmentComponent, ConfirmActionDialogComponent } from './manage-environment/manage-environment-dilog.component';

import { DirectivesModule } from '../../core/directives';

import { SsnMonitorComponent } from './ssn-monitor/ssn-monitor.component';
import { EndpointsComponent } from './endpoints/endpoints.component';
import { ProjectModule } from '../project';

export * from './management.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ProjectModule,
    BubbleModule,
    ConfirmationDialogModule,
    ComputationalResourcesModule,
    FormControlsModule,
    DirectivesModule,
    MaterialModule
  ],
  declarations: [
    ManagementComponent,
    ManagementGridComponent,
    BackupDilogComponent,
    ManageEnvironmentComponent,
    ReconfirmationDialogComponent,
    ConfirmActionDialogComponent,
    SsnMonitorComponent,
    EndpointsComponent
  ],
  entryComponents: [
    ReconfirmationDialogComponent,
    ConfirmActionDialogComponent,
    BackupDilogComponent,
    SsnMonitorComponent,
    EndpointsComponent,
    ManageEnvironmentComponent],
  providers: [EnvironmentsDataService],
  exports: [ManagementComponent]
})
export class ManagenementModule { }
