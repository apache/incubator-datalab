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

import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { UserResourceService } from '../../../core/services';

@Component({
  moduleId: module.id,
  selector: 'computational-resources-list',
  templateUrl: 'computational-resources-list.component.html',
  styleUrls: ['./computational-resources-list.component.css']
})

export class ComputationalResourcesListComponent {
  @ViewChild('confirmationDialog') confirmationDialog;
  @ViewChild('detailComputationalResource') detailComputationalResource;
  @Input() resources: any[];
  @Input() environment: any[];

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  collapse: boolean = true;
  constructor(private userResourceService: UserResourceService) { }

  toggleResourceList() {
    this.collapse = !this.collapse;
  }

  toggleResourceAction(resource, action) {
    if (action === 'stop') {
      this.confirmationDialog.open({ isFooter: false }, this.environment, resource, 'stop');
    } else if ('start') {
      this.userResourceService
        .toggleStopStartAction(this.environment['name'], resource.computational_name, 'start')
        .subscribe(res => {
          this.rebuildGrid();
        });
    }
  }

  rebuildGrid(): void {
    this.buildGrid.emit();
  }

  terminateComputationalResources(notebook, resource): void {
    this.confirmationDialog.open({ isFooter: false }, notebook, resource, 'terminate');
  };

  detailComputationalResources(environment, resource): void {
    this.detailComputationalResource.open({ isFooter: false }, environment, resource);
  };
}
