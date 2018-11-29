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

import { Component, ViewChild } from '@angular/core';
import { DateUtils } from '../../../core/util';
import { FormGroup, FormBuilder } from '@angular/forms';

import { CheckUtils } from '../../../core/util';
import { DataengineConfigurationService } from '../../../core/services';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { CLUSTER_CONFIGURATION } from '../computational-resource-create-dialog/cluster-configuration-templates';

@Component({
  selector: 'dlab-cluster-details',
  templateUrl: 'cluster-details.component.html',
  styleUrls: ['./cluster-details.component.scss']
})

export class DetailComputationalResourcesComponent {
  readonly DICTIONARY = DICTIONARY;

  resource: any;
  environment: any;
  @ViewChild('bindDialog') bindDialog;
  @ViewChild('configurationNode') configuration;

  upTimeInHours: number;
  upTimeSince: string = '';
  tooltip: boolean = false;
  public configurationForm: FormGroup;

  constructor(
    private dataengineConfigurationService: DataengineConfigurationService,
    private _fb: FormBuilder
  ) {}

  public open(param, environment, resource): void {
    this.tooltip = false;
    this.resource = resource;
    this.environment = environment;

    this.upTimeInHours = (this.resource.up_time) ? DateUtils.diffBetweenDatesInHours(this.resource.up_time) : 0;
    this.upTimeSince = (this.resource.up_time) ? new Date(this.resource.up_time).toString() : '';
    this.initFormModel();

    if (this.resource.image === 'docker.dlab-dataengine') this.getClusterConfiguration();
    this.bindDialog.open(param);
  }

  public isEllipsisActive($event): void {
    if ($event.target.offsetWidth < $event.target.scrollWidth)
      this.tooltip = true;
  }

  public getClusterConfiguration(): void {
    this.dataengineConfigurationService
      .getClusterConfiguration(this.environment.name, this.resource.computational_name)
      .subscribe(result => {
        console.log(result);
      });
  }

  public editClusterConfiguration(data): void {
    this.dataengineConfigurationService
      .editClusterConfiguration(data.configuration_parameters, this.environment.name, this.resource.computational_name)
      .subscribe(result => {
        console.log(result);
      });
  }

  private initFormModel(): void {
    this.configurationForm = this._fb.group({
      configuration_parameters: ['', [this.validConfiguration.bind(this)]]
    });
  }

  private validConfiguration(control) {
    if (this.configuration)
      return this.configuration.nativeElement['checked']
        ? (control.value && control.value !== null && CheckUtils.isJSON(control.value) ? null : { valid: false })
        : null;
  }
}
