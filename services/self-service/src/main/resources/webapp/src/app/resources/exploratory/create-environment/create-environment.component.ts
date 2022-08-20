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

import { Component, OnInit, Inject } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { UserResourceService, ProjectService } from '../../../core/services';
import { CheckUtils, SortUtils, HTTP_STATUS_CODES, PATTERNS, HelpUtils } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { CLUSTER_CONFIGURATION } from '../../computational/computational-resource-create-dialog/cluster-configuration-templates';
import { tap } from 'rxjs/operators';
import { timer } from 'rxjs';
import { DockerImageName } from '../../../core/models';
import { Project } from '../../../administration/project/project.model';

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
  selectedImage: any;
  maxNotebookLength: number = 14;
  maxCustomTagLength: number = 63;
  templateName = DockerImageName;
  public areShapes: boolean;
  public selectedCloud: string = '';
  public gpuCount: Array<number>;
  public gpuTypes: Array<string> = [];
  public addSizeToGpuType = HelpUtils.addSizeToGpuType;

  public additionalParams = {
    configurationNode: false,
    gpu: false,
  };

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

  public setImage(image) {
    this.selectedImage = image;
    timer(500).subscribe(_ => {
      document.querySelector('#buttons').scrollIntoView({ block: 'center', behavior: 'smooth' });
    });
  }

  public setEndpoints(project) {
    const controls = ['endpoint', 'version', 'shape', 'gpu_type', 'gpu_count'];
    this.resetSelections(controls);

    this.endpoints = project.endpoints
      .filter(e => e.status === 'RUNNING')
      .map(e => e.name);
  }

  public resetSelections(controls: Array<string>) {
    if (this.images) this.images = [];
    if (this.selectedCloud) this.selectedCloud = '';
    if (this.additionalParams.gpu) {
      this.additionalParams.gpu = !this.additionalParams.gpu;
      this.createExploratoryForm.controls['gpu_enabled'].setValue(this.additionalParams.gpu);
    }
    this.selectedImage = null;
    this.areShapes = false;
    this.templates = [];
    this.currentTemplate = [];
    controls.forEach(control => {
      this.createExploratoryForm.controls[control].setValue(null);
    });
  }

  public getTemplates(project, endpoint) {
    const controls = ['version', 'shape'];
    this.resetSelections(controls);

    const endpoints = this.data.environments.find(env => env.project === project).endpoints;
    this.selectedCloud = endpoints.find(endp => endp.name === endpoint).cloudProvider.toLowerCase();

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
    this.selectedImage = null;
    const controls = ['notebook_image_name', 'shape', 'gpu_type', 'gpu_count'];

    controls.forEach(control => {
      this.createExploratoryForm.controls[control].setValue(null);
      if (control !== 'shape') {
        this.createExploratoryForm.controls[control].clearValidators();
        this.createExploratoryForm.controls[control].updateValueAndValidity();
      }
    });

    if (this.additionalParams.gpu) {
      this.additionalParams.gpu = !this.additionalParams.gpu;
      this.createExploratoryForm.controls['gpu_enabled'].setValue(this.additionalParams.gpu);
    }

    if (this.selectedCloud === 'gcp' && template?.image === 'docker.datalab-deeplearning') {
      this.createExploratoryForm.controls['notebook_image_name'].setValidators([Validators.required]);
      this.createExploratoryForm.controls['notebook_image_name'].updateValueAndValidity();
    }

    if (this.selectedCloud === 'gcp' &&
        (template?.image === 'docker.datalab-jupyter' ||
        template?.image === 'docker.datalab-deeplearning' ||
        template?.image === 'docker.datalab-tensor')) {

      this.gpuTypes = template?.computationGPU ? HelpUtils.sortGpuTypes(template.computationGPU) : [];

      // tslint:disable-next-line:max-line-length
      if (template?.image === 'docker.datalab-tensor' /**|| template?.image === 'docker.datalab-jupyter-conda'|| template?.image === 'docker.datalab-jupyter-gpu' */|| template?.image === 'docker.datalab-deeplearning') {
        this.addGpuFields();
      }
    }

    this.currentTemplate = template;
    const allowed: any = ['GPU optimized'];

    if (template.exploratory_environment_versions[0].template_name.toLowerCase().indexOf('tensorflow') === -1
      && template.exploratory_environment_versions[0].template_name.toLowerCase().indexOf('deeplearning') === -1
      && template.exploratory_environment_versions[0].template_name.toLowerCase().indexOf('deep learning') === -1
      && template.exploratory_environment_versions[0].template_name.toLowerCase().indexOf('data science') === -1
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
    this.areShapes = !!Object.keys(this.shapes).length;
  }

  public createExploratoryEnvironment(data) {
    const parameters: any = {
      image: this.currentTemplate.image,
      template_name: this.currentTemplate.exploratory_environment_versions[0].template_name
    };

    if (!data.notebook_image_name
      && this.currentTemplate.image === 'docker.datalab-deeplearning'
      && (this.selectedCloud === 'aws' || this.selectedCloud === 'azure')) {
      data.notebook_image_name = this.currentTemplate.exploratory_environment_versions[0].version;
    }

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
    this.additionalParams.configurationNode = !this.additionalParams.configurationNode;
    if (this.additionalParams.configurationNode) {
      const value = (this.additionalParams.configurationNode && this.createExploratoryForm)
        ? JSON.stringify(CLUSTER_CONFIGURATION.SPARK, undefined, 2) : '';
      timer(500).subscribe(_ => {
        document.querySelector('#buttons').scrollIntoView({ block: 'start', behavior: 'smooth' });
      });
      this.createExploratoryForm.controls['cluster_config'].setValue(value);
    }
  }

  public addGpuFields() {
    this.additionalParams.gpu = !this.additionalParams.gpu;
    this.createExploratoryForm.controls['gpu_enabled'].setValue(this.additionalParams.gpu);

    const controls = ['gpu_type', 'gpu_count'];
    if (!this.additionalParams.gpu) {
      controls.forEach(control => {
        this.createExploratoryForm.controls[control].setValue(null);
        this.createExploratoryForm.controls[control].clearValidators();
        this.createExploratoryForm.controls[control].updateValueAndValidity();
      });

    } else {
        controls.forEach(control => {
        this.createExploratoryForm.controls[control].setValidators([Validators.required]);
        this.createExploratoryForm.controls[control].updateValueAndValidity();
      });
      timer(500).subscribe(_ => {
        document.querySelector('#buttons').scrollIntoView({ block: 'center', behavior: 'smooth' });
      });
    }
  }

  public setInstanceSize() {
    const {image, computationGPU} = this.currentTemplate;
    if (image === this.templateName.jupyterJpu) {
      this.createExploratoryForm.get('gpu_count').setValue(computationGPU[0]);
    } else {
        const controls = ['gpu_type', 'gpu_count'];
        controls.forEach(control => {
        this.createExploratoryForm.controls[control].setValue(null);
      });
    }
  }

  public setCount(type: any, gpuType: any): void {
    if (gpuType && this.currentTemplate.image === 'docker.datalab-deeplearning') {
      this.additionalParams.gpu = true;
      this.createExploratoryForm.controls['gpu_enabled'].setValue(this.additionalParams.gpu);
      this.createExploratoryForm.controls['gpu_count'].setValidators([Validators.required]);
      this.createExploratoryForm.controls['gpu_count'].updateValueAndValidity();
    }
    // if (type === 'master') {
      this.gpuCount = [1, 2, 4];
    // } else {
    //   const slaveShape = this.resourceForm.controls['shape_slave'].value;
    //   this.slaveGPUcount = HelpUtils.setGPUCount(slaveShape, gpuType);
    // }
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
        Validators.maxLength(this.maxNotebookLength),
        this.checkDuplication.bind(this)
      ]],
      cluster_config: ['', [this.validConfiguration.bind(this)]],
      custom_tag: ['', [
        Validators.pattern(PATTERNS.namePattern),
        Validators.maxLength(this.maxCustomTagLength)
      ]],
      gpu_type: [null],
      gpu_count: [null],
      gpu_enabled: [false]
    });
  }

  private getImagesList() {
    this.userResourceService.getUserImages(this.currentTemplate.image, this.createExploratoryForm.controls['project'].value,
      this.createExploratoryForm.controls['endpoint'].value)
      .subscribe(
        (res: any) => {
          this.images = res.filter(el => el.status === 'ACTIVE');

          if (this.selectedCloud === 'gcp' && this.currentTemplate.image === 'docker.datalab-deeplearning') {
            this.currentTemplate.exploratory_environment_images = this.currentTemplate.exploratory_environment_images.map(image => {
              return {name: image['Image family'] ?? image.name, description: image['Description'] ?? image.description};
            });
            this.images.push(...this.currentTemplate.exploratory_environment_images);
          }
        },
        error => this.toastr.error(error.message || 'Images list loading failed!', 'Oops!')
      );
  }

  private checkDuplication(control) {
    if (this.createExploratoryForm
      && this.createExploratoryForm.controls.project.value
      && this.resourceGrid.containsNotebook(control.value, this.projectExploratories[this.createExploratoryForm.controls.project.value]))
      return { duplication: true };
  }

  private validConfiguration(control) {
    if (this.additionalParams.configurationNode)
      return this.additionalParams.configurationNode
        ? (control.value && control.value !== null && CheckUtils.isJSON(control.value) ? null : { valid: false })
        : null;
  }
}
