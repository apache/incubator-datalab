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

import { Component, OnInit, ViewChild, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { Response } from '@angular/http';

import { ConfirmationDialogModel } from './confirmation-dialog.model';
import { ConfirmationDialogType } from './confirmation-dialog-type.enum';
import { UserResourceService, HealthStatusService, ManageEnvironmentsService } from '../../../core/services';
import { ErrorMapUtils, HTTP_STATUS_CODES } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  moduleId: module.id,
  selector: 'confirmation-dialog',
  templateUrl: 'confirmation-dialog.component.html',
  styleUrls: ['./confirmation-dialog.component.scss', '../modal.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class ConfirmationDialogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  model: ConfirmationDialogModel;
  isAliveResources: boolean;
  processError: boolean = false;
  errorMessage: string = '';

  dataengines: Array<any> = [];
  dataengineServices: Array<any> = [];

  confirmationType: number = 0;

  @ViewChild('bindDialog') bindDialog;
  @Input() manageAction: boolean = false;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    private userResourceService: UserResourceService,
    private healthStatusService: HealthStatusService,
    private manageEnvironmentsService: ManageEnvironmentsService
  ) {
    this.model = ConfirmationDialogModel.getDefault();
  }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  public open(param, notebook: any, type: ConfirmationDialogType) {
    this.confirmationType = type;

    this.model = new ConfirmationDialogModel(type, notebook, (response: Response) => {
      if (response.status === HTTP_STATUS_CODES.OK) {
        this.close();
        this.buildGrid.emit();
      }
    },
      (response: Response) => {
        this.processError = true;
        this.errorMessage = ErrorMapUtils.setErrorMessage(response);
      },
      this.manageAction,
      this.userResourceService,
      this.healthStatusService,
      this.manageEnvironmentsService);

    this.bindDialog.open(param);
    if (!this.confirmationType) this.filterResourcesByType(notebook.resources);
    this.isAliveResources = this.model.isAliveResources(notebook.resources);
  }

  public close() {
    this.bindDialog.close();
  }

  private filterResourcesByType(resources) {
    resources
    .filter(resource => (resource.status != 'failed' && resource.status != 'terminated' && resource.status != 'terminating' && resource.status != 'stopped'))
    .forEach(resource => { (resource.image === 'docker.dlab-dataengine') ? this.dataengines.push(resource) : this.dataengineServices.push(resource); });
  }

  private resetDialog(): void {
    this.processError = false;
    this.dataengines = [];
    this.dataengineServices = [];
    this.errorMessage = '';
  }
}
