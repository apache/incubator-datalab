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

import { Component, OnInit, EventEmitter, Output, ViewChild, ChangeDetectorRef } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Response } from '@angular/http';

import { ComputationalResourceCreateModel } from './';
import { UserResourceService } from '../../../core/services';
import { ErrorMapUtils, HTTP_STATUS_CODES } from '../../../core/util';

import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  moduleId: module.id,
  selector: 'computational-resource-create-dialog',
  templateUrl: 'computational-resource-create-dialog.component.html',
  styleUrls: ['./computational-resource-create-dialog.component.css'],
})

export class ComputationalResourceCreateDialogComponent implements OnInit {
  readonly PROVIDER = DICTIONARY.cloud_provider;
  readonly DICTIONARY = DICTIONARY;

  model: ComputationalResourceCreateModel;
  notebook_instance: any;
  full_list: any;
  template_description: string;
  shapes: any;
  spotInstance: boolean = false;
  clusterNamePattern: string = '[-_a-zA-Z0-9]+';
  nodeCountPattern: string = '^[1-9]\\d*$';
  delimitersRegex = /[-_]?/g;
  processError: boolean = false;
  errorMessage: string = '';

  public minInstanceNumber: number;
  public maxInstanceNumber: number;
  public minPreemptibleInstanceNumber: number;
  public maxPreemptibleInstanceNumber: number;
  public minSpotPrice: number = 0;
  public maxSpotPrice: number = 0;
  public resourceForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('name') name;
  @ViewChild('clusterType') cluster_type;
  @ViewChild('templatesList') templates_list;
  @ViewChild('masterShapesList') master_shapes_list;
  @ViewChild('shapesSlaveList') slave_shapes_list;
  @ViewChild('spotInstancesCheck') spotInstancesSelect;
  @ViewChild('preemptibleNode') preemptible;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    private userResourceService: UserResourceService,
    private _fb: FormBuilder,
    private ref: ChangeDetectorRef
  ) {
    this.model = ComputationalResourceCreateModel.getDefault(userResourceService);
  }

  ngOnInit() {
    this.initFormModel();
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  public isNumberKey($event): boolean {
    const charCode = ($event.which) ? $event.which : $event.keyCode;
    if (charCode !== 46 && charCode > 31 && (charCode < 48 || charCode > 57)) {
      $event.preventDefault();
      return false;
    }
    return true;
  }

  public onUpdate($event): void {
    if ($event.model.type === 'template') {
      this.model.setSelectedTemplate($event.model.index);
      this.master_shapes_list.setDefaultOptions(this.model.selectedImage.shapes.resourcesShapeTypes,
        this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'description'), 'master_shape', 'description', 'json');
      this.slave_shapes_list.setDefaultOptions(this.model.selectedImage.shapes.resourcesShapeTypes,
        this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'description'), 'slave_shape', 'description', 'json');

      this.shapes.master_shape = this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'type');
      this.shapes.slave_shape = this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'type');
    }
    if ($event.model.type === 'cluster_type') {
      this.model.setSelectedClusterType($event.model.index);
      this.setDefaultParams();
      this.getComputationalResourceLimits();
    }

    if (this.shapes[$event.model.type])
      this.shapes[$event.model.type] = $event.model.value.type;

    if (DICTIONARY.cloud_provider === 'aws')
      if ($event.model.type === 'slave_shape' && this.spotInstancesSelect.nativeElement['checked']) {
        this.spotInstance = $event.model.value.spot;
      }
  }

  public createComputationalResource($event, data, shape_master: string, shape_slave: string) {
    this.model.setCreatingParams(data.cluster_alias_name, data.instance_number, shape_master, shape_slave,
      this.spotInstance, data.instance_price, data.preemptible_instance_number);
    this.model.confirmAction();
    $event.preventDefault();
    return false;
  }

  public containsComputationalResource(conputational_resource_name: string): boolean {
    if (conputational_resource_name)
      for (let index = 0; index < this.full_list.length; index++) {
        if (this.notebook_instance.name === this.full_list[index].name) {
          for (let iindex = 0; iindex < this.full_list[index].resources.length; iindex++) {
            const computational_name = this.full_list[index].resources[iindex].computational_name.toString().toLowerCase();
            if (this.delimitersFiltering(conputational_resource_name) === this.delimitersFiltering(computational_name))
              return true;
          }
        }
      }
    return false;
  }

  public delimitersFiltering(resource): string {
    return resource.replace(this.delimitersRegex, '').toString().toLowerCase();
  }

  public selectSpotInstances($event): void {
    if ($event.target.checked) {
      const filtered = this.filterAvailableSpots();

      this.slave_shapes_list.setDefaultOptions(filtered, this.shapePlaceholder(filtered, 'description'),
        'slave_shape', 'description', 'json');
      this.shapes.slave_shape = this.shapePlaceholder(filtered, 'type');

      this.spotInstance = this.shapePlaceholder(filtered, 'spot');
      this.resourceForm.controls['instance_price'].setValue(this.shapePlaceholder(filtered, 'price'));
    } else {
      this.slave_shapes_list.setDefaultOptions(this.model.selectedImage.shapes.resourcesShapeTypes,
        this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'description'), 'slave_shape', 'description', 'json');
      this.shapes.slave_shape = this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'type');

      this.spotInstance = false;
      this.resourceForm.controls['instance_price'].setValue(0);
    }
  }

  public selectPreemptibleNodes($event) {
    if ($event.target.checked)
      this.resourceForm.controls['preemptible_instance_number'].setValue(this.minPreemptibleInstanceNumber);
  }

  private filterAvailableSpots() {
    const filtered = JSON.parse(JSON.stringify(this.slave_shapes_list.items));
    for (const item in this.slave_shapes_list.items) {
        filtered[item] = filtered[item].filter(el => el.spot);
        if (filtered[item].length <= 0) {
          delete filtered[item];
        }
    }
    return filtered;
  }

  public isAvailableSpots(): boolean {
    if (this.slave_shapes_list && this.slave_shapes_list.items)
      return !!Object.keys(this.filterAvailableSpots()).length;

    return false;
  }

  public open(params, notebook_instance, full_list): void {
    if (!this.bindDialog.isOpened) {
      this.notebook_instance = notebook_instance;
      this.full_list = full_list;
      this.model = new ComputationalResourceCreateModel('', 0, '', '', notebook_instance.name, (response: Response) => {
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
          this.template_description = this.model.selectedItem.description;
        },
        () => {
          this.bindDialog.open(params);
          this.ref.detectChanges();

          this.setDefaultParams();
          this.getComputationalResourceLimits();
        },
        this.userResourceService);
    }
  }

  public close(): void {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  private initFormModel(): void {
    this.resourceForm = this._fb.group({
      cluster_alias_name: ['', [Validators.required, Validators.pattern(this.clusterNamePattern), this.providerMaxLength, this.checkDuplication.bind(this)]],
      instance_number: ['', [Validators.required, Validators.pattern(this.nodeCountPattern), this.validInstanceNumberRange.bind(this)]],
      preemptible_instance_number: [0, [this.validPreemptibleRange.bind(this)]],
      instance_price: [0, [this.validInstanceSpotRange.bind(this)]]
    });
  }

  private shapePlaceholder(resourceShapes, byField: string) {
    for (const index in resourceShapes) return resourceShapes[index][0][byField];
  }

  private getComputationalResourceLimits(): void {
    let activeImage = DICTIONARY[this.model.selectedImage.image];

    if (this.model.selectedImage) {
      this.minInstanceNumber = this.model.selectedImage.limits[activeImage.total_instance_number_min];
      this.maxInstanceNumber = this.model.selectedImage.limits[activeImage.total_instance_number_max];

      if (DICTIONARY.cloud_provider === 'gcp'&& this.model.selectedImage.image === 'docker.dlab-dataengine-service') {
        this.maxInstanceNumber = this.model.selectedImage.limits[activeImage.total_instance_number_max] - 1;
        this.minPreemptibleInstanceNumber = this.model.selectedImage.limits.min_dataproc_preemptible_instance_count;
      }

      if (DICTIONARY.cloud_provider === 'aws' && this.model.selectedImage.image === 'docker.dlab-dataengine-service') {
        this.minSpotPrice = this.model.selectedImage.limits.min_emr_spot_instance_bid_pct;
        this.maxSpotPrice = this.model.selectedImage.limits.max_emr_spot_instance_bid_pct;
      }

      this.resourceForm.controls['instance_number'].setValue(this.minInstanceNumber);
      this.resourceForm.controls['preemptible_instance_number'].setValue(this.minPreemptibleInstanceNumber);
    }
  }

  private validInstanceNumberRange(control) {
    if (control && control.value)
      if (DICTIONARY.cloud_provider === 'gcp'&& this.model.selectedImage.image === 'docker.dlab-dataengine-service') {
        this.validPreemptibleNumberRange();
        return control.value >= this.minInstanceNumber && control.value <= this.maxInstanceNumber ? null : { valid: false };
      } else {
        return control.value >= this.minInstanceNumber && control.value <= this.maxInstanceNumber ? null : { valid: false };
      }
  }

  private validPreemptibleRange(control) {
    if (this.preemptible)
      return this.preemptible.nativeElement['checked']
        ? (control.value !== null && control.value >= this.minPreemptibleInstanceNumber && control.value <= this.maxPreemptibleInstanceNumber ? null : { valid: false })
        : control.value;
  }

  private validPreemptibleNumberRange() {
    let instance_value = this.resourceForm.controls['instance_number'].value;
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

  private checkDuplication(control) {
    if (this.containsComputationalResource(control.value))
      return { duplication: true }
  }

  private providerMaxLength(control) {
    if (DICTIONARY.cloud_provider !== 'aws')
      return control.value.length <=10 ? null : { valid: false };
  }

  private setDefaultParams(): void {
    this.filterShapes();
    this.shapes = {
      master_shape: this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'type'),
      slave_shape: this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'type')
    };
    if (DICTIONARY.cloud_provider !== 'azure') {
      this.cluster_type.setDefaultOptions(this.model.resourceImages,
        this.model.selectedImage.template_name, 'cluster_type', 'template_name', 'array');
        if (this.model.selectedImage.image === 'docker.dlab-dataengine-service')
          this.templates_list.setDefaultOptions(this.model.templates,
            this.model.selectedItem.version, 'template', 'version', 'array');
    }
    this.master_shapes_list.setDefaultOptions(this.model.selectedImage.shapes.resourcesShapeTypes,
      this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'description'), 'master_shape', 'description', 'json');
    this.slave_shapes_list.setDefaultOptions(this.model.selectedImage.shapes.resourcesShapeTypes,
      this.shapePlaceholder(this.model.selectedImage.shapes.resourcesShapeTypes, 'description'), 'slave_shape', 'description', 'json');
  }

  private filterShapes(): void {
    if (this.notebook_instance.template_name.toLowerCase().indexOf('tensorflow') !== -1
      || this.notebook_instance.template_name.toLowerCase().indexOf('deep learning') !== -1) {
      const allowed = ['GPU optimized'];
      const filtered = Object.keys(this.model.selectedImage.shapes.resourcesShapeTypes)
        .filter(key => allowed.includes(key))
        .reduce((obj, key) => {
          obj[key] = this.model.selectedImage.shapes.resourcesShapeTypes[key];
          return obj;
        }, {});

      if (DICTIONARY.cloud_provider !== 'azure') {
        this.model.resourceImages = this.model.resourceImages.filter(image => image.image === 'docker.dlab-dataengine');
        this.model.setSelectedClusterType(0);
      }
      this.model.selectedImage.shapes.resourcesShapeTypes = filtered;
    }
  }

  private resetDialog(): void {
    this.processError = false;
    this.errorMessage = '';

    this.spotInstance = false;
    this.initFormModel();
    this.getComputationalResourceLimits();
    this.model.resetModel();

    if (this.PROVIDER === 'aws')
      this.spotInstancesSelect.nativeElement['checked'] = false;

    if (this.PROVIDER === 'gcp' && this.preemptible)
      this.preemptible.nativeElement['checked'] = false;
  }
}
