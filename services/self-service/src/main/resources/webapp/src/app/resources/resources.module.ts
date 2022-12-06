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

import { MaterialModule } from '../shared/material.module';
import { ResourcesComponent } from './resources.component';
import { ResourcesGridModule } from './resources-grid';
import { ExploratoryEnvironmentCreateModule } from './exploratory/create-environment';
import { ManageUngitComponent } from './manage-ungit/manage-ungit.component';
import { ConfirmDeleteAccountDialogComponent } from './manage-ungit/manage-ungit.component';
import { MatTreeModule } from '@angular/material/tree';
import { BucketDataService } from './bucket-browser/bucket-data.service';
import { ConvertFileSizePipeModule } from '../core/pipes/convert-file-size';
import { BucketBrowserModule } from './bucket-browser/bucket-browser.module';
import { ImagesComponent } from './images/images.component';
import { CheckboxModule } from '../shared/checkbox';
import { BubbleModule } from '../shared';
import { IsElementAvailablePipeModule, NormalizeDropdownMultiValuePipeModule } from '../core/pipes';
import { LocalDatePipeModule } from '../core/pipes/local-date-pipe';
import { ImageActionDialogModule } from './exploratory/image-action-dialog/image-action-dialog.module';
import { ImageDetailDialogModule } from './exploratory/image-detail-dialog/image-detail-dialog.module';
import { LibraryInfoModalModule } from './exploratory/library-info-modal/library-info-modal.module';
import { PageFilterComponent } from './exploratory/page-filter/page-filter.component';
import { DirectivesModule } from '../core/directives';

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
    BucketBrowserModule,
    CheckboxModule,
    BubbleModule,
    NormalizeDropdownMultiValuePipeModule,
    LocalDatePipeModule,
    ImageActionDialogModule,
    ImageDetailDialogModule,
    LibraryInfoModalModule,
    DirectivesModule,
    IsElementAvailablePipeModule,
  ],
  declarations: [
    ResourcesComponent,
    ManageUngitComponent,
    ConfirmDeleteAccountDialogComponent,
    ImagesComponent,
    PageFilterComponent,
  ],
  entryComponents: [ManageUngitComponent, ConfirmDeleteAccountDialogComponent],
  providers: [BucketDataService],
  exports: [ResourcesComponent]
})
export class ResourcesModule { }
