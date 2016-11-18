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

import { ImageType } from "./imageType.enum";
import { ResourceShapeModel } from './resourceShape.model';

export class ExploratoryEnvironmentVersionModel {
  template_name: string;
  description: string;
  environment_type: ImageType;
  version: string;
  vendor: string;

  shapes: Array<ResourceShapeModel>;

  constructor(jsonModel:any, shapes: Array<ResourceShapeModel>) {
    this.template_name = jsonModel.template_name;
    this.description = jsonModel.description;
    this.environment_type = ImageType.EXPLORATORY;
    this.version = jsonModel.version;
    this.vendor = jsonModel.vendor;
    this.shapes = shapes;
  }
}
