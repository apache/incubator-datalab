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
import { MaterialModule } from '../material.module';

import { TimeCoverComponent } from './time-cover.component';
import { TickerComponent } from './ticker.component';
import {
  TimePickerComponent,
  TimePickerDialogComponent
} from './time-picker.component';
import {LocalDatePipeModule} from '../../core/pipes/local-date-pipe';

export * from './time-picker.component';

@NgModule({
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MaterialModule, LocalDatePipeModule],
  declarations: [TimePickerComponent, TimePickerDialogComponent, TimeCoverComponent, TickerComponent],
  entryComponents: [TimePickerDialogComponent],
  exports: [TimePickerComponent]
})
export class TimePickerModule {}
