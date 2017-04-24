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
import { MaterialModule } from '@angular/material';

import { ModalModule } from '../../../shared/modal-dialog/index';
import { ComputationalResourcesList } from './computational-resources-list.component';
import { ConfirmationComputationalResourcesModule } from './../confirmation-computational-resources/index';
import { DetailComputationalResourcesModule } from './../detail-computational-resources/index';

export * from './computational-resources-list.component';

@NgModule({
  imports: [
    CommonModule,
    ModalModule,
    ConfirmationComputationalResourcesModule,
    DetailComputationalResourcesModule,
    MaterialModule.forRoot()
  ],
  declarations: [ComputationalResourcesList],
  exports: [ComputationalResourcesList],
})

export class ComputationalResourcesModule { }
