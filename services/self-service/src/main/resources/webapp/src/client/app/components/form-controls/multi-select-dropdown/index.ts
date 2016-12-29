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
import { MultiSelectDropdown } from './multi-select-dropdown.component';
import { ClickOutsideModule } from '../../../directives/click-outside/index';

export * from './multi-select-dropdown.component';

@NgModule({
  imports: [CommonModule, ClickOutsideModule],
  declarations: [MultiSelectDropdown],
  exports: [MultiSelectDropdown]
})

export class MultiSelectDropdownModule { }
