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

import { DropdownListComponent } from './dropdown-list/dropdown-list.component';
import { MultiSelectDropdownComponent } from './multi-select-dropdown/multi-select-dropdown.component';
import { DirectivesModule } from '../../core/directives';
import {ConvertActionPipeModule, KeysPipeModule, UnderscorelessPipeModule} from '../../core/pipes';
import { BubbleModule } from '..';
import {MultiLevelSelectDropdownComponent} from './multi-level-select-dropdown/multi-level-select-dropdown.component';
import {CheckboxModule} from '../checkbox';

export * from './multi-select-dropdown/multi-select-dropdown.component';
export * from './dropdown-list/dropdown-list.component';

@NgModule({
  imports: [
    CommonModule,
    DirectivesModule,
    KeysPipeModule,
    UnderscorelessPipeModule,
    BubbleModule,
    ConvertActionPipeModule,
    CheckboxModule
  ],
  declarations: [DropdownListComponent, MultiSelectDropdownComponent, MultiLevelSelectDropdownComponent],
  exports: [DropdownListComponent, MultiSelectDropdownComponent, MultiLevelSelectDropdownComponent]
})
export class FormControlsModule {}
