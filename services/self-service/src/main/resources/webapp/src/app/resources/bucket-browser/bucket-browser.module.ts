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

import { MaterialModule } from '../../shared/material.module';
import { ResourcesGridModule } from '../resources-grid';
import { ExploratoryEnvironmentCreateModule } from '../exploratory/create-environment';

import { BucketBrowserComponent } from './bucket-browser.component';
import { FolderTreeComponent } from './folder-tree/folder-tree.component';
import { MatTreeModule } from '@angular/material/tree';
import { BucketDataService } from './bucket-data.service';
import { BucketConfirmationDialogComponent } from './bucket-confirmation-dialog/bucket-confirmation-dialog.component';
import { BucketTreeComponent } from './buckets-tree/bucket-tree.component';
import { ConvertFileSizePipeModule } from '../../core/pipes/convert-file-size';
import { LocalDatePipeModule } from '../../core/pipes/local-date-pipe';
import { ConvertActionPipeModule } from '../../core/pipes/convert-action-pipe';
import { CheckboxModule } from '../../shared/checkbox';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ResourcesGridModule,
    ExploratoryEnvironmentCreateModule,
    MaterialModule,
    MatTreeModule,
    ConvertFileSizePipeModule,
    LocalDatePipeModule,
    ConvertActionPipeModule,
    CheckboxModule
  ],
  declarations: [
    BucketBrowserComponent,
    FolderTreeComponent,
    BucketTreeComponent,
    BucketConfirmationDialogComponent
  ],
  entryComponents: [BucketBrowserComponent, FolderTreeComponent, BucketTreeComponent, BucketConfirmationDialogComponent],
  providers: [BucketDataService],
})
export class BucketBrowserModule { }
