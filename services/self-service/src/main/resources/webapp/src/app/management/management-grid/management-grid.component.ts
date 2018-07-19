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

import { Component, OnInit, ViewChild, Input, Output, EventEmitter } from '@angular/core';

import { HealthStatusService, UserAccessKeyService } from '../../core/services';
import { ConfirmationDialogType } from '../../shared';

@Component({
  selector: 'management-grid',
  templateUrl: 'management-grid.component.html',
  styleUrls: ['./management-grid.component.scss',
              '../../resources/resources-grid/resources-grid.component.css']
})
export class ManagementGridComponent implements OnInit {
   @Output() refreshGrid: EventEmitter<{}> = new EventEmitter();

   @ViewChild('confirmationDialog') confirmationDialog;
   @ViewChild('keyReuploadDialog') keyReuploadDialog;
   

    constructor(
      private healthStatusService: HealthStatusService,
      private userAccessKeyService: UserAccessKeyService,
    ) { }

    ngOnInit(): void {
      this.buildGrid();
    }
    
    buildGrid(): void {
      this.refreshGrid.emit();
    }
}
