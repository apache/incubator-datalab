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

import {Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import { UserResourceService } from "./../../services/userResource.service";

@Component({
    moduleId: module.id,
    selector: 'computational-resources-list',
    templateUrl: 'computational-resources-list.component.html',
    styleUrls: ['./computational-resources-list.component.css']
})

export class ComputationalResourcesList {
  @ViewChild('terminateConfirmateResource') terminateConfirmateResource;
  @ViewChild('detailComputationalResource') detailComputationalResource;
  @Input() resources: any[];
  @Input() environment: any[];

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  collapse: boolean = false;

  constructor(private userResourceService: UserResourceService) { }

  toggleResourceList() {
    this.collapse = !this.collapse;
  }

  rebuildGrid(): void {
    this.buildGrid.emit();
  }

  terminateComputationalResources(notebook, resource): void {
    this.terminateConfirmateResource.open({ isFooter: false }, notebook, resource);
  };

  detailComputationalResources(environment, resource): void {
    this.detailComputationalResource.open({ isFooter: false }, environment, resource);
  };
}
