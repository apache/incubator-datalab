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
/* tslint:disable:no-empty */

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { UserResourceService } from '../../../core/services';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Injectable()
export class ComputationalResourceModel {

  constructor(private userResourceService: UserResourceService) { }

  public createComputationalResource(parameters, image, env, spot): Observable<{}> {
    const config = parameters.configuration_parameters ? JSON.parse(parameters.configuration_parameters) : null;

    if (DICTIONARY.cloud_provider === 'aws' && image.image === 'docker.dlab-dataengine-service') {
      return this.userResourceService.createComputationalResource_DataengineService({
        name: parameters.cluster_alias_name,
        emr_instance_count: parameters.instance_number,
        emr_master_instance_type: parameters.shape_master,
        emr_slave_instance_type: parameters.shape_slave,
        emr_version: parameters.version,
        notebook_name: env.name,
        image: image.image,
        template_name: image.template_name,
        emr_slave_instance_spot: spot,
        emr_slave_instance_spot_pct_price: parameters.instance_price,
        config: config,
        project: env.project,
        custom_tag: parameters.custom_tag
      });
    } else if (DICTIONARY.cloud_provider === 'gcp' && image.image === 'docker.dlab-dataengine-service') {
      return this.userResourceService.createComputationalResource_DataengineService({
        template_name: image.template_name,
        image: image.image,
        notebook_name: env.name,
        name: parameters.cluster_alias_name,
        dataproc_master_count: 1,
        dataproc_slave_count: (parameters.instance_number - 1),
        dataproc_preemptible_count: parameters.preemptible_instance_number,
        dataproc_master_instance_type: parameters.shape_master,
        dataproc_slave_instance_type: parameters.shape_slave,
        dataproc_version: parameters.version,
        config: config,
        project: env.project,
        custom_tag: parameters.custom_tag
      });
    } else {
      return this.userResourceService.createComputationalResource_Dataengine({
        name: parameters.cluster_alias_name,
        dataengine_instance_count: parameters.instance_number,
        dataengine_instance_shape: parameters.shape_master,
        notebook_name: env.name,
        image: image.image,
        template_name: image.template_name,
        config: config,
        project: env.project,
        custom_tag: parameters.custom_tag
      });
    }
  }
}
