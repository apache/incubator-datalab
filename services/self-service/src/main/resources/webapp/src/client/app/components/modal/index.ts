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

import { Modal } from './modal.component';
import { CommonModule } from '@angular/common';
import { NgModule, Component } from '@angular/core';

export * from './modal.component';

@Component({
  selector: 'modal-header',
  template: `<ng-content></ng-content>`
})
export class ModalHeader { }

@Component({
  selector: 'modal-content',
  template: `<ng-content></ng-content>`
})
export class ModalContent { }

@Component({
  selector: 'modal-footer',
  template: `<ng-content></ng-content>`
})
export class ModalFooter { }


@NgModule({
  imports: [CommonModule],
  declarations: [Modal, ModalHeader, ModalContent, ModalFooter],
  exports: [Modal, ModalHeader, ModalContent, ModalFooter]
})
export class ModalModule { }
