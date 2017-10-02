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

import { Component, OnInit } from '@angular/core';
import { EnvironmentStatusModel } from './environment-status.model';
import { HealthStatusService } from '../core/services';

@Component({
    moduleId: module.id,
    selector: 'health-status',
    templateUrl: 'health-status.component.html',
    styles: [`.sub-nav { justify-content: flex-end }`]
})
export class HealthStatusComponent implements OnInit {

    environmentsHealthStatuses: Array<EnvironmentStatusModel>;
    healthStatus: string;

    constructor(private healthStatusService: HealthStatusService) { }

    ngOnInit(): void {
      this.buildGrid();
    }

    buildGrid(): void {
      this.healthStatusService.getEnvironmentStatuses()
        .subscribe((result) => {
            this.environmentsHealthStatuses = this.loadHealthStatusList(result);
        });
    }

    loadHealthStatusList(healthStatusList): Array<EnvironmentStatusModel> {
        this.healthStatus = healthStatusList.status;

        if (healthStatusList.list_resources)
            return healthStatusList.list_resources.map((value) => {
                return new EnvironmentStatusModel(
                value.type,
                value.resource_id,
                value.status);
            });
    }
}
