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

import { ResourceShapeModel } from "./resourceShape.model";
import { ComputationalResourceApplicationTemplate } from "./computationalResourceApplicationTemplate.model";
import { ImageType } from "./imageType.enum";

export class ComputationalResourceImage {
  template_name: string;
  description: string;
  environment_type: ImageType;
  shapes: Array<ResourceShapeModel>;
  application_templates: Array<ComputationalResourceApplicationTemplate>

  constructor(jsonModel:any) {
    this.template_name = jsonModel.template_name;
    this.description = jsonModel.description;
    this.environment_type = ImageType.СOMPUTATIONAL;
    this.application_templates = [];
    this.shapes = [];

    if(jsonModel.computation_resources_shapes && jsonModel.computation_resources_shapes.length > 0)
      for (let index = 0; index < jsonModel.computation_resources_shapes.length; index++)
        this.shapes.push(new ResourceShapeModel(jsonModel.computation_resources_shapes[index]));

    if(jsonModel.templates && jsonModel.templates.length > 0)
      for (let index = 0; index < jsonModel.templates.length; index++)
        this.application_templates.push(new ComputationalResourceApplicationTemplate(jsonModel.templates[index], this.shapes));

  }
}
