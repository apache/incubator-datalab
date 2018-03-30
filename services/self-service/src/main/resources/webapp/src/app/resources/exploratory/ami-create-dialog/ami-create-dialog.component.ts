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

import { Component, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';

import { UserResourceService } from '../../../core/services';
import { HTTP_STATUS_CODES } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'dlab-ami-create-dialog',
  templateUrl: './ami-create-dialog.component.html',
  styleUrls: ['./ami-create-dialog.component.scss']
})
export class AmiCreateDialogComponent {
  readonly DICTIONARY = DICTIONARY;
  public notebook: any;
  public createAMIForm: FormGroup;

  namePattern = '[-_a-zA-Z0-9]+';
  delimitersRegex = /[-_]?/g;
  imagesList: any;

  @ViewChild('bindDialog') bindDialog;
  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    private _userResource: UserResourceService,
    private _fb: FormBuilder
  ) {}

  ngOnInit() {
    this._userResource.getImagesList().subscribe(res => this.imagesList = res);
  }

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
        this.bindDialog.close();
        this.buildGrid.emit();
      }
    });
  }

  private initFormModel(): void {
    this.createAMIForm = this._fb.group({
      name: ['', [Validators.required, Validators.pattern(this.namePattern), this.providerMaxLength, this.checkDuplication.bind(this)]],
      description: [''],
      exploratory_name: [this.notebook.name]
    });
  }

  private providerMaxLength(control) {
    if (DICTIONARY.cloud_provider !== 'aws')
      return control.value.length <=10 ? null : { valid: false };
  }

  private delimitersFiltering(resource): string {
    return resource.replace(this.delimitersRegex, '').toString().toLowerCase();
  }

  private checkDuplication(control) {
    if (control.value)
      return this.isDuplicate(control.value) ? { duplication: true } : null;
  }

  private isDuplicate(value: string) {
    for (let index = 0; index < this.imagesList.length; index++) {
      if (this.delimitersFiltering(value) === this.delimitersFiltering(this.imagesList[index].name))
        return true;
    }
    return false;
  }
}
