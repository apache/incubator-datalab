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
    new ResourceShapeTypesModel({}));
  computationalResourceImages: Array<ComputationalResourceImage> = [];
  computationalResourceApplicationTemplates: Array<ComputationalResourceApplicationTemplate> = [];

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
    if (this.computationalResourceImages.length === 0)
      this.userResourceService.getComputationalResourcesTemplates()
        .subscribe(
        data => {
          for (let parentIndex = 0; parentIndex < data.length; parentIndex++) {
            let computationalResourceImage = new ComputationalResourceImage(data[parentIndex]);
            this.computationalResourceImages.push(computationalResourceImage);

            for (let index = 0; index < computationalResourceImage.application_templates.length; index++)
              this.computationalResourceApplicationTemplates.push(computationalResourceImage.application_templates[index]);
          }
          if (this.computationalResourceImages.length > 0)
            this.setSelectedTemplate(0);

          if (this.continueWith)
            this.continueWith();
        });
  }

  public setSelectedTemplate(index: number): void {
    if (this.computationalResourceApplicationTemplates && this.computationalResourceApplicationTemplates[index]) {
      this.selectedItem = this.computationalResourceApplicationTemplates[index];
      if (this.selectedItemChanged)
        this.selectedItemChanged();
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
    return this.userResourceService.createComputationalResource({
      name: this.computational_resource_alias,
      emr_instance_count: this.computational_resource_count,
      emr_master_instance_type: this.computational_resource_master_shape,
      emr_slave_instance_type: this.computational_resource_slave_shape,
      emr_version: this.selectedItem.version,
      notebook_name: this.notebook_name,
      emr_slave_instance_spot: this.emr_slave_instance_spot,
      emr_slave_instance_spot_pct_price: this.emr_slave_instance_price
    });
  };
}
