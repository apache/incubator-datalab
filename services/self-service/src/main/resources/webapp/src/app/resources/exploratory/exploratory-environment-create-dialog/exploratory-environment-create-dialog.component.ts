/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Component, OnInit, EventEmitter, Output, ViewChild, ChangeDetectorRef, Inject } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ToastrService } from 'ngx-toastr';

import { Project } from '../../../administration/project/project.component';

import { ExploratoryEnvironmentCreateModel } from './exploratory-environment-create.model';
import { UserResourceService, ProjectService } from '../../../core/services';
import { CheckUtils, HTTP_STATUS_CODES } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { CLUSTER_CONFIGURATION } from '../../computational/computational-resource-create-dialog/cluster-configuration-templates';

@Component({
  selector: 'exploratory-environment-create-dialog',
  templateUrl: 'exploratory-environment-create-dialog.component.html',
  styleUrls: ['./create-environment.component.scss']
})

export class ExploratoryEnvironmentCreateComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  projects: Project[] =[];
  templates = [];
  currentTemplate: any;
  shapes: Array<any> = [];

  model: ExploratoryEnvironmentCreateModel;
  templateDescription: string;
  namePattern = '[-_a-zA-Z0-9]*[_-]*[a-zA-Z0-9]+';
  resourceGrid: any;
  images: Array<any>;
  environment_shape: string;

  public createExploratoryForm: FormGroup;

  // @ViewChild('environment_name') environment_name;
  // @ViewChild('templatesList') templates_list;
  // @ViewChild('shapesList') shapes_list;
  // @ViewChild('imagesList') userImagesList;
  @ViewChild('configurationNode') configuration;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<ExploratoryEnvironmentCreateComponent>,
    private userResourceService: UserResourceService,
    private _fb: FormBuilder,
    private changeDetector: ChangeDetectorRef,
    private projectService: ProjectService
  ) {
    this.model = ExploratoryEnvironmentCreateModel.getDefault(userResourceService);
    this.resourceGrid = data;
  }

  ngOnInit() {
    this.getProjects();

    this.initFormModel();
  }

  initFormModel(): void {
    this.createExploratoryForm = this._fb.group({
      project: [''],
      template_name: [''],
      image: [''],
      shape: [''],
      environment_name: ['', [Validators.required, Validators.pattern(this.namePattern),
                              this.providerMaxLength, this.checkDuplication.bind(this)]],
      configuration_parameters: ['', [this.validConfiguration.bind(this)]]
    });
  }

  public getProjects() {
    this.projectService.getProjectsList().subscribe((projects: any) => this.projects = projects);
  }

  public getTemplates($event) {
    this.userResourceService.getExploratoryTemplates($event.value).subscribe(templates => this.templates = templates);
  }

  public getShapes($event, template) {
    this.currentTemplate = template;
    this.shapes = template.exploratory_environment_shapes;
    this.getImagesList();
  }

  providerMaxLength(control) {
    if (DICTIONARY.cloud_provider !== 'aws')
      return control.value.length <= 10 ? null : { valid: false };
  }

  private validConfiguration(control) {
    if (this.configuration)
      return this.configuration.nativeElement['checked']
        ? (control.value && control.value !== null && CheckUtils.isJSON(control.value) ? null : { valid: false })
        : null;
  }

  checkDuplication(control) {
    if (this.resourceGrid.containsNotebook(control.value))
      return { duplication: true };
  }

  shapePlaceholder(resourceShapes, byField: string): string {
    for (const index in resourceShapes)
      return resourceShapes[index][0][byField];
  }

  // setDefaultParams(): void {
  //   this.environment_shape = this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'type');

  //   this.templates_list.setDefaultOptions(this.model.exploratoryEnvironmentTemplates,
  //     this.model.selectedItem.template_name, 'template', 'template_name', 'array');
  //   this.shapes_list.setDefaultOptions(this.model.selectedItem.shapes.resourcesShapeTypes,
  //     this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'description'), 'shape', 'description', 'json');

    // if (this.userImages && this.userImages.length > 0) {
    //   this.userImagesList.setDefaultOptions(this.userImages, 'Select existing ' + DICTIONARY.image, 'ami', 'name', 'array', null, true);
    // }
  // }

  // onUpdate($event): void {
  //   if ($event.model.type === 'template') {
  //     this.model.setSelectedTemplate($event.model.index);
  //     this.shapes_list.setDefaultOptions(this.model.selectedItem.shapes.resourcesShapeTypes,
  //       this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'description'), 'shape', 'description', 'json');
  //     this.environment_shape = this.shapePlaceholder(this.model.selectedItem.shapes.resourcesShapeTypes, 'type');

  //     this.getImagesList();
  //   }

  //   if ($event.model.type === 'shape')
  //     this.environment_shape = $event.model.value.type;
  // }

  public selectImage($event): void {
    debugger;
    // this.model.notebookImage = $event.model.value ? $event.model.value.fullName : null;
  }

  createExploratoryEnvironment_btnClick($event, data) {
    this.model.setCreatingParams(
      data.environment_name,
      this.environment_shape,
      data.configuration_parameters ? JSON.parse(data.configuration_parameters) : null);
    this.model.confirmAction();
    $event.preventDefault();
    return false;
  }

  // public open(params?): void {
  //   this.model = new ExploratoryEnvironmentCreateModel('', '', '', '', '',
  //   response => {
  //     if (response.status === HTTP_STATUS_CODES.OK) this.dialogRef.close();
  //   },
  //   error => this.toastr.error(error.message || 'Exploratory creation failed!', 'Oops!'),
  //   () => this.templateDescription = this.model.selectedItem.description,
  //   () => {
  //     this.initFormModel();
  //     // this.setDefaultParams();
  //     this.getImagesList();
  //   },
  //   this.userResourceService);
  // }

  public selectConfiguration() {
    const value = (this.configuration.nativeElement.checked && this.createExploratoryForm)
      ? JSON.stringify(CLUSTER_CONFIGURATION.SPARK, undefined, 2) : '';

    this.createExploratoryForm.controls['configuration_parameters'].setValue(value);
  }

  private getImagesList() {
    this.userResourceService.getUserImages(this.currentTemplate.image)
      .subscribe((res: any) => this.images = res.filter(el => el.status === 'CREATED'),
      error => this.toastr.error(error.message || 'Images list loading failed!', 'Oops!'));
  }
}
