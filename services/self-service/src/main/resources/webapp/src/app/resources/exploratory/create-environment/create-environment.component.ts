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

import { Component, OnInit, ViewChild, Inject } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ToastrService } from 'ngx-toastr';

import { Project } from '../../../administration/project/project.component';
import { UserResourceService, ProjectService } from '../../../core/services';
import { CheckUtils, SortUtils, HTTP_STATUS_CODES, PATTERNS } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { CLUSTER_CONFIGURATION } from '../../computational/computational-resource-create-dialog/cluster-configuration-templates';

@Component({
  selector: 'create-environment',
  templateUrl: 'create-environment.component.html',
  styleUrls: ['./create-environment.component.scss']
})

export class ExploratoryEnvironmentCreateComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public createExploratoryForm: FormGroup;

  projects: Project[] = [];
  templates = [];
  endpoints: Array<String> = [];
  currentTemplate: any;
  shapes = [] || {};
  resourceGrid: any;
  images: Array<any>;

  @ViewChild('configurationNode') configuration;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<ExploratoryEnvironmentCreateComponent>,
    private userResourceService: UserResourceService,
    private _fb: FormBuilder,
    private projectService: ProjectService
  ) {
    this.resourceGrid = data;
  }

  ngOnInit() {
    this.getUserProjects();
    this.initFormModel();
  }

  public getProjects() {
    this.projectService.getProjectsList().subscribe((projects: any) => this.projects = projects);
  }

  public getUserProjects() {
    this.projectService.getUserProjectsList().subscribe((projects: any) => this.projects = projects);
  }

  public getTemplates($event, project) {
    this.endpoints = project.endpoints;
    this.userResourceService.getExploratoryTemplates($event.value).subscribe(templates => this.templates = templates);
  }

  public getShapes(template) {
    this.currentTemplate = template;
    this.shapes = SortUtils.shapesSort(template.exploratory_environment_shapes);
    this.getImagesList();
  }

  public createExploratoryEnvironment(data) {
    const parameters: any = {
      image: this.currentTemplate.image,
      template_name: this.currentTemplate.exploratory_environment_versions[0].template_name
    };

    data.cluster_config = data.cluster_config ? JSON.parse(data.cluster_config) : null
    this.userResourceService.createExploratoryEnvironment({ ...parameters, ...data }).subscribe((response: any) => {
      if (response.status === HTTP_STATUS_CODES.OK) this.dialogRef.close();
    }, error => this.toastr.error(error.message || 'Exploratory creation failed!', 'Oops!'));
  }


  public selectConfiguration() {
    const value = (this.configuration.nativeElement.checked && this.createExploratoryForm)
      ? JSON.stringify(CLUSTER_CONFIGURATION.SPARK, undefined, 2) : '';

    document.querySelector('#config').scrollIntoView({ block: 'start', behavior: 'smooth' });
    this.createExploratoryForm.controls['cluster_config'].setValue(value);
  }

  private initFormModel(): void {
    this.createExploratoryForm = this._fb.group({
      project: ['', Validators.required],
      endpoint: ['', Validators.required],
      version: ['', Validators.required],
      notebook_image_name: [''],
      shape: ['', Validators.required],
      name: ['', [Validators.required, Validators.pattern(PATTERNS.namePattern), this.providerMaxLength, this.checkDuplication.bind(this)]],
      cluster_config: ['', [this.validConfiguration.bind(this)]],
      custom_tag: ['', [Validators.pattern(PATTERNS.namePattern)]]
    });
  }

  private getImagesList() {
    this.userResourceService.getUserImages(this.currentTemplate.image)
      .subscribe((res: any) => this.images = res.filter(el => el.status === 'CREATED'),
        error => this.toastr.error(error.message || 'Images list loading failed!', 'Oops!'));
  }

  private checkDuplication(control) {
    if (this.resourceGrid.containsNotebook(control.value))
      return { duplication: true };
  }

  private providerMaxLength(control) {
    if (DICTIONARY.cloud_provider !== 'aws')
      return control.value.length <= 10 ? null : { valid: false };
  }

  private validConfiguration(control) {
    if (this.configuration)
      return this.configuration.nativeElement['checked']
        ? (control.value && control.value !== null && CheckUtils.isJSON(control.value) ? null : { valid: false })
        : null;
  }
}
