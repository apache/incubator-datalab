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
import { FormsModule } from '@angular/forms';
import { NgDateRangePickerModule } from 'ng-daterangepicker';
import { MaterialModule } from '../../shared/material.module';
import { FormControlsModule } from '../../shared/form-controls';
import { KeysPipeModule, LineBreaksPipeModule, ConvertActionPipeModule } from '../../core/pipes';
import {AuditComponent} from './audit.component';
import {AuditGridComponent, AuditInfoDialogComponent} from './audit-grid/audit-grid.component';
import {AuditToolbarComponent} from './audit-toolbar/audit-toolbar.component';
import {LocalDatePipeModule} from '../../core/pipes/local-date-pipe';
import {LocalCurrencyModule} from '../../core/pipes/local-currency-pipe';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        FormControlsModule,
        ConvertActionPipeModule,
        KeysPipeModule,
        LineBreaksPipeModule,
        NgDateRangePickerModule,
        MaterialModule,
        LocalDatePipeModule,
        LocalCurrencyModule
    ],
  declarations: [
    AuditGridComponent,
    AuditToolbarComponent,
    AuditComponent,
    AuditInfoDialogComponent
  ],
  entryComponents: [AuditInfoDialogComponent],
  exports: [AuditComponent]
})
export class AuditModule { }
