/***************************************************************************

Copyright (c) 2018, EPAM SYSTEMS INC

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

import { Component, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';

import { UserResourceService } from '../../../core/services';
import { HTTP_STATUS_CODES } from '../../../core/util';

@Component({
  selector: 'dlab-ami-create-dialog',
  templateUrl: './ami-create-dialog.component.html',
  styleUrls: ['./ami-create-dialog.component.scss']
})
export class AmiCreateDialogComponent {
  public notebook: any;
  public createAMIForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;

  constructor(
    private _userResource: UserResourceService,
    private _fb: FormBuilder
  ) {}

  public open(param, notebook): void {
    this.notebook = notebook;

    this.initFormModel();
    this.bindDialog.open(param);
  }

  public resetForm() {
    this.initFormModel();
    this.bindDialog.close();
  }

  public assignChanges(data) {
    this._userResource.createAMI(data).subscribe(res => {
      if (res.status === HTTP_STATUS_CODES.ACCEPTED) {
        debugger;
        this.bindDialog.close();
      }
    });
  }

  private initFormModel(): void {
    this.createAMIForm = this._fb.group({
      name: ['', Validators.required],
      description: [''],
      exploratory_name: [this.notebook.name]
    });
  }
}
