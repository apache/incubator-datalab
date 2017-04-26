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

import { DropdownList } from './dropdown-list/dropdown-list.component';
import { MultiSelectDropdown } from './multi-select-dropdown/multi-select-dropdown.component';
import { ClickOutsideModule } from '../../core/directives/click-outside/index';
import { KeysPipeModule, UnderscorelessPipeModule } from '../../core/pipes';

export * from './multi-select-dropdown/multi-select-dropdown.component';
export * from './dropdown-list/dropdown-list.component';

@NgModule({
  imports: [
    CommonModule,
    ClickOutsideModule,
    KeysPipeModule,
    UnderscorelessPipeModule
  ],
  declarations: [
    DropdownList,
    MultiSelectDropdown
  ],
  exports: [
    DropdownList,
    MultiSelectDropdown
  ]
})

export class FormControlsModule { }
