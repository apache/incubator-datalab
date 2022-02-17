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
import { MaterialModule } from '../material.module';
import { ProgressDialogModule, BubbleModule } from '../index';
import { NavbarComponent } from './navbar.component';
import { NotificationDialogModule } from '../modal-dialog/notification-dialog';
import {EdgeActionDialogModule} from '../modal-dialog/edge-action-dialog';
import {OdahuActionDialogModule} from '../modal-dialog/odahu-action-dialog';
import { TruncateTextPipeModule } from '../../core/pipes/truncate-text-pipe';


export * from './navbar.component';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    MaterialModule,
    NotificationDialogModule,
    EdgeActionDialogModule,
    ProgressDialogModule,
    BubbleModule,
    OdahuActionDialogModule,
    TruncateTextPipeModule,
  ],
  declarations: [NavbarComponent],
  exports: [NavbarComponent, RouterModule]
})
export class NavbarModule { }
