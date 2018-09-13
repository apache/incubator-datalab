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

import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'detail-computational-resources',
  templateUrl: 'detail-computational-resources.component.html'
})

export class DetailComputationalResourcesComponent {
  readonly DICTIONARY = DICTIONARY;

  resource: any;
  environment: any;
  @ViewChild('bindDialog') bindDialog;

  upTimeInHours: number;
  upTimeSince: string = '';
  tooltip: boolean = false;

  public open(param, environment, resource): void {
    this.tooltip = false;
    this.resource = resource;
    this.environment = environment;

    this.upTimeInHours = (this.resource.up_time) ? DateUtils.diffBetweenDatesInHours(this.resource.up_time) : 0;
    this.upTimeSince = (this.resource.up_time) ? new Date(this.resource.up_time).toString() : '';

    this.bindDialog.open(param);
  }

  public isEllipsisActive($event): void {
    if ($event.target.offsetWidth < $event.target.scrollWidth)
      this.tooltip = true;
  }
}
