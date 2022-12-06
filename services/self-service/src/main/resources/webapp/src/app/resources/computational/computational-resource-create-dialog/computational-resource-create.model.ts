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

@Injectable()
export class ComputationalResourceModel {

  constructor(private userResourceService: UserResourceService) { }

  public createComputationalResource(parameters, image, env, provider, gpu?): Observable<{}> {
    const config = parameters.configuration_parameters ? JSON.parse(parameters.configuration_parameters) : null;
    const requestObj = this.createRequestObject(parameters, image, env, provider, config, gpu);
    const dataEngineServiceCondition = (provider === 'aws' && image.image === 'docker.datalab-dataengine-service')
      || (provider === 'gcp' && image.image === 'docker.datalab-dataengine-service')
      || (provider === 'azure' && image.template_name === 'HDInsight cluster');

    if (dataEngineServiceCondition) {
      return this.userResourceService.createComputationalResource_DataengineService(requestObj, provider);
    } else {
      return this.userResourceService.createComputationalResource_Dataengine(requestObj, provider);
    }
  }

  private createRequestObject(parameters, image, env, provider, config, gpu?) {
    if (provider === 'aws' && image.image === 'docker.datalab-dataengine-service') {
      return {
        name: parameters.cluster_alias_name,
        emr_instance_count: parameters.instance_number,
        emr_master_instance_type: parameters.shape_master,
        emr_slave_instance_type: parameters.shape_slave,
        emr_version: parameters.version,
        notebook_name: env.name,
        image: image.image,
        template_name: image.template_name,
        emr_slave_instance_spot: parameters.emr_slave_instance_spot,
        emr_slave_instance_spot_pct_price: parameters.instance_price,
        config: config,
        project: env.project,
        custom_tag: parameters.custom_tag
      };
    } else if (provider === 'gcp' && image.image === 'docker.datalab-dataengine-service') {
      return {
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
        custom_tag: parameters.custom_tag,
        master_gpu_type: gpu ? parameters.master_GPU_type : null,
        slave_gpu_type: gpu ? parameters.slave_GPU_type : null,
        master_gpu_count: gpu ? parameters.master_GPU_count : null,
        slave_gpu_count: gpu ? parameters.slave_GPU_count : null,
        gpu_enabled: gpu
      };
    } else if (provider === 'azure' && image.template_name === 'HDInsight cluster') {
      return {
        template_name: image.template_name,
        image: image.image,
        name: parameters.cluster_alias_name,
        project: env.project,
        custom_tag: parameters.custom_tag,
        notebook_name: env.name,
        config: config,
        hdinsight_master_instance_type: parameters.shape_master,
        hdinsight_slave_instance_type: parameters.shape_slave,
        hdinsight_version: parameters.version,
        hdinsight_instance_count: parameters.instance_number ,
      };
    } else {
      return {
        name: parameters.cluster_alias_name,
        dataengine_instance_count: parameters.instance_number,
        master_instance_shape: parameters.shape_master,
        slave_instance_shape: parameters.shape_slave,
        gpu_enabled: gpu,
        master_gpu_type: gpu ? parameters.master_GPU_type : null,
        slave_gpu_type: gpu ? parameters.slave_GPU_type : null,
        master_gpu_count: gpu ? parameters.master_GPU_count : null,
        slave_gpu_count: gpu ? parameters.slave_GPU_count : null,
        notebook_name: env.name,
        image: image.image,
        template_name: image.template_name,
        config: config,
        project: env.project,
        custom_tag: parameters.custom_tag
      };
    }
  }
}
