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

import { Component } from '@angular/core';
import { UserResourceService } from './../services/userResource.service';
import { EnvironmentStatusModel } from './environment-status.model';

@Component({
    moduleId: module.id,
    selector: 'health-status',
    templateUrl: 'health-status.component.html',
    styleUrls: ['health-status.component.css',
                '../components/resources-grid/resources-grid.component.css']
})
export class HealthStatusComponent {
  environmentsHealthStatuses: EnvironmentStatusModel[];

  constructor(
    private userResourceService: UserResourceService
  ) { }

  ngOnInit(): void {
    this.buildGrid();
  }

  buildGrid(): void {
    this.userResourceService.getEnvironmentStatuses()
      .subscribe((result) => {
        this.environmentsHealthStatuses = this.loadHealthStatusList(result);
      });
  }

  loadHealthStatusList(healthStatusList): Array<EnvironmentStatusModel> {
    return healthStatusList.map((value) => {
      return new EnvironmentStatusModel(
        value.instance_type,
        value.cloud_type,
        value.instance_ip,
        value.instance_path,
        value.status);
    });
  }
}
