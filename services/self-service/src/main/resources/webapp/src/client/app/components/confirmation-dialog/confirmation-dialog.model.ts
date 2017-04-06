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

import { ConfirmationDialogType } from './confirmation-dialog-type.enum';
import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';
import { UserResourceService } from '../../services/userResource.service';
import { HealthStatusService } from '../../services/healthStatus.service';

export class ConfirmationDialogModel {
  private title: string;
  private notebook: any;
  private confirmAction: Function;
  private userResourceService: UserResourceService;
  private healthStatusService: HealthStatusService;

  static getDefault(): ConfirmationDialogModel {
    return new
      ConfirmationDialogModel(
      ConfirmationDialogType.StopExploratory, { name: '', resources: [] }, () => { }, () => { }, null, null);
  }

  constructor(
    confirmationType: ConfirmationDialogType,
    notebook: any,
    fnProcessResults: any,
    fnProcessErrors: any,
    userResourceService: UserResourceService,
    healthStatusService: HealthStatusService
  ) {
    this.userResourceService = userResourceService;
    this.healthStatusService = healthStatusService;
    this.setup(confirmationType, notebook, fnProcessResults, fnProcessErrors);
  }

  public isAliveResources(resources): boolean {
    if(resources)
      for (var i = 0, len = resources.length; i < len; i++)
        if (resources[i].status.toLowerCase() === 'running')
          return true;

    return false;
  }


  private stopExploratory(): Observable<Response> {
    return this.userResourceService.suspendExploratoryEnvironment(this.notebook, 'stop');
  }

  private terminateExploratory(): Observable<Response> {
    return this.userResourceService.suspendExploratoryEnvironment(this.notebook, 'terminate');
  }

  private stopEdgeNode(): Observable<Response> {
    return this.healthStatusService.suspendEdgeNode();
  }

  private setup(confirmationType: ConfirmationDialogType, notebook: any, fnProcessResults: any, fnProcessErrors: any): void {

    let containRunningResourcesStopMessage = 'Exploratory Environment will be stopped\
     and all connected computational resources will be terminated.';
    let defaultStopMessage = 'Exploratory Environment will be stopped';

    let containRunningResourcesTerminateMessage = 'Exploratory Environment and all connected computational resources\
     will be terminated.';
    let defaultTerminateMessage = 'Exploratory Environment will be terminated.';

    let edgeNodeStopMessage = 'Edge node will be stopped. You will need to start it later to proceed working with DLAB.';

    switch (confirmationType) {
      case ConfirmationDialogType.StopExploratory: {
        this.title = this.isAliveResources(notebook.resources) ? containRunningResourcesStopMessage : defaultStopMessage;
        this.notebook = notebook;
        this.confirmAction = () => this.stopExploratory()
          .subscribe((response: Response) => fnProcessResults(response),
          (response: Response) => fnProcessErrors(response));
      }
        break;
      case ConfirmationDialogType.TerminateExploratory: {
        this.title = this.isAliveResources(notebook.resources) ? containRunningResourcesTerminateMessage : defaultTerminateMessage;
        this.notebook = notebook;
        this.confirmAction = () => this.terminateExploratory()
          .subscribe((response: Response) => fnProcessResults(response),
          (response: Response) => fnProcessErrors(response));
      }
        break;
      case ConfirmationDialogType.StopEdgeNode: {
        this.title = edgeNodeStopMessage;
        this.notebook = notebook;
        this.confirmAction = () => this.stopEdgeNode()
          .subscribe((response: Response) => fnProcessResults(response),
          (response: Response) => fnProcessErrors(response));
      }
        break;
      default: {
        this.title = this.isAliveResources(notebook.resources) ? containRunningResourcesTerminateMessage : defaultTerminateMessage;
        this.notebook = notebook;
        this.confirmAction = () => this.stopExploratory()
          .subscribe((response: Response) => fnProcessResults(response),
          (response: Response) => fnProcessErrors(response));
      }
        break;
    }
  }
}
