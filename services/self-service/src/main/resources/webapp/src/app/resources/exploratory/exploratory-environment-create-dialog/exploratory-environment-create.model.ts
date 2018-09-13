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

import { ExploratoryEnvironmentVersionModel, ResourceShapeTypesModel } from '../../../core/models';
import { UserResourceService } from '../../../core/services';
import { SortUtil } from '../../../core/util';

export class ExploratoryEnvironmentCreateModel {

  confirmAction: Function;
  selectedItemChanged: Function;

  selectedItem: ExploratoryEnvironmentVersionModel = new ExploratoryEnvironmentVersionModel('', {}, new ResourceShapeTypesModel({}));
  exploratoryEnvironmentTemplates: Array<ExploratoryEnvironmentVersionModel> = [];

  public notebookImage: any;
  private environment_image: string;
  private environment_name: string;
  private environment_template_name: string;
  private environment_version: string;
  private environment_shape: string;
  private userResourceService: UserResourceService;
  private continueWith: Function;

  static getDefault(userResourceService): ExploratoryEnvironmentCreateModel {
    return new ExploratoryEnvironmentCreateModel('', '', '', '', '', () => { }, () => { }, null, null, userResourceService);
  }

  constructor(
    environment_image: string,
    environment_name: string,
    environment_template_name: string,
    environment_version: string,
    environment_shape: string,
    fnProcessResults: any,
    fnProcessErrors: any,
    selectedItemChanged: Function,
    continueWith: Function,
    userResourceService: UserResourceService
  ) {
    this.userResourceService = userResourceService;
    this.selectedItemChanged = selectedItemChanged;
    this.continueWith = continueWith;
    this.prepareModel(
      environment_image,
      environment_name,
      environment_template_name,
      environment_version,
      environment_shape,
      fnProcessResults,
      fnProcessErrors);
    this.loadTemplates();
  }

  public setSelectedItem(item: ExploratoryEnvironmentVersionModel): void {
    this.selectedItem = item;
  }

  public setSelectedTemplate(index): void {
    if (this.exploratoryEnvironmentTemplates && this.exploratoryEnvironmentTemplates[index]) {
      this.selectedItem = this.exploratoryEnvironmentTemplates[index];
      if (this.selectedItemChanged)
        this.selectedItemChanged();
    }
  }

  public setCreatingParams(name, shape): void {
    this.environment_image = this.selectedItem.image;
    this.environment_version = this.selectedItem.version;
    this.environment_template_name = this.selectedItem.template_name;
    this.environment_name = name;
    this.environment_shape = shape;
  }

  public loadTemplates(): void {
    if (this.exploratoryEnvironmentTemplates.length === 0)
      this.userResourceService.getExploratoryEnvironmentTemplates()
        .subscribe(
        data => {
          for (let parentIndex = 0; parentIndex < data.length; parentIndex++) {

            const shapeJson = data[parentIndex].exploratory_environment_shapes;
            const exploratoryJson = data[parentIndex].exploratory_environment_versions;
            const shapeObj: ResourceShapeTypesModel = new ResourceShapeTypesModel(SortUtil.shapesSort(shapeJson));

            for (let index = 0; index < exploratoryJson.length; index++)
              this.exploratoryEnvironmentTemplates.push(
                new ExploratoryEnvironmentVersionModel(data[parentIndex].image, exploratoryJson[index], shapeObj));
          }
          if (this.exploratoryEnvironmentTemplates.length > 0) {
            this.exploratoryEnvironmentTemplates.sort(function(t1, t2) {
              return ((t1.template_name < t2.template_name) ? -1 : ((t1.template_name > t2.template_name) ? 1 : 0));
            });
            this.setSelectedTemplate(0);
          }

          if (this.continueWith)
            this.continueWith();
        });
  }

  public resetModel(): void {
    this.setSelectedTemplate(0);
  }

  private createExploratoryEnvironment(): Observable<{}> {
    let params: any = {
      image: this.environment_image,
      template_name: this.environment_template_name,
      name: this.environment_name,
      shape: this.environment_shape,
      version: this.environment_version
    };
    if (this.notebookImage)
      params.notebook_image_name = this.notebookImage;

    return this.userResourceService.createExploratoryEnvironment(params);
  }

  private prepareModel(
    environment_image: string,
    environment_name: string,
    environment_template_name: string,
    environment_version: string,
    environment_shape: string,
    fnProcessResults: any, fnProcessErrors: any): void {

    this.setCreatingParams(environment_name, environment_shape);
    this.confirmAction = () => this.createExploratoryEnvironment()
      .subscribe(
        response => fnProcessResults(response),
        error => fnProcessErrors(error));
  }
}
