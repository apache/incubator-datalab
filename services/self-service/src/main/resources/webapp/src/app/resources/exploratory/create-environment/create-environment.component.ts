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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { Project } from '../../../administration/project/project.component';
import { UserResourceService, ProjectService } from '../../../core/services';
import { CheckUtils, SortUtils, HTTP_STATUS_CODES, PATTERNS } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { CLUSTER_CONFIGURATION } from '../../computational/computational-resource-create-dialog/cluster-configuration-templates';
import {tap} from 'rxjs/operators';

@Component({
  selector: 'create-environment',
  templateUrl: 'create-environment.component.html',
  styleUrls: ['./create-environment.component.scss']
})

export class ExploratoryEnvironmentCreateComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public createExploratoryForm: FormGroup;
  public projectExploratories: {};

  projects: Project[] = [];
  templates = [];
  endpoints: Array<String> = [];
  currentTemplate: any;
  shapes = [] || {};
  resourceGrid: any;
  images: Array<any>;
  maxNotebookLength: number = 14;

  @ViewChild('configurationNode', { static: false }) configuration;

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
    this.getNamesByProject();
    this.getUserProjects();
    this.initFormModel();
    this.createExploratoryForm.get('project').valueChanges.subscribe(v => {
      if ( this.createExploratoryForm.controls.name.value) {
        this.createExploratoryForm.get('name').updateValueAndValidity();
      }
    });
  }

  public getProjects() {
    this.projectService.getProjectsList().subscribe((projects: any) => this.projects = projects);
  }

  public getUserProjects() {
    this.projectService.getUserProjectsList(true).subscribe((projects: any) => {
      this.projects = projects;
      const activeProject = projects.find(item => item.name === this.resourceGrid.activeProject);
      if (this.resourceGrid.activeProject && activeProject) {
        this.setEndpoints(activeProject);
        this.createExploratoryForm.controls['project'].setValue(activeProject.name);
      }
    });
  }

  public setEndpoints(project) {
    if (this.images) this.images = [];

    this.endpoints = project.endpoints
      .filter(e => e.status === 'RUNNING')
      .map(e => e.name);
  }

  public getTemplates(project, endpoint) {
    this.userResourceService.getExploratoryTemplates(project, endpoint)
      .pipe(tap(results => {
        results.sort((a, b) =>
          (a.exploratory_environment_versions[0].template_name > b.exploratory_environment_versions[0].template_name) ?
            1 : -1);
      }))
      .subscribe(templates =>  {
        this.templates = templates;
      }
      );
  }

  public getShapes(template) {
    this.currentTemplate = template;
    const allowed: any = ['GPU optimized'];
    if (template.exploratory_environment_versions[0].template_name.toLowerCase().indexOf('tensorflow') === -1
      && template.exploratory_environment_versions[0].template_name.toLowerCase().indexOf('deep learning') === -1
    ) {
      const filtered = Object.keys(
        SortUtils.shapesSort(template.exploratory_environment_shapes))
        .filter(key => !(allowed.includes(key)))
        .reduce((obj, key) => {
          obj[key] = template.exploratory_environment_shapes[key];
          return obj;
        }, {});
      template.exploratory_environment_shapes.computation_resources_shapes = filtered;
      this.shapes = SortUtils.shapesSort(template.exploratory_environment_shapes.computation_resources_shapes);
      this.getImagesList();
    } else {
      this.shapes = SortUtils.shapesSort(template.exploratory_environment_shapes);
      this.getImagesList();
    }
  }

  public createExploratoryEnvironment(data) {
    const parameters: any = {
      image: this.currentTemplate.image,
      template_name: this.currentTemplate.exploratory_environment_versions[0].template_name
    };

    data.cluster_config = data.cluster_config ? JSON.parse(data.cluster_config) : null;
    this.userResourceService.createExploratoryEnvironment({ ...parameters, ...data }).subscribe((response: any) => {
      if (response.status === HTTP_STATUS_CODES.OK) this.dialogRef.close();
    }, error => this.toastr.error(error.message || 'Exploratory creation failed!', 'Oops!'));
  }

  public getNamesByProject() {
    this.userResourceService.getProjectByExploratoryEnvironment().subscribe(responce => {
      this.projectExploratories = responce;
    });
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
      name: ['', [
        Validators.required,
        Validators.pattern(PATTERNS.namePattern),
        this.providerMaxLength.bind(this),
        this.checkDuplication.bind(this)
      ]],
      cluster_config: ['', [this.validConfiguration.bind(this)]],
      custom_tag: ['', [Validators.pattern(PATTERNS.namePattern)]]
    });
  }

  private getImagesList() {
    this.userResourceService.getUserImages(this.currentTemplate.image, this.createExploratoryForm.controls['project'].value,
      this.createExploratoryForm.controls['endpoint'].value)
      .subscribe((res: any) => this.images = res.filter(el => el.status === 'CREATED'),
        error => this.toastr.error(error.message || 'Images list loading failed!', 'Oops!'));
  }

  private checkDuplication(control) {
    if (this.createExploratoryForm
      && this.createExploratoryForm.controls.project.value
      && this.resourceGrid.containsNotebook(control.value, this.projectExploratories[this.createExploratoryForm.controls.project.value]))
      return { duplication: true };
  }

  private providerMaxLength(control) {
    if (control && control.value)
      return control.value.length <= this.maxNotebookLength ? null : { valid: false };
  }

  private validConfiguration(control) {
    if (this.configuration)
      return this.configuration.nativeElement['checked']
        ? (control.value && control.value !== null && CheckUtils.isJSON(control.value) ? null : { valid: false })
        : null;
  }
}
