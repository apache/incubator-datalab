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
/* tslint:disable:no-empty */

import { Response } from '@angular/http';
import { UserResourceService } from '../../../core/services';

export class ComputationalResourcesModel {

  public computationalName: string;
  private notebook: any;
  private resource: any;
  private confirmAction: Function;
  private userResourceService: UserResourceService;

  static getDefault(userResourceService: UserResourceService): ComputationalResourcesModel {
    return new ComputationalResourcesModel({}, {}, () => { }, () => { }, userResourceService);
  }

  constructor(
    notebook: any,
    resource: any,
    fnProcessResults: any,
    fnProcessErrors: any,
    userResourceService: UserResourceService
  ) {
    this.userResourceService = userResourceService;
    this.terminateComputationalResource(notebook, resource, fnProcessResults, fnProcessErrors);
  }

  terminateComputationalResource(notebook: any, resource: any, fnProcessResults: any, fnProcessErrors: any) {
    this.notebook = notebook;
    this.resource = resource;

    if (this.resource)
      this.computationalName = this.resource.computational_name;

    this.confirmAction = (action) => {
      if (action === 'stop') {
        this.userResourceService
          .toggleStopStartAction(notebook.name, resource.computational_name, 'stop')
          .subscribe((response: Response) => fnProcessResults(response), (response: Response) => fnProcessErrors(response));
      } else if (action === 'terminate') {
        this.userResourceService
          .suspendComputationalResource(notebook.name, resource.computational_name)
          .subscribe((response: Response) => fnProcessResults(response), (response: Response) => fnProcessErrors(response));
      }
    }
  };
}
