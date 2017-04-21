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

import { Component, ViewChild, Output, EventEmitter } from '@angular/core';
import { Response } from '@angular/http';

import { UserResourceService } from '../../../core/services';
import { ComputationalResourcesModel } from './confirmation-computational-resources.model';
import { ErrorMapUtils } from '../../../core/util/errorMapUtils';

@Component({
  moduleId: module.id,
  selector: 'confirmation-computational-resources',
  templateUrl: 'confirmation-computational-resources.component.html'
})

export class ConfirmationComputationalResources {
  model: ComputationalResourcesModel;

  processError: boolean = false;
  tooltip: boolean = false;
  errorMessage: string = '';

  @ViewChild('bindDialog') bindDialog;
  @Output() rebuildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(private userResourceService: UserResourceService) {
    this.model = ComputationalResourcesModel.getDefault(userResourceService);
  }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  public open(option, notebook, resource) {
    this.tooltip = false;
    this.model = new ComputationalResourcesModel(notebook, resource,
      (response: Response) => {
        this.close();
        this.rebuildGrid.emit();
      },
      (response: Response) => {
        this.processError = true;
        this.errorMessage = ErrorMapUtils.setErrorMessage(response);
      },
      this.userResourceService);

    if (!this.bindDialog.isOpened) {
      this.bindDialog.open(option);
    }
  }

  public isEllipsisActive($event) {
    if ($event.target.offsetWidth < $event.target.scrollWidth)
      this.tooltip = true;
  }

  public close() {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  private resetDialog(): void {
    this.processError = false;
    this.errorMessage = '';
  }
}
