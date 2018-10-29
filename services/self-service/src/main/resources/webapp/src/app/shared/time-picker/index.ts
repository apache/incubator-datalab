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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../material.module';

import { TimeCoverComponent } from './time-cover.component';
import { TickerComponent } from './ticker.component';
import {
  TimePickerComponent,
  TimePickerDialogComponent
} from './time-picker.component';

export * from './time-picker.component';

@NgModule({
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MaterialModule],
  declarations: [TimePickerComponent, TimePickerDialogComponent, TimeCoverComponent, TickerComponent],
  entryComponents: [TimePickerDialogComponent],
  exports: [TimePickerComponent]
})
export class TimePickerModule {}
