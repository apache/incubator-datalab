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

import { CommonModule } from '@angular/common';
import { NgModule, Component } from '@angular/core';

import { MaterialModule } from '../material.module';
import { ModalComponent } from './modal.component';

export * from './modal.component';

@Component({
  selector: 'modal-header',
  template: `<ng-content></ng-content>`
})
export class ModalHeaderComponent {}

@Component({
  selector: 'modal-content',
  template: `<ng-content></ng-content>`
})
export class ModalContentComponent {}

@Component({
  selector: 'modal-footer',
  template: `<ng-content></ng-content>`
})
export class ModalFooterComponent {}

@NgModule({
  imports: [CommonModule, MaterialModule],
  declarations: [
    ModalComponent,
    ModalHeaderComponent,
    ModalContentComponent,
    ModalFooterComponent
  ],
  exports: [
    ModalComponent,
    ModalHeaderComponent,
    ModalContentComponent,
    ModalFooterComponent
  ]
})
export class ModalModule {}
