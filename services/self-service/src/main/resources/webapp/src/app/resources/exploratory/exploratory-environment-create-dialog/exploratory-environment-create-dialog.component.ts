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

import { Component, OnInit, EventEmitter, Output, ViewChild, ChangeDetectorRef, ViewContainerRef } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { ToastsManager } from 'ng2-toastr';

import { ExploratoryEnvironmentCreateModel } from '.';
import { UserResourceService } from '../../../core/services';
import { ErrorUtils, HTTP_STATUS_CODES } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'exploratory-environment-create-dialog',
  templateUrl: 'exploratory-environment-create-dialog.component.html'
})

export class ExploratoryEnvironmentCreateDialogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  model: ExploratoryEnvironmentCreateModel;
  templateDescription: string;
  namePattern = '[-_a-zA-Z0-9]+';
  resourceGrid: any;
  userImages: Array<any>;
  environment_shape: string;
  
  public createExploratoryEnvironmentForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('environment_name') environment_name;
  @ViewChild('templatesList') templates_list;
  @ViewChild('shapesList') shapes_list;
  @ViewChild('imagesList') userImagesList;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    private userResourceService: UserResourceService,
    private _fb: FormBuilder,
    private changeDetector : ChangeDetectorRef,
    public toastr: ToastsManager,
    public vcr: ViewContainerRef
  ) {
    this.model = ExploratoryEnvironmentCreateModel.getDefault(userResourceService);
    this.toastr.setRootViewContainerRef(vcr);
  }

  ngOnInit() {
    this.initFormModel();
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  initFormModel(): void {
    this.createExploratoryEnvironmentForm = this._fb.group({
      environment_name: ['', [Validators.required, Validators.pattern(this.namePattern), this.providerMaxLength, this.checkDuplication.bind(this)]]
    });
  }

  providerMaxLength(control) {
    if (DICTIONARY.cloud_provider !== 'aws')
      return control.value.length <=10 ? null : { valid: false };
  }

  checkDuplication(control) {
    if (this.resourceGrid.containsNotebook(control.value))
      return { duplication: true }
  }

  shapePlaceholder(resourceShapes, byField: string): string {
    for (const index in resourceShapes)
      return resourceShapes[index][0][byField];
  }

  setDefaultParams(): void {
    this.environment_shape = this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'type');

    this.templates_list.setDefaultOptions(this.model.exploratoryEnvironmentTemplates,
      this.model.selectedItem.template_name, 'template', 'template_name', 'array');
    this.shapes_list.setDefaultOptions(this.model.selectedItem.shapes.resourcesShapeTypes,
      this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'description'), 'shape', 'description', 'json');

    if (this.userImages && this.userImages.length > 0) {
      this.userImagesList.setDefaultOptions(this.userImages, 'Select existing ' + DICTIONARY.image, 'ami', 'name', 'array', null, true);
    }
  }

  onUpdate($event): void {
    if ($event.model.type === 'template') {
      this.model.setSelectedTemplate($event.model.index);
      this.shapes_list.setDefaultOptions(this.model.selectedItem.shapes.resourcesShapeTypes,
        this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'description'), 'shape', 'description', 'json');
      this.environment_shape = this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'type');

      this.getImagesList();
    }

    if ($event.model.type === 'shape')
      this.environment_shape = $event.model.value.type;
  }

  selectImage($event): void {
    this.model.notebookImage = $event.model.value ? $event.model.value.fullName : null;
  }

  createExploratoryEnvironment_btnClick($event, data) {
    this.model.setCreatingParams(data.environment_name, this.environment_shape);
    this.model.confirmAction();
    $event.preventDefault();
    return false;
  }

  public open(params): void {
    if (!this.bindDialog.isOpened) {
      this.model = new ExploratoryEnvironmentCreateModel('', '', '', '', '',
      response => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.close();
          this.buildGrid.emit();
        }
      },
      error => this.toastr.error(error.message || 'Exploratory creation failed!', 'Oops!', { toastLife: 5000 }),
      () => {
        this.templateDescription = this.model.selectedItem.description;
      },
      () => {
        this.bindDialog.open(params);
        this.setDefaultParams();
        this.getImagesList();
      },
      this.userResourceService);
    }
  }

  getImagesList() {
    const image = this.model.selectedItem.image;
    this.userResourceService.getUserImages(image)
      .subscribe((res: any) => {
        this.userImages = res.filter(el => el.status === 'CREATED');
        
        this.changeDetector.detectChanges();
        this.setDefaultParams();
      },
      error => this.toastr.error(error.message || 'Images list loading failed!', 'Oops!', { toastLife: 5000 }));
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  private resetDialog(): void {
    this.initFormModel();
    this.model.resetModel();
  }
}
