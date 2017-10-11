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
/* tslint:disable:no-empty */

import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';

import { UserResourceService } from '../../../core/services';
import { ComputationalResourceImage,
         ComputationalResourceApplicationTemplate,
         ResourceShapeTypesModel } from '../../../core/models';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

export class ComputationalResourceCreateModel {

  confirmAction: Function;
  selectedItemChanged: Function;

  computational_resource_alias: string;
  computational_resource_count: number;
  computational_resource_master_shape: string;
  computational_resource_slave_shape: string;
  notebook_name: string;
  emr_slave_instance_spot: boolean;
  emr_slave_instance_price: number;

  selectedItem: ComputationalResourceApplicationTemplate = new ComputationalResourceApplicationTemplate({},
    new ResourceShapeTypesModel({}), '', '', '');
  selectedImage: ComputationalResourceImage;
  resourceImages: Array<ComputationalResourceImage> = [];
  templates: Array<ComputationalResourceApplicationTemplate> = [];

  private userResourceService: UserResourceService;
  private continueWith: Function;

  static getDefault(userResourceService): ComputationalResourceCreateModel {
    return new ComputationalResourceCreateModel('', 0, '', '', '', () => { }, () => { }, null, null, userResourceService);
  }

  constructor(
    computational_resource_alias: string,
    computational_resource_count: number,
    computational_resource_master_shape: string,
    computational_resource_slave_shape: string,
    notebook_name: string,
    fnProcessResults: any,
    fnProcessErrors: any,
    selectedItemChanged: Function,
    continueWith: Function,
    userResourceService: UserResourceService
  ) {
    this.notebook_name = notebook_name;
    this.userResourceService = userResourceService;
    this.selectedItemChanged = selectedItemChanged;
    this.continueWith = continueWith;
    this.prepareModel(fnProcessResults, fnProcessErrors);
    this.loadTemplates();
  }

  public setSelectedItem(item: ComputationalResourceApplicationTemplate) {
    this.selectedItem = item;
  }

  public setCreatingParams(name: string, count: number, shape_master: string, shape_slave: string, spot: boolean, price: number): void {
    this.computational_resource_alias = name;
    this.computational_resource_count = count;
    this.computational_resource_master_shape = shape_master;
    this.computational_resource_slave_shape = shape_slave;
    this.emr_slave_instance_spot = spot;
    this.emr_slave_instance_price = price;
  }

  public loadTemplates(): void {
    if (this.resourceImages.length === 0)
      this.userResourceService.getComputationalResourcesTemplates()
        .subscribe(
        data => {
          let computationalResourceImage;

          for (let parentIndex = 0; parentIndex < data.computational_templates.length; parentIndex++) {
            computationalResourceImage = new ComputationalResourceImage(data.computational_templates[parentIndex]);
            
            if (DICTIONARY.cloud_provider === 'aws')
              this.resourceImages.push(computationalResourceImage);
          }
          
          if (this.resourceImages.length > 0 && DICTIONARY.cloud_provider === 'aws') {
            this.setSelectedClusterType(0);
          } else if (DICTIONARY.cloud_provider !== 'aws') {
            this.selectedItem = computationalResourceImage;
            this.selectedImage = computationalResourceImage;
          }

          if (this.continueWith)
            this.continueWith();
        });
  }

  public setSelectedClusterType(index) {
    this.selectedImage = this.resourceImages[index];
    this.templates = [];
    
    for (let index = 0; index < this.selectedImage.application_templates.length; index++)
      this.templates.push(this.selectedImage.application_templates[index]);

    this.setSelectedTemplate(0);
  }

  public setSelectedTemplate(index: number): void {
    if (this.templates && this.templates[index]) {
      this.selectedItem = this.templates[index];
      if (this.selectedItemChanged)
        this.selectedItemChanged();
    } else {
      this.selectedItem = null;
    }
  }

  public resetModel() {
    this.setSelectedTemplate(0);
  }

  private prepareModel(fnProcessResults: any, fnProcessErrors: any): void {
    this.confirmAction = () => this.createComputationalResource()
      .subscribe(
      (response: Response) => fnProcessResults(response),
      (response: Response) => fnProcessErrors(response)
      );
  }

  private createComputationalResource(): Observable<Response> {
    if (DICTIONARY.cloud_provider === 'aws' && this.selectedItem.image === 'docker.dlab-dataengine-service') {
      return this.userResourceService.createComputationalResource_DataengineService({
        name: this.computational_resource_alias,
        emr_instance_count: this.computational_resource_count,
        emr_master_instance_type: this.computational_resource_master_shape,
        emr_slave_instance_type: this.computational_resource_slave_shape,
        emr_version: this.selectedItem.version,
        notebook_name: this.notebook_name,
        image: this.selectedItem.image,
        template_name: this.selectedItem.template_name,
        emr_slave_instance_spot: this.emr_slave_instance_spot,
        emr_slave_instance_spot_pct_price: this.emr_slave_instance_price
      });
    } else {
      return this.userResourceService.createComputationalResource_Dataengine({
        name: this.computational_resource_alias,
        dataengine_instance_count: this.computational_resource_count,
        dataengine_master: this.computational_resource_master_shape,
        dataengine_slave: this.computational_resource_slave_shape,
        notebook_name: this.notebook_name,
        image: this.selectedItem.image,
        template_name: this.selectedItem.template_name,
      });
    }
  };
}
