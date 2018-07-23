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
import { HealthStatusService, ManageEnvironmentsService }  from './../core/services';
import { EnvironmentModel }  from './management.model';

@Component({
  selector: 'environments-management',
  templateUrl: './management.component.html',
  styleUrls: ['./management.component.scss']
})
export class ManagementComponent implements OnInit {
  public healthStatus: string = '';
  public billingEnabled: boolean;
  public admin: boolean;

  public allEnvironmentData: Array<EnvironmentModel>;

  constructor(
    private healthStatusService: HealthStatusService,
    private manageEnvironmentsService: ManageEnvironmentsService
  ) { }

  ngOnInit() {
    this.buildGrid();
  }
  
  public buildGrid() {
    this.getEnvironmentHealthStatus();
    this.getAllEnvironmentData();
  }

  private getAllEnvironmentData() {
    this.manageEnvironmentsService.getAllEnvironmentData().subscribe(
      (result: any) => {
        this.allEnvironmentData = this.loadEnvironmentList(result);

        console.log(this.allEnvironmentData);
      }
    )
  }

  private loadEnvironmentList(data): Array<EnvironmentModel> {
    // this.checkUserAccessKey();
    if (data)
      return data.map(value => {
        return new EnvironmentModel(
          value.resource_name || value.resource_type,
          value.status,
          value.shape,
          value.computational_resources,
          value.user)
      });
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => {
        this.healthStatus = result.status;
        this.billingEnabled = result.billingEnabled;
        this.admin = result.admin;

        // this.checkUserAccessKey();
      });
  }

  manageEnvironmentAction($event) {
    console.log($event);
    
    this.manageEnvironmentsService
      .environmentManagement($event.environment.user, $event.action, $event.environment.name, $event.resource.computational_name)
      .subscribe(res => {
        debugger;
      });
  }
}
