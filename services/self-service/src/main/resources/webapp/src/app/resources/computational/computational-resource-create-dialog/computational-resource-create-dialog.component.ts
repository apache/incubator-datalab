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

import { Component, OnInit, ViewChild, Inject, ChangeDetectorRef } from '@angular/core';
import {FormGroup, FormBuilder, Validators, FormControl} from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { ComputationalResourceModel } from './computational-resource-create.model';
import { UserResourceService } from '../../../core/services';
import { HTTP_STATUS_CODES, PATTERNS, CheckUtils, SortUtils } from '../../../core/util';

import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { CLUSTER_CONFIGURATION } from './cluster-configuration-templates';

@Component({
  selector: 'computational-resource-create-dialog',
  templateUrl: 'computational-resource-create-dialog.component.html',
  styleUrls: ['./computational-resource-create-dialog.component.scss']
})

export class ComputationalResourceCreateDialogComponent implements OnInit {
  readonly PROVIDER = this.data.notebook.cloud_provider;
  readonly DICTIONARY = DICTIONARY;
  readonly CLUSTER_CONFIGURATION = CLUSTER_CONFIGURATION;
  readonly CheckUtils = CheckUtils;

  notebook_instance: any;
  resourcesList: any;
  clusterTypes = [];
  userComputations = [];
  projectComputations = [];
  selectedImage: any;
  spotInstance: boolean = true;

  loading: boolean = false;

  public minInstanceNumber: number;
  public maxInstanceNumber: number;
  public minPreemptibleInstanceNumber: number;
  public maxPreemptibleInstanceNumber: number;
  public minSpotPrice: number = 0;
  public maxSpotPrice: number = 0;
  public resourceForm: FormGroup;

  @ViewChild('spotInstancesCheck', { static: false }) spotInstancesSelect;
  @ViewChild('preemptibleNode', { static: false }) preemptible;
  @ViewChild('configurationNode', { static: false }) configuration;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<ComputationalResourceCreateDialogComponent>,
    private userResourceService: UserResourceService,
    private model: ComputationalResourceModel,
    private _fb: FormBuilder,
    private _ref: ChangeDetectorRef,
  ) { }

  ngOnInit() {
    this.loading = true;
    this.notebook_instance = this.data.notebook;
    this.resourcesList = this.data.full_list;
    this.initFormModel();
    this.getTemplates(this.notebook_instance.project, this.notebook_instance.endpoint, this.notebook_instance.cloud_provider);
  }

  public selectImage($event) {
    this.selectedImage = $event;
    this.getComputationalResourceLimits();

    if ($event.templates && $event.templates.length)
      this.resourceForm.controls['version'].setValue($event.templates[0].version);
  }

  public selectSpotInstances($event?): void {
    if ($event ? $event.target.checked : (this.spotInstancesSelect && this.spotInstancesSelect.nativeElement['checked'])) {
      this.spotInstance = true;
      this.resourceForm.controls['instance_price'].setValue(50);
    } else {
      this.spotInstance = false;
      this.resourceForm.controls['instance_price'].setValue(0);
    }
  }

  public selectPreemptibleNodes($event) {
    if ($event.target.checked)
      this.resourceForm.controls['preemptible_instance_number'].setValue(this.minPreemptibleInstanceNumber);
  }

  public selectConfiguration() {
    if (this.configuration && this.configuration.nativeElement.checked) {
      const template = (this.selectedImage.image === 'docker.dlab-dataengine-service')
        ? CLUSTER_CONFIGURATION.EMR
        : CLUSTER_CONFIGURATION.SPARK;
      this.resourceForm.controls['configuration_parameters'].setValue(JSON.stringify(template, undefined, 2));
    } else {
      this.resourceForm.controls['configuration_parameters'].setValue('');
    }
  }

  public preemptibleCounter($event, action): void {
    $event.preventDefault();

    const value = this.resourceForm.controls['preemptible_instance_number'].value;
    const newValue = (action === 'increment' ? Number(value) + 1 : Number(value) - 1);
    this.resourceForm.controls.preemptible_instance_number.setValue(newValue);
  }

  public isAvailableSpots(): boolean {
    if (this.PROVIDER === 'aws' && this.selectedImage.image === 'docker.dlab-dataengine-service')
      return !!Object.keys(this.filterAvailableSpots()).length;

    return false;
  }

  public createComputationalResource(data) {
    this.model.createComputationalResource(data, this.selectedImage, this.notebook_instance, this.spotInstance, this.PROVIDER.toLowerCase())
      .subscribe((response: any) => {
        if (response.status === HTTP_STATUS_CODES.OK) this.dialogRef.close();
      }, error => this.toastr.error(error.message, 'Oops!'));
  }

  private initFormModel(): void {
    this.resourceForm = this._fb.group({
      template_name: ['', [Validators.required]],
      version: [''],
      shape_master: ['', Validators.required],
      shape_slave: [''],
      cluster_alias_name: ['', [Validators.required, Validators.pattern(PATTERNS.namePattern), Validators.maxLength(DICTIONARY[this.PROVIDER].max_cluster_name_length),
      this.checkDuplication.bind(this)]],
      instance_number: ['', [Validators.required, Validators.pattern(PATTERNS.nodeCountPattern), this.validInstanceNumberRange.bind(this)]],
      preemptible_instance_number: [0,
        Validators.compose([Validators.pattern(PATTERNS.integerRegex),
        this.validPreemptibleRange.bind(this)])],
      instance_price: [0, [this.validInstanceSpotRange.bind(this)]],
      configuration_parameters: ['', [this.validConfiguration.bind(this)]],
      custom_tag: [this.notebook_instance.tags.custom_tag]
    });
  }

  private shapePlaceholder(resourceShapes, byField: string) {
    for (const index in resourceShapes) return resourceShapes[index][0][byField];
  }

  private getComputationalResourceLimits(): void {
    if (this.selectedImage && this.selectedImage.image) {
      const activeImage = DICTIONARY[this.PROVIDER][this.selectedImage.image];

      this.minInstanceNumber = this.selectedImage.limits[activeImage.total_instance_number_min];
      this.maxInstanceNumber = this.selectedImage.limits[activeImage.total_instance_number_max];

      if (this.PROVIDER === 'gcp' && this.selectedImage.image === 'docker.dlab-dataengine-service') {
        this.maxInstanceNumber = this.selectedImage.limits[activeImage.total_instance_number_max] - 1;
        this.minPreemptibleInstanceNumber = this.selectedImage.limits.min_dataproc_preemptible_instance_count;
      }

      if (this.PROVIDER === 'aws' && this.selectedImage.image === 'docker.dlab-dataengine-service') {
        this.minSpotPrice = this.selectedImage.limits.min_emr_spot_instance_bid_pct;
        this.maxSpotPrice = this.selectedImage.limits.max_emr_spot_instance_bid_pct;

        if (this.spotInstancesSelect) this.spotInstancesSelect.nativeElement['checked'] = true;
        this.selectSpotInstances();
      }

      this.resourceForm.controls['instance_number'].setValue(this.minInstanceNumber);
      this.resourceForm.controls['preemptible_instance_number'].setValue(this.minPreemptibleInstanceNumber);
    }
  }

  //  Validation
  private validInstanceNumberRange(control) {
    if (control && control.value)
      if (this.PROVIDER === 'gcp' && this.selectedImage.image === 'docker.dlab-dataengine-service') {
        this.validPreemptibleNumberRange();
        return control.value >= this.minInstanceNumber && control.value <= this.maxInstanceNumber ? null : { valid: false };
      } else {
        return control.value >= this.minInstanceNumber && control.value <= this.maxInstanceNumber ? null : { valid: false };
      }
  }

  private validPreemptibleRange(control) {
    if (this.preemptible)
      return this.preemptible.nativeElement['checked']
        ? (control.value !== null
          && control.value >= this.minPreemptibleInstanceNumber
          && control.value <= this.maxPreemptibleInstanceNumber ? null : { valid: false })
        : control.value;
  }

  private validPreemptibleNumberRange() {
    const instance_value = this.resourceForm.controls['instance_number'].value;
    this.maxPreemptibleInstanceNumber = Math.max((this.maxInstanceNumber - instance_value), 0);

    const value = this.resourceForm.controls['preemptible_instance_number'].value;
    if (value !== null && value >= this.minPreemptibleInstanceNumber && value <= this.maxPreemptibleInstanceNumber) {
      this.resourceForm.controls['preemptible_instance_number'].setErrors(null);
    } else {
      this.resourceForm.controls['preemptible_instance_number'].setErrors({ valid: false });
    }
  }

  private validInstanceSpotRange(control) {
    if (this.spotInstancesSelect)
      return this.spotInstancesSelect.nativeElement['checked']
        ? (control.value >= this.minSpotPrice && control.value <= this.maxSpotPrice ? null : { valid: false })
        : control.value;
  }

  private validConfiguration(control) {
    if (this.configuration)
      return this.configuration.nativeElement['checked']
        ? (control.value && control.value !== null && CheckUtils.isJSON(control.value) ? null : { valid: false })
        : null;
  }

  private checkDuplication(control) {
    if (this.containsComputationalResource(control.value, this.userComputations)){
      return { 'user-duplication': true };
    }

    if (this.containsComputationalResource(control.value, this.projectComputations)){
      return { 'other-user-duplication': true };
    }
  }

  private getTemplates(project, endpoint, provider) {
    this.userResourceService.getComputationalTemplates(project, endpoint, provider).subscribe(
      clusterTypes => {
        this.clusterTypes = clusterTypes.templates;
        this.userComputations = clusterTypes.user_computations;
        this.projectComputations = clusterTypes.project_computations;

        this.clusterTypes.forEach((cluster, index) => this.clusterTypes[index].computation_resources_shapes = SortUtils.shapesSort(cluster.computation_resources_shapes));
        this.selectedImage = clusterTypes.templates[0];

        if (this.selectedImage) {
          this._ref.detectChanges();

          this.filterShapes();
          this.resourceForm.get('template_name').setValue(this.selectedImage.template_name);
          this.getComputationalResourceLimits();
        }

        this.loading = false;
      }, () => this.loading = false);
  }

  private filterShapes(): void {
    if (this.notebook_instance.template_name.toLowerCase().indexOf('tensorflow') !== -1
      || this.notebook_instance.template_name.toLowerCase().indexOf('deep learning') !== -1) {
      const allowed: any = ['GPU optimized'];
      const filtered = Object.keys(
        SortUtils.shapesSort(this.selectedImage.computation_resources_shapes))
        .filter(key => allowed.includes(key))
        .reduce((obj, key) => {
          obj[key] = this.selectedImage.computation_resources_shapes[key];
          return obj;
        }, {});

      if (this.PROVIDER !== 'azure') {
        const images = this.clusterTypes.filter(image => image.image === 'docker.dlab-dataengine');
        this.clusterTypes = images;
        this.selectedImage = this.clusterTypes[0];
      }
      this.selectedImage.computation_resources_shapes = filtered;
    }
  }

  private filterAvailableSpots() {
    const filtered = JSON.parse(JSON.stringify(this.selectedImage.computation_resources_shapes));
    for (const item in this.selectedImage.computation_resources_shapes) {
      filtered[item] = filtered[item].filter(el => el.spot);
      if (filtered[item].length <= 0) {
        delete filtered[item];
      }
    }
    return filtered;
  }

  private containsComputationalResource(conputational_resource_name: string, existNames: Array<string>): boolean {
    if (conputational_resource_name) {
      return existNames.some(resource =>
        CheckUtils.delimitersFiltering(conputational_resource_name) === CheckUtils.delimitersFiltering(resource));
    }
  }
}
