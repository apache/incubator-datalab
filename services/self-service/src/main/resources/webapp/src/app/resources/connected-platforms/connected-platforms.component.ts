/*!
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

import { Component, OnInit } from '@angular/core';
import { GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { HealthStatusService } from '../../core/services';
import { ToastrService } from 'ngx-toastr';
import { ConnectedPlatformDisplayedColumns, Image_Table_Titles } from './connected-platforms.comnfig';

const mockedData = [
  {
    platformName: 'azure',
    linkToPlatform: 'www.google.com/'
  },
  {
    platformName: 'azure',
    linkToPlatform: 'google.com'
  },
  {
    platformName: 'azure',
    linkToPlatform: 'google.com'
  },
  {
    platformName: 'azure',
    linkToPlatform: 'google.com'
  },
];


@Component({
  selector: 'datalab-connected-platforms',
  templateUrl: './connected-platforms.component.html',
  styleUrls: ['./connected-platforms.component.scss']
})
export class ConnectedPlatformsComponent implements OnInit {
  readonly tableHeaderCellTitles: typeof Image_Table_Titles = Image_Table_Titles;

  healthStatus: GeneralEnvironmentStatus;

  displayedColumns: typeof ConnectedPlatformDisplayedColumns = ConnectedPlatformDisplayedColumns;
  dataSource = mockedData;

  constructor(
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
  ) { }

  ngOnInit(): void {
    this.getEnvironmentHealthStatus();
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe(
      (result: GeneralEnvironmentStatus) => {
        this.healthStatus = result;
      },
      error => this.toastr.error(error.message, 'Oops!')
    );
  }
}
