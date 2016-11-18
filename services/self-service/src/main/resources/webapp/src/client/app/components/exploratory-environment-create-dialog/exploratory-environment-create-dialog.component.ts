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

import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { Response } from "@angular/http";
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { UserResourceService } from "../../services/userResource.service";
import { ExploratoryEnvironmentCreateModel } from './exploratory-environment-create.model';
import { ExploratoryEnvironmentVersionModel } from '../../models/exploratoryEnvironmentVersion.model';
import { ResourceShapeModel } from '../../models/resourceShape.model';

import { ErrorMapUtils } from './../../util/errorMapUtils';
import HTTP_STATUS_CODES from 'http-status-enum';

@Component({
  moduleId: module.id,
  selector: 'exploratory-environment-create-dialog',
  templateUrl: 'exploratory-environment-create-dialog.component.html'
})

export class ExploratoryEnvironmentCreateDialog {
  model: ExploratoryEnvironmentCreateModel;
  notebookExist: boolean = false;
  checkValidity: boolean = false;
  templateDescription: string;
  namePattern = "[-_ a-zA-Z0-9]+";
  resourceGrid: any;

  processError: boolean = false;
  errorMessage: string = '';

  public createExploratoryEnvironmentForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('environment_name') environment_name;
  @ViewChild('environment_shape') environment_shape;


  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    private userResourceService: UserResourceService,
    private _fb: FormBuilder
  ) {
    this.model = ExploratoryEnvironmentCreateModel.getDefault(userResourceService);
  }

  ngOnInit() {
    this.initFormModel();
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  initFormModel(): void {
    this.createExploratoryEnvironmentForm = this._fb.group({
      environment_name: ['', [Validators.required, Validators.pattern(this.namePattern)]]
    });
  }

  createExploratoryEnvironment_btnClick($event, data, valid, index, shape) {
    this.notebookExist = false;
    this.checkValidity = true;

    if (this.resourceGrid.containsNotebook(data.environment_name)) {
      this.notebookExist = true;
      return false;
    }

    this.model.setCreatingParams(this.model.exploratoryEnvironmentTemplates[index].version, data.environment_name, shape);
    this.model.confirmAction();
    $event.preventDefault();
    return false;
  }

  templateSelectionChanged(value) {
    this.model.setSelectedTemplate(value);
  }

  open(params) {
    if (!this.bindDialog.isOpened) {
      this.model = new ExploratoryEnvironmentCreateModel('', '', '', (response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.close();
          this.buildGrid.emit();
        }
      },
        (response: Response) => {
          this.processError = true;
          this.errorMessage = ErrorMapUtils.setErrorMessage(response);
        },
        () => {
          this.templateDescription = this.model.selectedItem.description;
        },
        () => {
          this.bindDialog.open(params);
        },
        this.userResourceService);
    }
  }

  close() {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  private resetDialog(): void {
    this.notebookExist = false;
    this.checkValidity = false;
    this.processError = false;
    this.errorMessage = '';

    this.initFormModel();
    this.model.resetModel();
  }
}
