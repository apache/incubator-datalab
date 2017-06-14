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

@Component({
  selector: 'dlab-reporting',
  template: `
  <dlab-navbar></dlab-navbar>
  <dlab-toolbar></dlab-toolbar>
  <dlab-reporting-grid></dlab-reporting-grid>
  <footer>
    Total price *** $
  </footer>
  `,
  styles: [`
    footer {
      position: fixed;
      left: 0px;
      bottom: 0px;
      height: 30px;
      width: 100%;
      background: #f9fafb;
      color: #36b1d8;
      text-align: right;
      padding: 30px 40px;
      font-size: 20px;
    }
  `]
})
export class ReportingComponent implements OnInit {

  constructor() { }

  ngOnInit() {}

}
