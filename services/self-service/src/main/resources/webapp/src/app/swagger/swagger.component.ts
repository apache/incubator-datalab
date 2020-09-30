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

import { Component, OnInit } from '@angular/core';
import {HealthStatusService} from '../core/services';
import {ToastrService} from 'ngx-toastr';

declare const SwaggerUIBundle: any;

@Component({
  selector: 'datalab-swagger',
  templateUrl: './swagger.component.html',
  styleUrls: ['./swagger.component.scss']
})
export class SwaggerComponent implements OnInit {
  private healthStatus: any;

  constructor(
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
    ) {
  }

  ngOnInit(): void {
    this.getEnvironmentHealthStatus();
    const ui = SwaggerUIBundle({
      dom_id: '#swagger-ui',
      layout: 'BaseLayout',
      presets: [
        SwaggerUIBundle.presets.apis,
        SwaggerUIBundle.SwaggerUIStandalonePreset
      ],
      url: '../assets/endpoint-api.json',
      docExpansion: 'none',
      operationsSorter: 'alpha'
    });
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus().subscribe(
      (result: any) => {
        this.healthStatus = result;
      },
      error => this.toastr.error(error.message, 'Oops!'));
  }

}
