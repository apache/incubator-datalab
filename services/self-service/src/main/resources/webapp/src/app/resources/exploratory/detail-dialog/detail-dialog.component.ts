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
import { FormGroup, FormBuilder } from '@angular/forms';

import { DateUtils, CheckUtils } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { DataengineConfigurationService } from '../../../core/services';
import { CLUSTER_CONFIGURATION } from '../../computational/computational-resource-create-dialog/cluster-configuration-templates';

@Component({
  selector: 'detail-dialog',
  templateUrl: 'detail-dialog.component.html',
  styleUrls: ['./detail-dialog.component.scss']
})

export class DetailDialogComponent {
  readonly DICTIONARY = DICTIONARY;

  notebook: any;
  upTimeInHours: number;
  upTimeSince: string = '';
  tooltip: boolean = false;

  public configurationForm: FormGroup;

  @ViewChild('bindDialog') bindDialog;
  @ViewChild('configurationNode') configuration;

  constructor(
    private dataengineConfigurationService: DataengineConfigurationService,
    private _fb: FormBuilder
  ) {}

  public open(param, notebook): void {
    this.tooltip = false;
    this.notebook = notebook;

    this.upTimeInHours = (notebook.time) ? DateUtils.diffBetweenDatesInHours(this.notebook.time) : 0;
    this.upTimeSince = (notebook.time) ? new Date(this.notebook.time).toString() : '';

    this.initFormModel();
    this.getClusterConfiguration();
    this.bindDialog.open(param);
  }

  public isEllipsisActive($event): void {
    if ($event.target.offsetWidth < $event.target.scrollWidth)
      this.tooltip = true;
  }

  public getClusterConfiguration(): void {
    this.dataengineConfigurationService
      .getExploratorySparkConfiguration(this.notebook.name)
      .subscribe(result => {
        console.log(result);
      });
  }

  public editClusterConfiguration(data): void {
    this.dataengineConfigurationService
      .editExploratorySparkConfiguration(data.configuration_parameters, this.notebook.name)
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
