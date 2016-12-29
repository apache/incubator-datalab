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
import { Observable } from 'rxjs';
import { Response } from '@angular/http';
import { UserResourceService } from '../../services/userResource.service';

export class ConfirmationDialogModel {
  private title: string;
  private notebook: any;
  private confirmAction: Function;
  private userResourceService: UserResourceService;

  static getDefault(): ConfirmationDialogModel {
    return new
      ConfirmationDialogModel(
      ConfirmationDialogType.StopExploratory, { name: '', resources: [] }, () => { }, () => { }, null);
  }

  constructor(
    confirmationType: ConfirmationDialogType,
    notebook: any,
    fnProcessResults: any,
    fnProcessErrors: any,
    userResourceService: UserResourceService
  ) {
    this.userResourceService = userResourceService;
    this.setup(confirmationType, notebook, fnProcessResults, fnProcessErrors);
  }

  public isAliveResources(resources): boolean {
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

  private setup(confirmationType: ConfirmationDialogType, notebook: any, fnProcessResults: any, fnProcessErrors: any): void {

    let containRunningResourcesStopMessage = 'Exploratory Environment will be stopped\
     and all connected computational resources will be terminated.';
    let defaultStopMessage = 'Exploratory Environment will be stopped';

    let containRunningResourcesTerminateMessage = 'Exploratory Environment and all connected computational resources\
     will be terminated.';
    let defaultTerminateMessage = 'Exploratory Environment will be terminated.';

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
