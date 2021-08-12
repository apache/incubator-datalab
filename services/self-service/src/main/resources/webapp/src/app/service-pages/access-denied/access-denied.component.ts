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

@Component({
  selector: 'datalab-access-denied',
  template: `
    <div class="no-access-page">
      <div class="content">
        <a class="logo" href="#/resources_list">
          <img src="assets/img/security-screen.png" alt="">
        </a>

        <div class="message-block">
          <h3>Access Denied!</h3>
          <p>The page you were trying to reach has restricted access.
            <a href="#/resources_list">Go to the Homepage?</a>
          </p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./access-denied.component.scss']
})
export class AccessDeniedComponent implements OnInit {
  constructor() { }
  ngOnInit() { }
}
